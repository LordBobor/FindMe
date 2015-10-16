package dev.ekozoch.findme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.location.Location;
import android.location.LocationListener;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.fivehundredpx.android.blur.BlurringView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import dev.ekozoch.findme.parse.classes.User;

public class MainActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private Marker mPositionMarker;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient googleApiClient;
    View mapLayout;
    BlurringView mBlurringView;
    FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();

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
        }
        else {
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
    public void onConnected(Bundle bundle) {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        displayLocationOnMap(lastLocation);
        createLocationRequest();
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
        LocationRequest request = new LocationRequest()
                .setInterval(1000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setSmallestDisplacement(500);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                displayLocationOnMap(location);
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(MainActivity.this, "Connection Suspended", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(MainActivity.this, connectionResult.toString(), Toast.LENGTH_SHORT).show();
    }

    private void displayLocationOnMap(final Location location) {
        if (location == null) return;
        if (FindMeApplication.currentUser==null) return;

        if (mPositionMarker == null) {

            final View markerView = ((LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_pin, null);
            ImageView ivIcon = (ImageView) markerView.findViewById(R.id.ivPhoto);
            Picasso.with(this).load(FindMeApplication.currentUser.getUserPic()).into(ivIcon, new Callback() {
                @Override
                public void onSuccess() {
                    mPositionMarker = mMap.addMarker(new MarkerOptions()
                            .flat(true)
                            .icon(BitmapDescriptorFactory
                                    .fromBitmap(createDrawableFromView(
                                            MainActivity.this,
                                            markerView)))
                            .anchor(0.5f, 0.5f)

                            .position(
                                    new LatLng(location.getLatitude(), location
                                            .getLongitude())));

                    animateMarker(mPositionMarker, location); // Helper method for smooth
                    // animation

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location
                            .getLatitude(), location.getLongitude()), 18.0f));
                }

                @Override
                public void onError() {

                }
            });


        } else {
            animateMarker(mPositionMarker, location); // Helper method for smooth
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
                    e.printStackTrace();
                }
            }
        });
    }

    private void addMarker(final User user) {
        if(FindMeApplication.currentUser!=null && user.getUsername().equals(FindMeApplication.currentUser.getUsername())) return;
        if(user.getLatitude()==0 || user.getLongitude()==0) return;

        final View markerView = ((LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_pin, null);
        ImageView ivIcon = (ImageView) markerView.findViewById(R.id.ivPhoto);
        ((ImageView) markerView.findViewById(R.id.ivPin)).setColorFilter(Color.parseColor("#00796B"));
        Picasso.with(this).load(user.getUserPic()).into(ivIcon, new Callback() {
            @Override
            public void onSuccess() {
                mPositionMarker = mMap.addMarker(new MarkerOptions()
                        .flat(true)
                        .icon(BitmapDescriptorFactory
                                .fromBitmap(createDrawableFromView(
                                        MainActivity.this,
                                        markerView)))
                        .anchor(0.5f, 0.5f)
                        .position(
                                new LatLng(user.getLatitude(), user.getLongitude())));
            }


            @Override
            public void onError() {

            }
        });
    }


    public static Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay()
                .getMetrics(displayMetrics);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels,
                displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(),
                view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }


    public void animateMarker(final Marker marker, final Location location) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final LatLng startLatLng = marker.getPosition();
        final double startRotation = marker.getRotation();
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);

                double lng = t * location.getLongitude() + (1 - t)
                        * startLatLng.longitude;
                double lat = t * location.getLatitude() + (1 - t)
                        * startLatLng.latitude;

                float rotation = (float) (t * location.getBearing() + (1 - t)
                        * startRotation);

                marker.setPosition(new LatLng(lat, lng));
                marker.setRotation(rotation);

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

}
