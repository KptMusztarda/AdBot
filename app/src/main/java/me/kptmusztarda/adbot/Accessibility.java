package me.kptmusztarda.adbot;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;

import java.util.Timer;
import java.util.TimerTask;

import me.kptmusztarda.handylib.Logger;


public class Accessibility extends AccessibilityService {

    private static final String TAG = "Accessibility";
    public static final String ACTION_BACK = "me.kptmusztarda.adbot.ACTION_BACK";
    private Timer timer;
    private Context context;

    private AccessibilityReceiver receiver = new AccessibilityReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.log(TAG, "Received: " + intent.getAction());
            if(intent.getAction().equals(ACTION_BACK)) {
                Logger.log(TAG, "GLOBAL_ACTION_BACK performed successfully? " + Boolean.toString(performGlobalAction(GLOBAL_ACTION_BACK)));
            }
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        context = this;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                receiver.register(context, new IntentFilter(ACTION_BACK));
            }
        }, 0, 10*60*1000);
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        receiver.unregister(context);
        if(timer != null) {
            timer.cancel();
            timer.purge();
        }
    }
}
