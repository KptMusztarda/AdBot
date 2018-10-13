package me.kptmusztarda.adbot;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.Objects;

public class Accessibility extends AccessibilityService {

    private static final String TAG = "MainActivity";
    public static final String ACTION_BACK = "me.kptmusztarda.adbot.ACTION_BACK";
    public static final String ACTION_SERVICE_OFF = "me.kptmusztarda.adbot.ACTION_SERVICE_OFF";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received: " + intent.getAction());
            if(intent.getAction().equals(ACTION_BACK)) {
                performGlobalAction(GLOBAL_ACTION_BACK);
//            } else if(Objects.equals(intent.getAction(), ACTION_SERVICE_OFF)) {
//                stopSelf();
            }
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(receiver, new IntentFilter(ACTION_BACK));
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
