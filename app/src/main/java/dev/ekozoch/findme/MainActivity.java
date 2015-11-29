package dev.ekozoch.findme;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.fivehundredpx.android.blur.BlurringView;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

import dev.ekozoch.findme.parse.classes.User;

public class MainActivity extends BaseActivity implements GoogleMap.OnMarkerClickListener {

    View mapLayout;
    BlurringView mBlurringView;
    FrameLayout container;
    private Marker mPositionMarker;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private FMGoogleApiClient mapsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        FindMeApplication.currentUser = User.getCurrentUser();
        if (getUser() == null) {
            //Если кэш пустой
            User.logInAnonymous(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e != null) {
                        //Не удалось войти
                        Toast.makeText(MainActivity.this, "LogIn operation failed", Toast.LENGTH_SHORT).show();
                    } else {
                        //Логин успешен
                        FindMeApplication.currentUser = (User) user;
                        //putDataOnView();
                    }
                }
            });
        } else {
            ParseUser.logInInBackground(FindMeApplication.currentUser.getUsername(), FindMeApplication.currentUser.getUsername(), new LogInCallback() {
                @Override
                public void done(ParseUser parseUser, ParseException e) {
                    setUpMapIfNeeded();
                }
            });
        }
        mapLayout = findViewById(R.id.mapLayout);
        container = (FrameLayout) findViewById(R.id.container);
        mBlurringView = (BlurringView) findViewById(R.id.blurring_view);
        // Give the blurring view a reference to the blurred view.
        mBlurringView.setBlurredView(mapLayout);
        mBlurringView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBlurringView.setVisibility(View.GONE);
                container.setVisibility(View.GONE);
            }
        });

        mapsClient = new FMGoogleApiClient(this);
        mapsClient.setOnMapsConnectedListener(new FMGoogleApiClient.OnMapsConnectedListener() {
            @Override
            public void onConnected(Bundle bundle) {
                displayLocationOnMap(mapsClient.getLastLocation());
                createLocationRequest();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMarkerClickListener(this);
//        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
//        mMap.getUiSettings().setMapToolbarEnabled(true);

        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Если нажали на иконку - показываем окно логина через соцсети.
        if (id == R.id.action_login_social) {
            mBlurringView.setVisibility(View.VISIBLE);
            container.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new LoginFragment())
                    .commit();
            mBlurringView.invalidate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Это зачем-то нужно для инициализации соцсетей
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SOCIAL_NETWORK_TAG);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void createLocationRequest() {
        LocationListener listener = new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                displayLocationOnMap(location);
            }
        };

        mapsClient.createTimeIntervalRequest(listener);
        mapsClient.createDistanceIntervalRequest(listener);
    }

    private void displayLocationOnMap(final Location location) {
        if (location == null) return;
        if (FindMeApplication.currentUser == null) return;

        if (mPositionMarker == null) {

            MarkerHelper.putMarkerOnMap(MainActivity.this, getUser(), mMap, new MarkerHelper.OnMarkerAddedListener() {
                @Override
                public void onMarkerAdded(Marker marker) {
                    mPositionMarker = marker;

                    MarkerHelper.animateMarker(mPositionMarker, location);
                    // Helper method for smooth
                    // animation

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location
                            .getLatitude(), location.getLongitude()), 18.0f));
                }
            });

        } else {
            MarkerHelper.animateMarker(mPositionMarker, location); // Helper method for smooth
            // animation

            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location
                    .getLatitude(), location.getLongitude())));
        }


        FindMeApplication.currentUser.setLatitude(location.getLatitude());
        FindMeApplication.currentUser.setLongitude(location.getLongitude());
        FindMeApplication.currentUser.saveInBackground(new SaveCallback() {
            @Override
            public void done(final ParseException e) {
                if (e != null) {
                    Toast.makeText(MainActivity.this, "Save data Fail", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } else {
                    Toast.makeText(MainActivity.this, "Save data success", Toast.LENGTH_SHORT).show();
                }
            }
        });

        loadUsers();
    }

    private void loadUsers() {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> objects, ParseException e) {
                if (e == null) {
                    for (ParseUser user : objects)
                        addMarker((User) user);

                } else {
                    // Something went wrong.
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

    private void addMarker(final User user) {
        if (FindMeApplication.currentUser != null && user.getUsername().equals(FindMeApplication.currentUser.getUsername()))
            return;
        if (user.getLatitude() == 0 || user.getLongitude() == 0) return;

        MarkerHelper.putMarkerOnMap(MainActivity.this, user, mMap);
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Toast.makeText(this, marker.getSnippet(), Toast.LENGTH_SHORT).show();
        UserDetailActivity.show(MainActivity.this, marker.getSnippet());
        return true;
    }

}
