package com.lagruva.backgroundservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;

import org.json.JSONObject;

import java.util.UUID;

public class ServiceReceiver extends BroadcastReceiver {

    private IBroadCastListener listener;
    public ServiceReceiver(IBroadCastListener listener) {
        this.listener = listener;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.debug("BackgroundService", "onReceive: " + intent);
        if (this.listener == null) {
            return;
        }
        this.listener.broadcastReceived(intent);
    }
}