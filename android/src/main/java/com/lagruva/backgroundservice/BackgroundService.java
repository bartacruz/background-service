package com.lagruva.backgroundservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

import androidx.core.location.LocationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationResult;

import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;

import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BackgroundService implements IBroadCastListener{
    private static BackgroundService instance;
    private static Context context;
    protected static GeolocationService.LocalBinder locationService = null;
    private static ServiceReceiver receiver = null;
    private String partnerId;
    public static boolean started;

    public static BackgroundService getInstance(){
        if (BackgroundService.instance == null) {
            BackgroundService.instance = new BackgroundService();
        }
        return BackgroundService.instance;
    }
    public void init(Context context) {
        if (this.context != null) {
            Logger.debug("BackgroundService", "service already instantiated");
            return;
        }
        this.context = context;
        Logger.debug("BackgroundService", "init with context " + context);
        bindReceiver();
    }

    public static void start() {
        Logger.debug("BackgroundService", "starting GeolocationService...");
        started = true;
        checkService(null);
    }
    public static void stop() {
        started=false;
        if (locationService != null) {
            locationService.stop();
        }
    }

    /**
     * Checks if Geolocation Service is binded, and if we are started, checks if is started.
     * @param ctx
     */
    public static void checkService(Context ctx) {
        Context context = ctx;
        if (ctx == null){
            context = GeolocationService.context;
        }
        Logger.debug("BackgroundService", "Geolocation pluginContext: " + context);
        if (BackgroundService.locationService == null) {
            boolean result = context.bindService(
                    new Intent(context, GeolocationService.class),
                    new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder binder) {
                            Logger.debug("BackgroundService", "GeolocationService binder set: " + name);
                            BackgroundService.locationService = (GeolocationService.LocalBinder) binder;
                            Logger.debug("BackgroundService", "GeolocationService binder started?: " + BackgroundService.started);
                            if (BackgroundService.started) {
                                BackgroundService.locationService.start();
                            }
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            Logger.debug("BackgroundService", "GeolocationService serviceDisconnected: " + name);
                        }

                        @Override
                        public void onNullBinding(ComponentName name) {
                            ServiceConnection.super.onNullBinding(name);
                            Logger.debug("BackgroundService", "nullBinding: " + name);
                        }
                    },
                    Context.BIND_AUTO_CREATE
            );
        } else if (BackgroundService.started) {
            Logger.debug("BackgroundService", "GeolocationService starting... ");
            BackgroundService.locationService.start();
        }

    }

    /**
     * Binds the ServiceReceiver to receive Intents
     */
    public static void bindReceiver() {
        if (receiver == null) {
            Logger.debug("BackgroundService", "creating service receiver.");
            receiver = new ServiceReceiver(getInstance());
            IntentFilter filter = new IntentFilter();
            filter.addAction(GeolocationService.ACTION_LOCATION);
            filter.addAction(MessagingService.ACTION_MESSAGE);
            LocalBroadcastManager.getInstance(context).registerReceiver(
                    receiver,
                    filter
            );
            Logger.debug("BackgroundService", "service receiver binded.");
        } else {
            Logger.debug("BackgroundService", "service receiver already set.");
        }
    }

    public void broadcastReceived(Intent intent) {
        Logger.debug("BackgroundService", "service broadcastReceived: " + intent.getAction());
        if (intent.getAction().equals(GeolocationService.ACTION_LOCATION)) {
            Location location = intent.getParcelableExtra("location");
            Logger.debug("BackgroundService", "service broadcast location received: " + location);
            sendPosition(location);
        } else if (intent.getAction().equals(MessagingService.ACTION_MESSAGE)) {
            RemoteMessage message = intent.getParcelableExtra("message");
            Logger.debug("BackgroundService", "service broadcast FCM received: " + message);
            String message_type = message.getData().get("type");
            try {
                JSONObject data = new JSONObject(message.getData().get("data"));
                Logger.debug("BackgroundService", "service data: " + data.getClass() + "==" + data);
                if (message_type.equals("login-ack")) {
                    this.partnerId = data.get("partner_id").toString();
                    Logger.info("BackgroundService", "service login-ack. partnerId=" + this.partnerId);
                } else if (message_type.equals("login-nack")) {
                    this.partnerId = null;
                    Logger.debug("BackgroundService", "login nack partnerId=null");
                }
            } catch (JSONException e) {
                Logger.error("BackgroundService", "login nack partnerId=null",e);
            }
        }
    }

    /**
     * Sends location updates from GeolocationService to Lagruva server
     * via the Firebase MessagingService
     * @param location
     */
    private void sendPosition(Location location) {
        if (partnerId == null) {
            Logger.warn("BackgroundService","partnerId not set, ignoring position.");
            return;
        }
        JSONObject position = new JSONObject();
        try {
            position.put("latitude",location.getLatitude());
            position.put("longitude",location.getLongitude());
            position.put("accuracy",location.getAccuracy());
            position.put("partner_id",partnerId);
            String args = "[" + position.toString()+"]";
            Logger.info("BackgroundService", "sending position: " + args);
            MessagingService.callMethod("towing.pos","create-nr",args,null);
        } catch (Exception e) {
            Logger.error("BackgroundService", "Error sending position", e);
        }
    }
}
