package com.lagruva.backgroundservice;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.android.BuildConfig;
import com.getcapacitor.plugin.util.AssetUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.getcapacitor.Logger;

import org.json.JSONObject;


public class GeolocationService extends Service {
    public int ONGOING_NOTIFICATION_ID = 9876543;
    private final IBinder binder = new LocalBinder();
    protected static boolean started;
    protected Notification.Builder notification;
    private FusedLocationProviderClient client;
    private LocationCallback callback;
    private String partnerId;
    static final String ACTION_BROADCAST = (
            GeolocationService.class.getPackage().getName() + ".broadcast"
    );
    public static final String ACTION_LOCATION = (
            GeolocationService.class.getPackage().getName() + ".location"
    );

    public GeolocationService() {
        Logger.debug("BackgroundService.Geo", "Freaking constructor " + started);
    }

    @Override
    public void onCreate() {

        super.onCreate();
        Logger.debug("BackgroundService.Geo", "onCreate");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Logger.debug("BackgroundService.Geo", "onBind" + intent);
        return binder;
    }

    public void updateSubText(String subtext) {
        if (notification == null) {
            return;
        }
        Notification updated = notification.setSubText(subtext).build();
        NotificationManager manager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE
        );
        manager.notify(ONGOING_NOTIFICATION_ID, updated);
    }

    public void start() {
        if (notification == null) {
            setupNotification();
        }
        Logger.debug("BackgroundService.Geo", "to foreground. callback=" + callback);
        updateSubText("En servicio");

        startForeground(ONGOING_NOTIFICATION_ID, notification.build());
        if (callback == null) {
            locationWatch();
        }
        GeolocationService.started = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.debug("BackgroundService.Geo", "onStartCommand");
        start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.debug("BackgroundService.Geo", "destroy");
        if (callback != null) {
            getClient().removeLocationUpdates(callback);
            Logger.debug("BackgroundService.Geo", "callback removed");
            callback = null;
        }
        if (notification != null) {
            // updateSubText("Stopped");
            Notification updated = notification.setSubText("Detenido").setOngoing(false).build();
            NotificationManager manager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE
            );
            manager.notify(ONGOING_NOTIFICATION_ID, updated);
        }

        stopForeground(true);
        super.onDestroy();

    }

    public void stop() {
        Logger.debug("BackgroundService.Geo", "to background");
        if (callback != null) {
            getClient().removeLocationUpdates(callback);
            callback = null;
        }
        updateSubText("Background");
        stopForeground(true);
        started = false;
    }

    private void setupNotification() {

        int resId = AssetUtil.getResourceID(this, "notificacion_lagruva", "drawable");
        Logger.debug("BackgroundService.Geo", "notification icon : " + resId);
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("Registrando tu ubicación")
                .setContentText("Presione para ir a la aplicación")
                .setSmallIcon(resId)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis());

        Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(
                this.getPackageName()
        );
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            Logger.debug("BackgroundService.Geo", "Launch intent service: " + launchIntent);
            builder.setContentIntent(
                    PendingIntent.getActivity(
                            this,
                            0,
                            launchIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    )
            );
        }
        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("Lagruva");
        }
        notification = builder;
        Logger.debug("BackgroundService.Geo", "notification set");

    }

    private FusedLocationProviderClient getClient() {
        if (client == null) {
            client = LocationServices.getFusedLocationProviderClient(this);
            Logger.debug("BackgroundService.Geo", "created new client");
        }
        return client;
    }

    private void locationWatch() {
        int distanceFilter = 10;

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setMaxWaitTime(2000);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(distanceFilter);

        LocationCallback callback = new LocationCallback() {
            //        callback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Logger.debug("BackgroundService.Geo", "onLocationResult: " + locationResult);
                Location location = locationResult.getLastLocation();
                Logger.debug("BackgroundService.Geo", "onLocation: " + location);
                Intent intent = new Intent(ACTION_LOCATION);
                intent.putExtra("location", location);
                intent.putExtra("id", "Lagruva");
                LocalBroadcastManager.getInstance(
                        GeolocationService.this
                ).sendBroadcast(intent);
                sendPosition(location);
            }

            @Override
            public void onLocationAvailability(LocationAvailability availability) {
                if (!availability.isLocationAvailable() && BuildConfig.DEBUG) {
                    Logger.debug("BackgroundService.Geo", "Location not available");
                }
            }
        };

        Logger.debug("BackgroundService.Geo", "created new request");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Logger.debug("BackgroundService.Geo", "Permissions not granted!");

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        getClient().requestLocationUpdates(
                locationRequest,
                callback,
                null
        );
        Logger.debug("BackgroundService.Geo", "location requested");
        this.callback = callback;
    }

    private JSObject formatLocation(Location location) {
        JSObject obj = new JSObject();
        obj.put("latitude", location.getLatitude());
        obj.put("longitude", location.getLongitude());
        // The docs state that all Location objects have an accuracy, but then why is there a
        // hasAccuracy method? Better safe than sorry.
        obj.put("accuracy", location.hasAccuracy() ? location.getAccuracy() : JSONObject.NULL);
        obj.put("altitude", location.hasAltitude() ? location.getAltitude() : JSONObject.NULL);
        if (Build.VERSION.SDK_INT >= 26 && location.hasVerticalAccuracy()) {
            obj.put("altitudeAccuracy", location.getVerticalAccuracyMeters());
        } else {
            obj.put("altitudeAccuracy", JSONObject.NULL);
        }
        // In addition to mocking locations in development, Android allows the
        // installation of apps which have the power to simulate location
        // readings in other apps.
        obj.put("simulated", location.isFromMockProvider());
        obj.put("speed", location.hasSpeed() ? location.getSpeed() : JSONObject.NULL);
        obj.put("bearing", location.hasBearing() ? location.getBearing() : JSONObject.NULL);
        obj.put("time", location.getTime());
        return obj;
    }

    /**
     * Sends location updates from GeolocationService to Lagruva server
     * via the Firebase MessagingService
     *
     * @param location
     */
    private void sendPosition(Location location) {

        if (partnerId == null) {
            Logger.debug("BackgroundService.Geo", "getting partner_id from default preferences");
            partnerId = PreferenceManager.getDefaultSharedPreferences(this).getString("partner_id",null);
            if (partnerId == null) {
                Logger.warn("BackgroundService.Geo", "partnerId not set, ignoring position.");
                return;
            }
        }
        JSONObject position = new JSONObject();
        try {
            position.put("latitude", location.getLatitude());
            position.put("longitude", location.getLongitude());
            position.put("accuracy", location.getAccuracy());
            position.put("partner_id", partnerId);
            String args = "[" + position.toString() + "]";
            Logger.info("BackgroundService.Geo", "sending position: " + args);
            MessagingService.callMethod(this, "towing.pos", "create-nr", args, null);
        } catch (Exception e) {
            Logger.error("BackgroundService.Geo", "Error sending position", e);
        }
    }

    public class LocalBinder extends Binder {
        public void start() {
            Logger.debug("BackgroundService.Geo", "[binder] starting");
            GeolocationService.this.start();
        }

        public void stop() {
            Logger.debug("BackgroundService.Geo", "[binder] stopping");
            GeolocationService.this.stop();
        }

        public boolean isStarted() {
            return started;


        }
    }
}