package dev.ekozoch.findme;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by ekozoch on 14.11.15.
 */
public class FMGoogleApiClient implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private Context context;
    private GoogleApiClient googleApiClient;
    private OnMapsConnectedListener listener;

    public FMGoogleApiClient(Context context) {
        this.context = context;
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();
    }

    public void setOnMapsConnectedListener(OnMapsConnectedListener listener) {
        this.listener = listener;
    }

    public void createTimeIntervalRequest(LocationListener timeIntervalListener) {
        LocationRequest timeIntervalRequest = new LocationRequest()
                .setInterval(1000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, timeIntervalRequest, timeIntervalListener);
    }

    public void createDistanceIntervalRequest(LocationListener locationListener) {
        LocationRequest distanceIntervalRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setSmallestDisplacement(500);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, distanceIntervalRequest, locationListener);
    }

    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (listener != null) listener.onConnected(bundle);
    }

    public Location getLastLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(context, "Connection Suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(context, connectionResult.toString(), Toast.LENGTH_SHORT).show();
    }


    public interface OnMapsConnectedListener {
        void onConnected(Bundle bundle);
    }
}
