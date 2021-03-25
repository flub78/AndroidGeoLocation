package com.letssurf.geolocation;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements FetchAddressTask.OnTaskCompleted {

    static final String TAG = "MainActivity";
    public static final int REQUEST_LOCATION_PERMISSION = 99;
    private static final String TRACKING_LOCATION_KEY = "tracking_location";

    // Views
    private ImageView mAndroidImageView;
    private AnimatorSet mRotateAnim;
    private TextView mLocationTextView;
    private Button mLocationButton;

    // Location classes
    private Location mLastLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mTrackingLocation = false;

    // Animation
    private LocationCallback mLocationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationButton = (Button) findViewById(R.id.button_location);
        mLocationTextView = (TextView) findViewById(R.id.textview_location);
        mAndroidImageView = (ImageView) findViewById(R.id.imageview_android);

        // Setup the animation
        mRotateAnim = (AnimatorSet) AnimatorInflater.loadAnimator
                (this, R.animator.rotate);
        mRotateAnim.setTarget(mAndroidImageView);

        if (savedInstanceState != null) {
            mTrackingLocation = savedInstanceState.getBoolean(
                    TRACKING_LOCATION_KEY);
        }

        // Initialize the FusedLocationClient.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // If tracking is turned on, reverse geocode into an address
                if (mTrackingLocation) {
                    new FetchAddressTask(MainActivity.this, MainActivity.this).execute(locationResult.getLastLocation());
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTrackingLocation) stopLocationTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTrackingLocation) startLocationTracking();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(TRACKING_LOCATION_KEY, mTrackingLocation);
    }

    /**
     * Activated on click on Get location button
     * @param view
     */
    public void onClick(View view) {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);

            // when there is no permission the tracking start is postponed
            // until the permission request result
        } else {
            Log.d(TAG, "getLocation: permissions granted");

             if (mTrackingLocation) {
                stopLocationTracking();
            } else {
                startLocationTracking();
            }
        }
    }

    /**
     * Start tracking
     */
    private void startLocationTracking() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "getLocation: permissions not granted");
            return;
        }
        Log.d(TAG, "The getLocation: permissions is granted");

        mFusedLocationClient.requestLocationUpdates
                (getLocationRequest(), mLocationCallback,
                        null /* Looper */);

        // mLocationTextView.setText(getString(R.string.address_text, getString(R.string.loading), System.currentTimeMillis()));

        mRotateAnim.start();
        mTrackingLocation = true;
        mLocationButton.setText(R.string.stop_tracking);
    }

    /**
     * Stop tracking
     */
    private void stopLocationTracking () {
        if (mTrackingLocation) {
            mRotateAnim.end();
            mTrackingLocation = false;
            mLocationTextView.setText(R.string.textview_hint);
            mLocationButton.setText(R.string.start_tracking);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    /**
     * Callback that is invoked when the user responds to the permissions
     * dialog.
     *
     * @param requestCode  Request code representing the permission request
     *                     issued by the app.
     * @param permissions  An array that contains the permissions that were
     *                     requested.
     * @param grantResults An array with the results of the request for each
     *                     permission requested.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                // If the permission is granted, get the location,
                // otherwise, show a Toast
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationTracking();
                } else {
                    Toast.makeText(this,
                            R.string.location_permission_denied,
                            Toast.LENGTH_SHORT).show();
                }
        }
    }

    /**
     * Update location text view with the result of the geo coding task
     * translation from coordinate to text address
     * @param result
     */
    @Override
    public void onTaskCompleted(String result) {
        // Update the UI
        if (mTrackingLocation)
            mLocationTextView.setText(getString(R.string.address_text,
                result, System.currentTimeMillis()));
    }

    /**
     * Generate a location request with correct parameters
     * not too slow, not too fast, GPS coordinate if possible
     * @return
     */
    private LocationRequest getLocationRequest () {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }
}
