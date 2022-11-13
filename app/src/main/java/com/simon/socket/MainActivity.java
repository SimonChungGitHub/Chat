package com.simon.socket;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    protected final Handler handler = new Handler();
    protected SharedPreferences preferences;
    protected String local_host;
    protected String connect_host;
    protected int port;
    private EditText input;
    private ScrollView scrollView;
    private LinearLayout msgBox;
    private TextView listeningMsg;
    private Thread listening;
    private Snackbar snackbar;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (snackbar != null) {
            snackbar.dismiss();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listeningMsg = findViewById(R.id.listening);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        scrollView = findViewById(R.id.scroll_view);
        msgBox = findViewById(R.id.msg_box);
        input = findViewById(R.id.input);
        Button send = findViewById(R.id.button);
        send.setOnClickListener((view) -> {
            Thread thread = new Thread(() -> {
                if (input.getText().toString().length() == 0) {
                    return;
                }
                TextView tvSend = createTextView();
                handler.post(() -> {
                    msgBox.addView(tvSend);
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                });
                if (client(input.getText().toString())) {
                    tvSend.setText(input.getText().toString());
                } else {
                    tvSend.setTextColor(Color.RED);
                    tvSend.setText("無法與對方連線");
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            input.setText("");
        });
        local_host = getLocalIpAddress();
        preferences.edit().putString("local_host", local_host).apply();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        port = Integer.parseInt(preferences.getString("port", "1212"));
        local_host = preferences.getString("local_host", "");
        connect_host = preferences.getString("connect_host", "");
        String text = "我的IP：" + local_host + "\n對方IP：" + connect_host;
        listeningMsg.setText(text);
        listening = new Thread(() -> {
            try (ServerSocketChannel server = ServerSocketChannel.open()) {
                server.socket().bind(new InetSocketAddress(port));
                while (true) {
                    SocketChannel socketChannel = server.accept();
                    server(socketChannel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listening.start();
    }

    @Override
    protected void onStop() {
//        listening.interrupt();
        super.onStop();
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

    private void server(SocketChannel socketChannel) {
        try {
            socketChannel.socket().setSoTimeout(500);
            ByteBuffer buffer = ByteBuffer.allocate(256);
            socketChannel.read(buffer);
            buffer.flip();
            String str = StandardCharsets.UTF_8.decode(buffer).toString();
            TextView tv = createTextView();
            tv.setTextColor(Color.GREEN);
            tv.setText(str);
            handler.post(() -> {
                msgBox.addView(tv);
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            });
            buffer.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean client(String sendMsg) {
        if (connect_host.equals("")) {
            snackbar = Snackbar.make(msgBox, "對方IP未設定", Snackbar.LENGTH_LONG);
            snackbar.show();
            return false;
        }
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.socket().connect(new InetSocketAddress(connect_host, port), 500);
            socketChannel.socket().setSoTimeout(500);
            ByteBuffer buffer = ByteBuffer.allocate(256);
            buffer.put((sendMsg).getBytes());
            buffer.flip();
            socketChannel.write(buffer);
            buffer.clear();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private TextView createTextView() {
        TextView textView = new TextView(this);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
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

}