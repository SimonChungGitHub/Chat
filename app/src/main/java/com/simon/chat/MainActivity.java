package com.simon.chat;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    private final Handler handler = new Handler();
    private SharedPreferences preferences;
    private String username;
    private String local_host;
    private String connect_host;
    private int textSize = 16;

    private EditText input;
    private ScrollView scrollView;
    private LinearLayout msgBox;
    private TextView listeningMsg;
    private Snackbar snackbar;

    private Intent intent;
    private ServiceConnection myServiceConn;
    private SocketServerService.ServiceBinder binder = null;
    protected SoundPool sounds;
    protected boolean soundEffect = true;
    protected int sound_send;
    protected int sound_receive;
    protected int beep;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (snackbar != null) {
            snackbar.dismiss();
        }
//        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (manager != null) {
//            manager.hideSoftInputFromWindow(input.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
//        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putString("local_host", getLocalIpAddress()).apply();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        sounds = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
        sound_send = sounds.load(this, R.raw.sound_send, 1);
        sound_receive = sounds.load(this, R.raw.sound_receive, 1);
        beep = sounds.load(this, R.raw.beep, 1);

        listeningMsg = findViewById(R.id.listening);
        scrollView = findViewById(R.id.scroll_view);
        msgBox = findViewById(R.id.msg_box);
        input = findViewById(R.id.input);
        Button send = findViewById(R.id.button);
        send.setOnClickListener((view) -> {
            String msg = input.getText().toString();
            if (msg.equals("") || msg.length() == 0) {
                return;
            }
            if (connect_host.equals("")) {
                snackbar = Snackbar.make(msgBox, getString(R.string.connect_host) + "未設定", Snackbar.LENGTH_LONG);
                snackbar.show();
                return;
            }
            @SuppressLint("SimpleDateFormat") SimpleDateFormat f = new SimpleDateFormat("HH:mm");
            msg = username + " " + f.format(new Date()) + "  " + msg;
            intent = new Intent(this, SocketServerService.class);
            intent.putExtra("connect_host", connect_host);
            intent.putExtra("msg", msg);
            startService(intent);
            input.setText("");
            if (sounds != null && soundEffect) {
                sounds.play(sound_send, 5.0F, 5.0F, 1, 0, 1.0F);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        username = preferences.getString("user_name", "無");
        local_host = preferences.getString("local_host", "");
        connect_host = preferences.getString("connect_host", "");
        soundEffect = preferences.getBoolean("sound_send", true);
        textSize = Integer.parseInt(preferences.getString("textSize", "16"));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        String text = getString(R.string.local_host) + local_host + "\n" + getString(R.string.connect_host) + connect_host;
        listeningMsg.setText(text);
        listeningMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);


        intent = new Intent(this, SocketServerService.class);
        startService(intent);
        myServiceConn = new MyServiceConn();
        bindService(intent, myServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        stopService(intent);
        if (binder != null) {
            unbindService(myServiceConn);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_setting) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_qrcode) {
            startActivity(new Intent(this, QRCodeActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private TextView createTextView() {
        TextView textView = new TextView(this);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setPadding(10, 10, 10, 10);
        return textView;
    }

    protected String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    class MyServiceConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (SocketServerService.ServiceBinder) service;
            binder.getService().setDataCallback(str -> {
                if (str != null && !str.equals("")) {
                    TextView tv = createTextView();
                    tv.setText(str);
                    handler.post(() -> {
                        msgBox.addView(tv);
                        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                        if (soundEffect) {
                            sounds.play(sound_receive, 5.0F, 5.0F, 1, 0, 1.0F);
                        }
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }
    }


}