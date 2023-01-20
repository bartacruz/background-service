package com.lagruva.backgroundservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.getcapacitor.JSObject;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.getcapacitor.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.UUID;

public class MessagingService extends FirebaseMessagingService {
    static final String ACTION_MESSAGE = (
            MessagingService.class.getPackage().getName() + ".message"
    );
    private static String sessionKey;
    private static String defaultReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.debug("BackgroundService.Messaging", "onCreate");
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);

    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Logger.debug("BackgroundService.Messaging","onMessageReceived: " + remoteMessage);
        super.onMessageReceived(remoteMessage);
        String message_type =remoteMessage.getData().get("type");
        Logger.debug("BackgroundService.Messaging","message_type: " + message_type);
        if (message_type.equals("ping")) {
            sendPong();
        } else if (message_type.equals("login-ack")) {
            try {
                JSONObject data = new JSONObject(remoteMessage.getData().get("data"));
                sessionKey = data.getString("key");
                defaultReceiver = remoteMessage.getSenderId();
                String partner_id = data.getString("partner_id");
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString("partner_id",partner_id)
                        .putString("sessionKey",sessionKey)
                        .putString("receiver",defaultReceiver + "@fcm.googleapis.com")
                        .apply();
            } catch (JSONException e) {
                Logger.error("BackgroundService.Messaging", "login-ack JSON error",e);
            }
        }
        // TODO: agregar login-nack para borrar los datos.

        Intent intent = new Intent(ACTION_MESSAGE);
                intent.putExtra("message", remoteMessage);
                LocalBroadcastManager.getInstance(
                        getApplicationContext()
                ).sendBroadcast(intent);
    }

    public void sendPong() {
        try {
            Logger.debug("BackgroundService.Messaging", "Sending pong");
            sendMessage(this,null,"pong",null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(Context context, String receiver, String type, JSONObject data) {
        if (context == null) {
            Logger.warn("BackgroundService.Messaging", "context is null. ignoring msg: " + data);
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (receiver == null) {
            if (defaultReceiver != null) {
                receiver = defaultReceiver + "@fcm.googleapis.com";
                Logger.debug("BackgroundService.Messaging", "using receiver from login-ack: " + receiver);
            } else {
                receiver = sharedPreferences.getString("receiver",null);
                Logger.debug("BackgroundService.Messaging", "using receiver from preferences: " + receiver);
            }
        }
//        SharedPreferences sharedPreferences = context.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
//        String sessionKey = sharedPreferences.getString("sessionKey", "NSNC");
//        String receiver = "845928467018@fcm.googleapis.com";
        try {
            RemoteMessage.Builder builder = new RemoteMessage.Builder(receiver)
                    .setTtl(5)
                    .setMessageId(UUID.randomUUID().toString());
            builder.addData("type", type);

            if (!type.equals("login") && (data == null || !data.has("key") )) {
                // Si no es un login y falta el dato del sessionKey, agrego el obtenido via login-ack
                if (sessionKey == null) {
                    Logger.debug("BackgroundService.Messaging", "sessionKey is null getting from prefs");
                    sessionKey = sharedPreferences.getString("sessionKey", null);
                }
                Logger.debug("BackgroundService.Messaging", "adding key for " + type + ": " + sessionKey);
                builder.addData("key", sessionKey);
            }
            if (data != null) {
                Iterator<String> keys = data.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String val = data.getString(key);
                    builder.addData(key, val);
                }
            }
            RemoteMessage message = builder.build();
            Logger.debug("BackgroundService.Messaging", "sending: "+message);
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void callMethod(Context context, String model, String method, String args, String kwargs) {
        JSONObject data = new JSONObject();
        try {
            data.put("model", model);
            data.put("method", method);
            if (args != null) {
                data.put("args", args);
            }

            sendMessage(context,null,"rpc", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        // PushNotificationsPlugin.onNewToken(s);
    }
    // Handles requests from the activity.
    public class LocalBinder extends Binder {
        public void callMethod(Context context, String model, String method, String args, String kwargs) {
            MessagingService.this.callMethod(context, model,method,args,kwargs);
        }
        public void sendMessage(Context context, String receiver, String type, JSONObject data) {
            MessagingService.this.sendMessage(context, receiver,type,data);
        }
    }
}
