package me.kptmusztarda.adbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import me.kptmusztarda.handylib.Logger;

public abstract class AccessibilityReceiver extends BroadcastReceiver {

    private static final String TAG = "AccessibilityReceiver";
    public boolean isRegistered;

    public Intent register(Context context, IntentFilter filter) {
        try {
            return !isRegistered ? context.registerReceiver(this, filter) : null;
        } finally {
            isRegistered = true;
            Logger.log(TAG, "BroadcastReceiver registered");
        }
    }

    public boolean unregister(Context context) {
        return isRegistered && unregisterInternal(context);
    }

    private boolean unregisterInternal(Context context) {
        context.unregisterReceiver(this);
        isRegistered = false;
        Logger.log(TAG, "BroadcastReceiver unregistered");
        return true;
    }
}
