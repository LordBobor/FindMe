package dev.ekozoch.findme;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import dev.ekozoch.findme.parse.classes.User;

/**
 * Created by ekozoch on 15.11.15.
 */
public class MarkerHelper {

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


    public static void animateMarker(final Marker marker, final Location location) {
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

    public static void putMarkerOnMap(final Context context, final User user, final GoogleMap mMap) {
        putMarkerOnMap(context, user, mMap, null);
    }

    public static void putMarkerOnMap(final Context context, final User user, final GoogleMap mMap, final OnMarkerAddedListener listener) {
        final View markerView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_pin, null);
        ImageView ivIcon = (ImageView) markerView.findViewById(R.id.ivPhoto);
        ((ImageView) markerView.findViewById(R.id.ivPin)).setColorFilter(Color.parseColor("#00796B"));
        Picasso.with(context).load(user.getUserPic()).into(ivIcon, new Callback() {
            @Override
            public void onSuccess() {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .flat(true)
                        .icon(BitmapDescriptorFactory.fromBitmap(MarkerHelper.createDrawableFromView(context, markerView)))
                        .anchor(0.5f, 0.5f)
                        .snippet(user.getObjectId() + " " + user.getUserPic())
                        .position(new LatLng(user.getLatitude(), user.getLongitude())));

                if (listener != null) listener.onMarkerAdded(marker);
            }

            @Override
            public void onError() {
                Log.e("LOG", "Counld not load picture");
            }
        });
    }


    public interface OnMarkerAddedListener {
        void onMarkerAdded(Marker marker);
    }

}
