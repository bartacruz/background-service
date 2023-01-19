package com.lagruva.backgroundservice;
import android.Manifest;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.getcapacitor.Logger;
import com.getcapacitor.plugin.util.AssetUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Map;


@CapacitorPlugin(name = "BackgroundService", permissions = {
    @Permission(strings = {}, alias = "receive"),
    @Permission(
            strings = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION },
            alias = BackgroundServicePlugin.LOCATION
    ),
    @Permission(strings = { Manifest.permission.ACCESS_COARSE_LOCATION }, alias = BackgroundServicePlugin.COARSE_LOCATION),
    @Permission(strings = { Manifest.permission.ACCESS_BACKGROUND_LOCATION}, alias = BackgroundServicePlugin.BACKGROUND_LOCATION),
    @Permission(strings = { Manifest.permission.FOREGROUND_SERVICE}, alias = BackgroundServicePlugin.FOREGROUND_SERVICE),}
    )
public class BackgroundServicePlugin extends Plugin implements IBroadCastListener {
    static final String LOCATION = "location";
    static final String COARSE_LOCATION = "coarseLocation";
    static final String BACKGROUND_LOCATION = "backgroundLocation";
    static final String FOREGROUND_SERVICE = "foregroundService";
    private Boolean stoppedWithoutPermissions = false;
    private BackgroundService implementation;
//    public MessagingService firebaseMessagingService;
    protected GeolocationService.LocalBinder testService = null;
    private ServiceReceiver receiver = null;

    public static Boolean started;

    public void load() {
        Logger.debug("BackgroundService","started? " + BackgroundServicePlugin.started);
        int resId = AssetUtil.getResourceID(getActivity().getApplicationContext(),"notifiacion_lagruva","drawable");
        bindReceiver(getContext());
        implementation = BackgroundService.getInstance();
        implementation.init(getActivity().getApplicationContext());
        MessagingService.start(getActivity().getApplicationContext());

        Logger.debug("BackgroundService","loaded");

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
        boolean result = getContext().bindService(
                new Intent(getContext(), GeolocationService.class),
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        Logger.debug("BackgroundService", "GeolocationService binder set: " + name);
                        testService = (GeolocationService.LocalBinder) binder;
                        Logger.debug("BackgroundService", "my context : " + getContext());
                        Logger.debug("BackgroundService", "geo context: " + GeolocationService.context);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Logger.debug("BackgroundService", "serviceDisconnected: " + name);
                    }

                    @Override
                    public void onNullBinding(ComponentName name) {
                        ServiceConnection.super.onNullBinding(name);
                        Logger.debug("BackgroundService", "nullBinding: " + name);
                    }
                },
                Context.BIND_AUTO_CREATE
        );

    }

    @PluginMethod
    public void start(PluginCall call) {
        Map<String, PermissionState> permissionsResult = getPermissionStates();
        for (Map.Entry<String, PermissionState> entry : permissionsResult.entrySet()) {
            Logger.debug("Permission "+entry.getKey()+": "+entry.getValue());

        }
        String alias = COARSE_LOCATION;
        if (getPermissionState(alias) != PermissionState.GRANTED) {
            requestPermissionForAlias(alias, call, "completeCurrentPosition");
        }
        started = true;
        Logger.debug("BackgroundService","plugin started");
        BackgroundService.start();
    }
    @PluginMethod
    public void stop(PluginCall call) {
        started = false;
        Logger.debug("BackgroundService","plugin stopped");
        BackgroundService.stop();
    }

    @PluginMethod()
    public void sendMessage(final PluginCall call) {
      try {

        JSONObject data = call.getObject("data");
        if (!data.has("type")) {
            Logger.warn("BackgroundService","sendMessage - message has no type: "+ data);
            call.reject("Message has no type");
            return;
        }
        String receiver = call.getString("receiver");
        String type = data.remove("type").toString();
        MessagingService.sendMessage(receiver, type,data);
        call.resolve(JSObject.fromJSONObject(data));
      } catch (Exception e) {
        e.printStackTrace();
        call.reject("Error sending message", e);
      }
    }

    /**
     * Completes the getCurrentPosition plugin call after a permission request
     * @see #getCurrentPosition(PluginCall)
     * @param call the plugin call
     */
    @PermissionCallback
    private void completeCurrentPosition(PluginCall call) {
        if (getPermissionState(BackgroundServicePlugin.COARSE_LOCATION) == PermissionState.GRANTED) {
            Logger.debug("BacgroundService","Permissions granted");
            call.resolve();
        } else {
            call.reject("Location permission was denied");
        }
    }

    public void bindReceiver(Context context) {
        if (receiver == null) {
            Logger.debug("BackgroundService", "creating service receiver.");
            receiver = new ServiceReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(GeolocationService.ACTION_LOCATION);
            filter.addAction(MessagingService.ACTION_MESSAGE);
            LocalBroadcastManager.getInstance(context).registerReceiver(
                    receiver,
                    filter
            );

            Logger.debug("BackgroundService", "plugin receiver binded.");
        } else {
            Logger.debug("BackgroundService", "plugin service receiver already set.");
        }

    }

    public void broadcastReceived(Intent intent) {
        Logger.debug("BackgroundService", "plugin broadcastReceived: " + intent.getAction());
        if (intent.getAction().equals(GeolocationService.ACTION_LOCATION)) {
            Location location = intent.getParcelableExtra("location");
            Logger.debug("BackgroundService", "broadcast location received: " + location);
            fireLocation(location);

        } else if (intent.getAction().equals(MessagingService.ACTION_MESSAGE)) {
            RemoteMessage message = intent.getParcelableExtra("message");
            Logger.debug("BackgroundService", "plugin broadcast FCM received: " + message);
            fireNotification(message);
        }
    }

    public void fireLocation(Location location) {
        Logger.debug("BackgroundService.Plugin","fireLocation "+location);
        JSObject remoteMessageData = getJSObjectForLocation(location);
        Logger.debug("BackgroundService","firing location "+remoteMessageData);
        notifyListeners("positionReceived", remoteMessageData, true);
    }

    public void fireNotification(RemoteMessage remoteMessage) {
        Logger.debug("BackgroundService","fire "+remoteMessage);
        JSObject remoteMessageData = new JSObject();

        JSObject data = new JSObject();
        remoteMessageData.put("id", remoteMessage.getMessageId());
        for (String key : remoteMessage.getData().keySet()) {
            Object value = remoteMessage.getData().get(key);
            data.put(key, value);
        }
        remoteMessageData.put("data", data);
        Logger.debug("BackgroundService","fire data"+data);

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
        Logger.debug("BackgroundService","firing "+remoteMessageData);
        notifyListeners("pushNotificationReceived", remoteMessageData, true);
    }

    private JSObject getJSObjectForLocation(Location location) {
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

    @Override
    protected void handleOnResume() {
        Logger.debug("BackgroundService","handleOnResume");
        super.handleOnResume();
    }

    @Override
    protected void handleOnPause() {
        Logger.debug("BackgroundService","handleOnPause");
        stoppedWithoutPermissions = !hasRequiredPermissions();
        super.handleOnPause();
    }

    @Override
    protected void handleOnDestroy() {
        Logger.debug("BackgroundService","handleOnDestroy");
        if (BackgroundServicePlugin.started) {
            BackgroundService.stop();
            BackgroundService.start();
        }

        super.handleOnDestroy();
    }

}
