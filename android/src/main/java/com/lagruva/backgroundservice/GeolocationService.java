package com.lagruva.backgroundservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.getcapacitor.Logger;

import org.json.JSONObject;


public class GeolocationService extends Service {
    public int ONGOING_NOTIFICATION_ID = 9876543;
    private final IBinder binder = new LocalBinder();
    protected boolean started = false;
    protected Notification.Builder notification;
    private FusedLocationProviderClient client;
    private static LocationCallback callback;
    public static Context context;
    static final String ACTION_BROADCAST = (
            GeolocationService.class.getPackage().getName() + ".broadcast"
    );
    public static final String ACTION_LOCATION = (
            GeolocationService.class.getPackage().getName() + ".location"
    );

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        Logger.debug("BackgroundService.Geo", "context set: " + context);
        //start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void updateSubText(String subtext) {
        Notification updated = notification.setSubText(subtext).build();
        NotificationManager manager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE
        );
        manager.notify(ONGOING_NOTIFICATION_ID,updated);
    }
    public void start() {
        if (notification == null) {
            setupNotification();
        }
        Logger.debug("BackgroundService.Geo", "to foreground. callback=" + callback);
        // Notification ID cannot be 0.
        updateSubText("En servicio");

        startForeground(ONGOING_NOTIFICATION_ID, notification.build());
        if ( callback == null)  {
            locationWatch();
        }
        started = true;
    }

    public void stop() {
        Logger.debug("BackgroundService.Geo", "to background");
        if (client != null && callback != null)  {
            client.removeLocationUpdates(callback);
            callback = null;
        }
        updateSubText("Background");
        stopForeground(true);
        started = false;
    }

    private void setupNotification() {
        Context context = getApplicationContext();
        int resId = AssetUtil.getResourceID(context,"notificacion_lagruva","drawable");
        Logger.debug("BackgroundService.Geo", "notification icon : " + resId);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle("Registrando tu ubicación")
                .setContentText("Presione para ir a la aplicación")
                .setSmallIcon(resId)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis());

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(
                context.getPackageName()
        );
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            Logger.debug("BackgroundService.Geo", "Launch intent service: " + launchIntent);
            builder.setContentIntent(
                    PendingIntent.getActivity(
                            context,
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

    private void locationWatch() {
        int distanceFilter = 10;
        if (client == null) {
            client = LocationServices.getFusedLocationProviderClient(
                    getApplicationContext()
            );
            Logger.debug("BackgroundService.Geo", "created new client");
        }
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setMaxWaitTime(2000);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(distanceFilter);

        LocationCallback callback = new LocationCallback(){
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
                        getApplicationContext()
                ).sendBroadcast(intent);
            }
            @Override
            public void onLocationAvailability(LocationAvailability availability) {
                if (!availability.isLocationAvailable() && BuildConfig.DEBUG) {
                    Logger.debug("BackgroundService.Geo", "Location not available");
                }
            }
        };

        Logger.debug("BackgroundService.Geo", "created new request");
        client.requestLocationUpdates(
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



     // Handles requests from the activity.
    public class LocalBinder extends Binder {
        public void start() {
            Logger.debug("BackgroundService.Geo", "[binder] to foreground");
            GeolocationService.this.start();
        }
         public void stop() {
             GeolocationService.this.stop();
         }
         public boolean isStarted() {
            return started;
         }
         public Context getContext() {return GeolocationService.this.getApplicationContext();}
     }
}