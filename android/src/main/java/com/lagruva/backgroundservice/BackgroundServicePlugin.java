package com.lagruva.backgroundservice;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.plugin.util.AssetUtil;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Map;


@CapacitorPlugin(name = "BackgroundService", permissions = {
    @Permission(strings = {}, alias = "receive"),
    @Permission(
            strings = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION },
            alias = BackgroundServicePlugin.LOCATION
    ),
    @Permission(strings = { Manifest.permission.ACCESS_COARSE_LOCATION }, alias = BackgroundServicePlugin.COARSE_LOCATION),
    }
    )

public class BackgroundServicePlugin extends Plugin implements IBroadCastListener {
    static final String LOCATION = "location";
    static final String COARSE_LOCATION = "coarseLocation";
    static final String FOREGROUND_SERVICE = "foregroundService";
    private Boolean stoppedWithoutPermissions = false;
    protected GeolocationService.LocalBinder locationService = null;
    private ServiceConnection locationConnection;
    private ServiceReceiver receiver = null;
    public static Boolean started;

    public void load() {
        Logger.debug("BackgroundService.Plugin","started? " + BackgroundServicePlugin.started);
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        int resId = AssetUtil.getResourceID(getActivity().getApplicationContext(),"notifiacion_lagruva","drawable");
        bindReceiver();
        bindLocationService();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getContext().getSystemService(
                    Context.NOTIFICATION_SERVICE
            );
            NotificationChannel channel = new NotificationChannel(
                    "Lagruva",
                    "Background Tracking",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setDescription("Lagruva Desc");
            manager.createNotificationChannel(channel);
        }
    }

    @PluginMethod
    public void start(PluginCall call) {
        Map<String, PermissionState> permissionsResult = getPermissionStates();
        for (Map.Entry<String, PermissionState> entry : permissionsResult.entrySet()) {
            Logger.debug("Permission " + entry.getKey() + ": " + entry.getValue());

        }
//        Logger.debug("BackgroundService.Plugin", "Cheching permissions");
//        String alias = BACKGROUND_LOCATION;
//        requestAllPermissions(call, "completeCurrentPosition");
//        requestPermissionForAlias(alias, call, "completeCurrentPosition");
//        String alias = BACKGROUND_LOCATION;
//        if (getPermissionState(alias) != PermissionState.GRANTED) {
//            openSettings();
//            call.reject("No hay permisos");
//        }
//            Logger.debug("BackgroundService.Plugin", "Asking permissions");
//            requestPermissionForAlias(alias, call, "completeCurrentPosition");
//        }
//        Logger.debug("BackgroundService.Plugin", "request all permissions called");
        realStart();
    }
    public void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        intent.setData(uri);
        getContext().startActivity(intent);

    }
    private void realStart() {
        started = true;
        Logger.debug("BackgroundService.Plugin","location tracking started");
        Intent serviceIntent = new Intent(getContext(), GeolocationService.class);
        serviceIntent.putExtra("action", "start");
        ContextCompat.startForegroundService(getContext(), serviceIntent);
        Logger.debug("BackgroundService.Plugin","loaded");
    }
    @PluginMethod
    public void stop(PluginCall call) {
        started = false;
//        Intent serviceIntent = new Intent(getContext(), GeolocationService.class);
//        serviceIntent.putExtra("action", "stop");
//        getContext().stopService(serviceIntent);
        locationService.stop();
        Logger.debug("BackgroundService.Plugin","location tracking stopped");
        
    }
