package com.lagruva.backgroundservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
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
    public static Context context;
    private static BackgroundService serviceInstance;
    private static String sessionKey;
    private static String defaultReceiver;

    public static void start(Context context) {
        MessagingService.context = context;
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        /*FirebaseInstanceId
                .getInstance()
                .getInstanceId()
                .addOnSuccessListener(
                        context.getAc(),
                        new OnSuccessListener<InstanceIdResult>() {
                            @Override
                            public void onSuccess(InstanceIdResult instanceIdResult) {
                                Logger.debug("BackgroundService.Messaging","FCM Success. Token: " + instanceIdResult.getToken());
                            }
                        }
                );
        FirebaseInstanceId
                .getInstance()
                .getInstanceId()
                .addOnFailureListener(
                        new OnFailureListener() {
                            public void onFailure(Exception e) {
                                Logger.error("BackgroundService.Messaging","FCM Failure", e);
                            }
                        }
                );*/
        Logger.debug("BackgroundService.Messaging", "messagingService started and context set");
    }
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        serviceInstance = BackgroundService.getInstance();
        Logger.debug("BackgroundService.Messaging", "messagingService context set");
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
                Logger.debug("BackgroundService.Messaging", "login-ack session: " + sessionKey);
                Logger.debug("BackgroundService.Messaging", "login-ack senderId: " + remoteMessage.getSenderId());
                Logger.debug("BackgroundService.Messaging", "login-ack from: " + remoteMessage.getFrom());
            } catch (JSONException e) {
                Logger.error("BackgroundService.Messaging", "login-ack JSON error",e);
            }
        }
        Intent intent = new Intent(ACTION_MESSAGE);
                intent.putExtra("message", remoteMessage);
                LocalBroadcastManager.getInstance(
                        getApplicationContext()
                ).sendBroadcast(intent);
    }
    public void sendPong() {
        try {
            Logger.debug("BackgroundService.Messaging", "Sending pong");
            sendMessage(null,"pong",null);
            Logger.debug("BackgroundService.Messaging", "Service started? " + BackgroundService.started);

            BackgroundService.checkService(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void sendMessage(String receiver, String type, JSONObject data) {
        if (context == null) {
            Logger.warn("BackgroundService.Messaging", "ignoring sendMessage, context is null: " + type + ", "+ data);
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
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

            if (data == null || (!type.equals("login") && !data.has("key")))
                // Si no es un login y falta el dato del sessionKey, agrego el obtenido via login-ack
                Logger.debug("BackgroundService.Messaging", "adding key: "+sessionKey);
                builder.addData("key", sessionKey);
                
            if (data != null) {
                Iterator<String> keys = data.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String val = data.getString(key);
                    builder.addData(key, val);
                }
            }
            RemoteMessage message = builder.build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void callMethod(String model, String method, String args, String kwargs) {
        JSONObject data = new JSONObject();
        try {
            data.put("model", model);
            data.put("method", method);
            if (args != null) {
                data.put("args", args);
            }

            sendMessage(null,"rpc", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        // PushNotificationsPlugin.onNewToken(s);
    }
}
