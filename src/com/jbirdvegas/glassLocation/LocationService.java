/*
 * HelloGlass.java
 * @author Cody Engel
 * http://codyengel.info
 */
package com.jbirdvegas.glassLocation;

// Glass Specific

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.google.android.glass.timeline.TimelineManager;

public class LocationService extends BaseService {
    public static final String LOCATION_MESSAGE = "location_message";
    public static final String STATUS_MESSAGE = "status_message";
    public static final String PROVIDER_MESSAGE = "provider_message";
    private static final String TAG = SensorService.class.getSimpleName();
    public static final String SEARCHING_METHOD = "searching_message";
    public static final String KILL_UPDATES = "kill_updates";

    /*
     * TimelineManager allows applications to interact with the timeline.
     * 
     * Additional information: https://developers.google.com/glass/develop/gdk/reference/com/google/android/glass/timeline/TimelineManager
     */
	private TimelineManager mTimelineManager;
	

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    @Override
	public void onCreate() {
		super.onCreate();
	} // onCreate

	@Override
	public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
		return null;
	} // IBinder

    private static boolean isStarted = false;
	/*
	 * onStartCommand is used to start a service from your voice trigger you set up in res/xml/voice_trigger_location.xmlxml
	 */
	public int onStartCommand(Intent intent, int flags, int startId) {
        setupService();
        if (!isStarted) {
            isStarted = true;
            location();
        }
        if (intent != null && intent.getExtras() != null) {
            if (intent.getExtras().getBoolean(KILL_UPDATES, false)) {
                killAllLocationThings(intent);
            }
        }

		Intent i = new Intent(this, getLiveCardClass());

        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Log.d(TAG, "Attempting to launch activity");
//        Intent updateIntent = getUpdateIntent();
//        updateIntent.putExtra(SEARCHING_METHOD, "nothing to report");
//        sendMessage(updateIntent);
        startActivity(i);

		return Service.START_NOT_STICKY;
	} // onStartCommand

    @Override
    protected Class<?> getLiveCardClass() {
        return LocationActivity.class;
    }

    private void killAllLocationThings(Intent intent) {
        if (mLocationManager != null&& mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent service = PendingIntent.getService(this, 42, intent, 0);
            alarmManager.cancel(service);
        }
        stopSelf();
    }

    @Override
    public boolean stopService(Intent name) {
        Log.d(TAG, "stopService called");
        return super.stopService(name);
    }

    private void location() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(true);

        String provider = mLocationManager.getBestProvider(criteria, true);
        boolean isEnabled = mLocationManager.isProviderEnabled(provider);
        if (isEnabled) {
            // Define a listener that responds to location updates
            mLocationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                    makeUseOfNewLocation(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            // Register the listener with the Location Manager to receive location updates
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, mLocationListener);
        }
    }

    private void makeUseOfNewLocation(Location location) {
        Intent intent = getUpdateIntent()
                .putExtra(LOCATION_MESSAGE, location);
        Log.d(TAG, "location returned: " + location.toString());
//        startActivity(intent);
        String lat = "Latitude: " + location.getLatitude();
        String lng = ", Longitude: " + location.getLongitude();
        String summary = lat + lng;
        sendMessage(intent);
    }

    @Override
    protected Intent getUpdateIntent() {
        return new Intent(LocationActivity.LOCATION_UPDATE);
    }
}