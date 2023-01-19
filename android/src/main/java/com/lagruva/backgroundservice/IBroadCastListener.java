package com.lagruva.backgroundservice;

import android.content.Intent;

public interface IBroadCastListener{
    public void broadcastReceived(Intent intent);
}