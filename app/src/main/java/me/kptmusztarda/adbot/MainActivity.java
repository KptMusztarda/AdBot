package me.kptmusztarda.adbot;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.time.TimeTCPClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String EXPIRE_DATE = "21/10/2018";
    private static final int ADMIN_INTENT = 15;
    private static final int SCALE = 100;
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;
    private Switch adminSwitch, mainSwitch;
    private SeekBar delayLockSeekBar, delayUnlockSeekBar, delayCloseSeekBar;
    private SharedPreferences pref;
    private SharedPreferences.Editor prefEditor;
    private boolean active;
    private int delayClose, delayLock, delayUnlock, cycle, adsleft, initialAds;
    private String loop;
    private boolean switchingMainSwitch = false;


    private static final String permissions[] = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };



    private void checkPermissions() {
        for(String p : permissions)
            if(ContextCompat.checkSelfPermission(this, p) != 0) {
                ActivityCompat.requestPermissions(this, permissions, 2137);
                break;
            }
        prefEditor.putBoolean("first_permission_check", false);
        prefEditor.commit();
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
////        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
////            prefEditor.putBoolean("first_permission_check", false);
////        }
//
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = getPreferences(MODE_PRIVATE);
        prefEditor = pref.edit();

        if(pref.getBoolean("first_permission_check", true)) {
            checkPermissions();
        }

        active = getIntent().getBooleanExtra("ACTIVE", false);

        Logger.setDirectory("", "adbot.log");
        Logger.log(TAG, "Starting activity with ACTIVE: " + active);

        unlock();

        setupViews();
        setDelays();

        adsleft = getIntent().getIntExtra("ads", -1);
        initialAds = pref.getInt("ads", 0);

        Logger.log(TAG, "Ads left: " + adsleft);
        Logger.log(TAG, "Initial ads: " + initialAds);

        if(active && isReadyToGo() && (adsleft != 0)) {
//            if(adsleft == initialAds) {
//                loop(true);
//            } else
                loop(false);
            adsleft--;
            if(adsleft == 0) {
                Logger.log(TAG, "Closing ad in " + delayClose/1000D + " seconds");
                new Handler().postDelayed(this::closeAd, delayClose);
                mainSwitch.setChecked(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        active = getIntent().getBooleanExtra("ACTIVE", false);
        setDelays();
        switchingMainSwitch = true;
        mainSwitch.setChecked(active);
        switchingMainSwitch = false;
    }

    private void setupViews() {
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mComponentName = new ComponentName(this, AdminReceiver.class);

        adminSwitch = findViewById(R.id.admin_switch);
        adminSwitch.setChecked(mDevicePolicyManager.isAdminActive(mComponentName));
        adminSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getResources().getString(R.string.admin_description));
                startActivityForResult(intent, ADMIN_INTENT);
            } else {
                mDevicePolicyManager.removeActiveAdmin(mComponentName);
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.admin_removed), Toast.LENGTH_SHORT).show();
            }
        });

        Switch accessibilitySwitch = findViewById(R.id.accessibility_switch);
        accessibilitySwitch.setChecked(isAccessibilityEnabled());
        accessibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        mainSwitch = findViewById(R.id.switch1);
        mainSwitch.setChecked(active);
        mainSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!switchingMainSwitch) {
                if (isChecked) {
                    active = true;
                    if (isReadyToGo()) {

                        if (!isExpired()) {

                            int largeCycle = pref.getInt("delay", 10 * 60 *1000);
                            if (initialAds > 0) adsleft = initialAds;
                            else if (initialAds == 0) adsleft = -1;

                            if(largeCycle == 0) {

                                loop(true);

                            } else {
                                setAlarm(largeCycle);
                            }

                        } else {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.expired), Toast.LENGTH_SHORT).show();
                            buttonView.setChecked(false);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_permissions), Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }
                } else {
                    Intent intent = getIntent();
                    intent.removeExtra("ACTIVE");
                    intent.putExtra("ACTIVE", false);
                    setIntent(intent);
                    active = false;
                }
            }
        });

        final TextView delayLockTextView = findViewById(R.id.delay_before_locking_value);
        delayLockSeekBar = findViewById(R.id.delay_before_locking_bar);
        delayLockSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                delayLockTextView.setText(Double.toString(i*SCALE/1000D));
                delayLock = i*SCALE;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefEditor.putInt("lock", seekBar.getProgress()*SCALE);
                prefEditor.commit();
            }
        });
        
        final TextView delayUnlockTextView = findViewById(R.id.delay_before_unlocking_value);
        delayUnlockSeekBar = findViewById(R.id.delay_before_unlocking_bar);
        delayUnlockSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                delayUnlockTextView.setText(Double.toString(i*SCALE/1000D));
                delayUnlock = i*SCALE;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefEditor.putInt("unlock", seekBar.getProgress()*SCALE);
                prefEditor.commit();
            }
        });

        final TextView delayCloseTextView = findViewById(R.id.delay_before_closing_value);
        delayCloseSeekBar = findViewById(R.id.delay_before_closing_bar);
        delayCloseSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                delayCloseTextView.setText(Double.toString(i*SCALE/1000D));
                delayClose = i*SCALE;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefEditor.putInt("close", seekBar.getProgress()*SCALE);
                prefEditor.commit();
            }
        });

        EditText delayEditText = findViewById(R.id.minutes_editText);
        delayEditText.setText(Integer.toString(pref.getInt("delay", 10)));
        delayEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0) {
                    int delay = Integer.parseInt(s.toString());
                    if(delay >=0) {
                        prefEditor.putInt("delay", delay);
                        prefEditor.commit();
                    } else delayEditText.setText("0");
                }
            }
        });
        delayEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus && delayEditText.getText().length() < 1) delayEditText.setText("0");
        });

        EditText adsEditText = findViewById(R.id.ads_editText);
        adsEditText.setText(Integer.toString(pref.getInt("ads", 10)));
        adsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0) {
                    int ads = Integer.parseInt(s.toString());
                    if(ads >=0) {
                        initialAds = ads;
                        prefEditor.putInt("ads", ads);
                        prefEditor.commit();
                    } else adsEditText.setText("0");
                }
            }
        });
        adsEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus && adsEditText.getText().length() < 1) adsEditText.setText("0");
        });
    }

    private void loop(boolean first) {

        Logger.log(TAG, "Loop!!! first? " + Boolean.toString(first));

        if(!first) {
            if(active) {
                Logger.log(TAG, "Closing ad in " + delayClose/1000D + " seconds");
                new Handler().postDelayed(this::closeAd, delayClose);
            }
        } else {

        }

        int delayLock;
        if(!first) delayLock = this.delayClose + this.delayLock;
        else delayLock = 0;
        if(active) {
            Logger.log(TAG, "Locking in " + delayLock/1000D + " seconds");
            new Handler().postDelayed(this::lock, delayLock);
        }

        int delayUnlock;
        if(!first) delayUnlock = this.delayClose + this.delayLock + this.delayUnlock;
        else delayUnlock = this.delayUnlock;
        if(active) {
            Logger.log(TAG, "Unlocking in " + delayUnlock/1000D + " seconds");
            new Handler().postDelayed(() -> restartActivity(), delayUnlock);
        }


    }

    private void setDelays() {
        delayLock = pref.getInt("lock", 3000);
        delayUnlock = pref.getInt("unlock", 3000);
        delayClose = pref.getInt("close", 3000);
        cycle = delayLock + delayUnlock + delayClose;

        delayLockSeekBar.setProgress(delayLock/SCALE);
        delayUnlockSeekBar.setProgress(delayUnlock/SCALE);
        delayCloseSeekBar.setProgress(delayClose/SCALE);
    }

    private void unlock() {
        Logger.log(TAG, "Unlocking");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK,
                "AdBot::WakeLock");
        wakeLock.acquire(cycle);

        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        Logger.log(TAG, "Is keyguard locked? " + Boolean.toString(keyguardManager.isKeyguardLocked()));
        if(keyguardManager.isKeyguardLocked()) {
            if(android.os.Build.VERSION.SDK_INT >= 26)keyguardManager.requestDismissKeyguard(this, null);
            else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 0);
        }
    }

    private void lock() {
        if(active) {
            Logger.log(TAG, "Locking");
            mDevicePolicyManager.lockNow();
            finish();
        }
    }

    private void restartActivity() {
        if(active && adsleft != 0) {
            Logger.log(TAG, "Restarting activity");
            Intent intent = getIntent();
            intent.removeExtra("ACTIVE");
            intent.putExtra("ACTIVE", active);
            intent.putExtra("ads", adsleft);
            finish();
            startActivity(intent);

        }
    }

    private void closeAd() {
        if(active) {
            Logger.log(TAG, "Back press");
            sendBroadcast(new Intent(Accessibility.ACTION_BACK));
        }
    }

    private void setAlarm(int x) {
        Logger.log(TAG, "Setting alarm: " + x + " minute(s)");
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = getIntent();
        intent.removeExtra("ACTIVE");
        intent.putExtra("ACTIVE", active);
        intent.putExtra("ads", initialAds);
        PendingIntent alarmIntent = PendingIntent.getActivity(this, 0, intent, 0);

        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 500,
                x*60*1000,
                alarmIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADMIN_INTENT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.admin_success), Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.admin_failure), Toast.LENGTH_SHORT).show();
                adminSwitch.setChecked(false);
            }
        }
    }

    private boolean isReadyToGo() {
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mComponentName = new ComponentName(this, AdminReceiver.class);
        return mDevicePolicyManager.isAdminActive(mComponentName) && isAccessibilityEnabled();
    }

    private boolean isAccessibilityEnabled(){
        int accessibilityEnabled = 0;
        final String ACCESSIBILITY_SERVICE_NAME = "me.kptmusztarda.adbot/me.kptmusztarda.adbot.Accessibility";
        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(this.getContentResolver(),android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
//            Log.d(TAG, "ACCESSIBILITY: " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled==1){
//            Log.d(TAG, "***ACCESSIBILIY IS ENABLED***: ");


            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
//            Log.d(TAG, "Setting: " + settingValue);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();
//                    Log.d(TAG, "Setting: " + accessabilityService);
                    if (accessabilityService.equalsIgnoreCase(ACCESSIBILITY_SERVICE_NAME)){
                        Log.d(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }

            Log.d(TAG, "***END***");
        }
        else{
            Log.d(TAG, "***ACCESSIBILIY IS DISABLED***");
        }
        return accessibilityFound;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        active = false;
    }

    private boolean isExpired() {
        final boolean expired[] = new boolean[1];

        expired[0] = pref.getBoolean("expired", false);

        if(!expired[0]) {

            Thread thread = new Thread(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                Date strDate;
                expired[0] = false;
                try {
                    strDate = sdf.parse(EXPIRE_DATE);
                    if (getTime().after(strDate)) {
                        prefEditor.putBoolean("expired", true);
                        prefEditor.commit();
                        expired[0] = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return expired[0];
    }

    public Date getTime() {
        Date date;
        try {
            TimeTCPClient client = new TimeTCPClient();
            try {
                client.setDefaultTimeout(60000);
                client.connect("time.nist.gov");
                date = client.getDate();
                return date;
            } finally {
                client.disconnect();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

}
