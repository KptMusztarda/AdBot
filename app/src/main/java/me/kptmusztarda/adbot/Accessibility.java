package me.kptmusztarda.adbot;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class Accessibility extends AccessibilityService {

    private static final String TAG = "MainActivity";
    public static final String ACTION_BACK = "me.kptmusztarda.adbot.ACTION_BACK";
    public static final String ACTION_SWIPE = "me.kptmusztarda.adbot.ACTION_SWIPE";
    public static final String ACTION_SERVICE_OFF = "me.kptmusztarda.adbot.ACTION_SERVICE_OFF";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received: " + intent.getAction());
            if(intent.getAction().equals(ACTION_BACK)) {
                performGlobalAction(GLOBAL_ACTION_BACK);
//            } else if(Objects.equals(intent.getAction(), ACTION_SERVICE_OFF)) {
//                stopSelf();
            } else if(intent.getAction().equals(ACTION_SWIPE)) {
                performGlobalAction(GLOBAL_ACTION_HOME);
//                GestureDescription.Builder builder = new GestureDescription.Builder();
//                Path path = new Path();
//
//                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//                Display display = wm.getDefaultDisplay();
//                Point size = new Point();
//                display.getSize(size);
//                int width = size.x;
//                int height = size.y;
//
//
//                path.moveTo(width*0.5f, height*0.4f);
//                path.lineTo(width*0.5f,height*0.6f);
//                builder.addStroke(new GestureDescription.StrokeDescription(path, 20,100));
//                GestureDescription gesture = builder.build();
//                Log.i(TAG, "dispathed? " + Boolean.toString(dispatchGesture(gesture, null, null)));
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
