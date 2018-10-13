package me.kptmusztarda.adbot;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import org.apache.commons.net.time.TimeTCPClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int ADMIN_INTENT = 15;
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;
    private Switch adminSwitch, accessibilitySwitch;
    private boolean firstCheck = true;
    private boolean accessibilityCheck;
    private int active;
    private int delay = 2000;

    private static final String permissions[] = {
            Manifest.permission.WAKE_LOCK,
    };

//    private void checkPermissions() {
//        for(String p : permissions)
//            if(ContextCompat.checkSelfPermission(this, p) != 0) {
//                ActivityCompat.requestPermissions(this, permissions, 2137);
//                break;
//            }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) android.os.Process.killProcess(android.os.Process.myPid());
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        checkPermissions();

//        serviceIntent = new Intent(getApplicationContext(), MainService.class);

//        registerReceiver(adminReceiver, new IntentFilter("android.app.action.DEVICE_ADMIN_ENABLED"), "android.permission.BIND_DEVICE_ADMIN", null);



        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        active = getIntent().getIntExtra("ACTIVE", 0);
//        Log.i(TAG, "Activity started with ACTION extra = " + active);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Log.i(TAG, "Tiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiime = " + getTime());
//            }
//        }).start();



        Switch mainSwitch = findViewById(R.id.switch1);
        mainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    active = 1;

                    if(isReadyToGo()) {

                        final boolean expired[] = {false};

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                                Date strDate;
                                expired[0] = false;
                                try {
                                    strDate = sdf.parse("15/10/2018");
                                    if (getTime().after(strDate)) {
                                        expired[0] = true;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        if(!expired[0]) start();
                        else {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.expired), Toast.LENGTH_SHORT).show();
                            buttonView.setChecked(false);
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_permissions), Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }

                } else active = 0;
            }
        });
        mainSwitch.setChecked(active == 1);

        Log.i(TAG, "isReadyToGo? " + Boolean.toString(isReadyToGo()));

        if(active == 1 && isReadyToGo()) {
            Log.i(TAG, "Delaying Back press");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(active == 1) {
                        Log.i(TAG, "Back press");
                        sendBroadcast(new Intent(Accessibility.ACTION_BACK));
                    }
                }
            }, delay);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mComponentName = new ComponentName(this, AdminReceiver.class);

        adminSwitch = findViewById(R.id.admin_switch);
        adminSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!firstCheck) {
                    if (isChecked) {
                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getResources().getString(R.string.admin_description));
                        startActivityForResult(intent, ADMIN_INTENT);
                    } else {
                        mDevicePolicyManager.removeActiveAdmin(mComponentName);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.admin_removed), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        adminSwitch.setChecked(mDevicePolicyManager.isAdminActive(mComponentName));


        accessibilitySwitch = findViewById(R.id.accessibility_switch);
        accessibilitySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!firstCheck) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                    if (isChecked) {
                        if (accessibilityCheck) Toast.makeText(getApplicationContext(), "Accessibility service enabled", Toast.LENGTH_SHORT).show();
                    } else {
//                        sendBroadcast(new Intent(Accessibility.ACTION_SERVICE_OFF));
                        if (accessibilityCheck) Toast.makeText(getApplicationContext(), "Accessibility service disabled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        accessibilitySwitch.setChecked(isAccessibilityEnabled());

        firstCheck = false;
        accessibilityCheck = false;
    }

    private void start() {
        Log.i(TAG, "Delaying locking");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if(active == 1) {
                    Log.i(TAG, "Locking");
                    mDevicePolicyManager.lockNow();

                    Log.i(TAG, "Delaying unlocking");
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra("ACTIVE", active);
                            Log.i(TAG, "Unlocking");
                            startActivity(intent);
                        }
                    }, delay);

                    finish();
                }
            }
        }, delay*2);
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
            Log.d(TAG, "ACCESSIBILITY: " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled==1){
            Log.d(TAG, "***ACCESSIBILIY IS ENABLED***: ");


            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            Log.d(TAG, "Setting: " + settingValue);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();
                    Log.d(TAG, "Setting: " + accessabilityService);
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
        active = 0;
    }

    public Date getTime() {
        Date date;
        try {
            TimeTCPClient client = new TimeTCPClient();
            try {
                // Set timeout of 60 seconds
                client.setDefaultTimeout(60000);
                // Connecting to time server
                // Other time servers can be found at : http://tf.nist.gov/tf-cgi/servers.cgi#
                // Make sure that your program NEVER queries a server more frequently than once every 4 seconds
                client.connect("time.nist.gov");
//                System.out.println(client.getDate());
                date = client.getDate();
                return date;
            } finally {
                client.disconnect();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

//        try{
//            //Make the Http connection so we can retrieve the time
//            HttpClient httpclient = new DefaultHttpClient();
//            // I am using yahoos api to get the time
//            HttpResponse response = httpclient.execute(new
//                    HttpGet("http://memownia.byethost33.com/meme/72"));
//            StatusLine statusLine = response.getStatusLine();
//            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                response.getEntity().writeTo(out);
//                out.close();
//                // The response is an xml file and i have stored it in a string
//                String responseString = out.toString();
//                Log.d("Response", responseString);
//                //We have to parse the xml file using any parser, but since i have to
//                //take just one value i have deviced a shortcut to retrieve it
//                int x = responseString.indexOf("<Timestamp>");
//                int y = responseString.indexOf("</Timestamp>");
//                //I am using the x + "<Timestamp>" because x alone gives only the start value
//                Log.d("Response", responseString.substring(x + "<Timestamp>".length(),y) );
//                String timestamp =  responseString.substring(x + "<Timestamp>".length(),y);
//                // The time returned is in UNIX format so i need to multiply it by 1000 to use it
//                Date d = new Date(Long.parseLong(timestamp) * 1000);
//                Log.d("Response", d.toString() );
//                return d.toString() ;
//            } else{
//                //Closes the connection.
//                response.getEntity().getContent().close();
//                throw new IOException(statusLine.getReasonPhrase());
//            }
//        }catch (ClientProtocolException e) {
//            Log.d("Response", e.getMessage());
//        }catch (IOException e) {
//            Log.d("Response", e.getMessage());
//        }
//        return null;
    }
}