//    @PluginMethod
//    public void isStarted(PluginCall call) {
//
//        call.resolve(JSObject.);
//    }

    @PluginMethod()
    public void sendMessage(final PluginCall call) {
      try {
        JSONObject data = call.getObject("data");
        if (!data.has("type")) {
            Logger.warn("BackgroundService.Plugin","sendMessage - message has no type: "+ data);
            call.reject("Message has no type");
            return;
        }
        String receiver = call.getString("receiver");
        String type = data.remove("type").toString();
        MessagingService.sendMessage(getContext(),receiver, type,data);
        call.resolve(JSObject.fromJSONObject(data));
      } catch (Exception e) {
        e.printStackTrace();
        call.reject("BackgroundService.Plugin Error sending message", e);
      }
    }

    /**
     * Completes the getCurrentPosition plugin call after a permission request
     * @see #start(PluginCall)
     * @param call the plugin call
     */
    @PermissionCallback
    private void completeCurrentPosition(PluginCall call) {
        if (getPermissionState(BackgroundServicePlugin.LOCATION) == PermissionState.GRANTED) {
            Logger.debug("BackgroundService","complete: Permissions granted");
            realStart();
            call.resolve();
        } else {
            call.reject("BackgroundService","complete: Location permission was denied");
        }
    }

    public void bindReceiver() {
        if (receiver == null) {
            Logger.debug("BackgroundService.Plugin", "creating service receiver.");
            receiver = new ServiceReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(GeolocationService.ACTION_LOCATION);
            filter.addAction(MessagingService.ACTION_MESSAGE);
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                    receiver,
                    filter
            );

            Logger.debug("BackgroundService.Plugin", "ServiceReceiver binded.");
        } else {
            Logger.debug("BackgroundService.Plugin", "ServiceReceiver already set.");
        }

    }

    public void broadcastReceived(Intent intent) {
        Logger.debug("BackgroundService.Plugin", "plugin broadcastReceived: " + intent.getAction());
        if (intent.getAction().equals(GeolocationService.ACTION_LOCATION)) {
            Location location = intent.getParcelableExtra("location");
            Logger.debug("BackgroundService.Plugin", "broadcast location received: " + location);
            fireLocation(location);

        } else if (intent.getAction().equals(MessagingService.ACTION_MESSAGE)) {
            RemoteMessage message = intent.getParcelableExtra("message");
            Logger.debug("BackgroundService.Plugin", "plugin broadcast FCM received: " + message);
            fireNotification(message);
        }
    }

    public void fireLocation(Location location) {
        Logger.debug("BackgroundService.Plugin","fireLocation "+location);
        JSObject remoteMessageData = getJSObjectForLocation(location);
        Logger.debug("BackgroundService.Plugin","firing location "+remoteMessageData);
        notifyListeners("positionReceived", remoteMessageData, true);
    }

    public void fireNotification(RemoteMessage remoteMessage) {
        Logger.debug("BackgroundService.Plugin","fire "+remoteMessage);
        JSObject remoteMessageData = new JSObject();

        JSObject data = new JSObject();
        remoteMessageData.put("id", remoteMessage.getMessageId());
        for (String key : remoteMessage.getData().keySet()) {
            Object value = remoteMessage.getData().get(key);
            data.put(key, value);
        }
        remoteMessageData.put("data", data);
        Logger.debug("BackgroundService.Plugin","fire data"+data);

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification != null) {
            remoteMessageData.put("title", notification.getTitle());
            remoteMessageData.put("body", notification.getBody());
            remoteMessageData.put("click_action", notification.getClickAction());

            Uri link = notification.getLink();
            if (link != null) {
                remoteMessageData.put("link", link.toString());
            }
        }
        Logger.debug("BackgroundService.Plugin","firing "+remoteMessageData);
        notifyListeners("pushNotificationReceived", remoteMessageData, true);
    }

    private static JSObject getJSObjectForLocation(Location location) {
        JSObject ret = new JSObject();
        JSObject coords = new JSObject();
        ret.put("coords", coords);
        ret.put("timestamp", location.getTime());
        coords.put("latitude", location.getLatitude());
        coords.put("longitude", location.getLongitude());
        coords.put("accuracy", location.getAccuracy());
        coords.put("altitude", location.getAltitude());
        if (Build.VERSION.SDK_INT >= 26) {
            coords.put("altitudeAccuracy", location.getVerticalAccuracyMeters());
        }
        coords.put("speed", location.getSpeed());
        coords.put("heading", location.getBearing());
        return ret;
    }

    private void bindLocationService() {
        this.locationConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Logger.debug("BackgroundService.plugin", "GeolocationService binder set: " + name);
                locationService = (GeolocationService.LocalBinder) binder;
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Logger.debug("BackgroundService.plugin", "Geolocation serviceDisconnected: " + name);
            }
            @Override
            public void onNullBinding(ComponentName name) {
                ServiceConnection.super.onNullBinding(name);
                Logger.debug("BackgroundService.plugin", "nullBinding: " + name);
            }
        };

        getContext().bindService(
                new Intent(getContext(), GeolocationService.class),
                locationConnection,
                Context.BIND_AUTO_CREATE
        );
        Logger.debug("BackgroundService.plugin", "locationService bind called: ");
    }

    @Override
    protected void handleOnResume() {
        Logger.debug("BackgroundService.Plugin","handleOnResume");
        super.handleOnResume();
    }

    @Override
    protected void handleOnPause() {
        Logger.debug("BackgroundService.Plugin","handleOnPause");
        stoppedWithoutPermissions = !hasRequiredPermissions();
        super.handleOnPause();
    }

    @Override
    protected void handleOnDestroy() {
        Logger.debug("BackgroundService.Plugin","handleOnDestroy");

        if (locationConnection != null) {
            if (locationService.isStarted()) {
                Logger.debug("BackgroundService.Plugin","grolocationService is started. trying to keep it that way");
                Intent serviceIntent = new Intent(getContext(), GeolocationService.class);
                serviceIntent.putExtra("action", "start");
                ContextCompat.startForegroundService(getContext(), serviceIntent);
            }
            Logger.debug("BackgroundService.Plugin","unbinding locationConnection");
            getContext().unbindService(locationConnection);
        }

        super.handleOnDestroy();
    }

}
