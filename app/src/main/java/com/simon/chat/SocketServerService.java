package com.simon.chat;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;

import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class SocketServerService extends Service {
    private final int BUFFER_SIZE = 512 * 1024;
    private final int TIMEOUT = 500;
    private String data;
    private DataCallback dataCallback = null;
    private SocketChannel socketChannel;
    private SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(() -> {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            int port = Integer.parseInt(preferences.getString("port", "1212"));
            try (ServerSocketChannel server = ServerSocketChannel.open()) {
                server.socket().bind(new InetSocketAddress(port));
                while (true) {
                    socketChannel = server.accept();
                    socketChannel.socket().setSoTimeout(TIMEOUT);
                    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                    socketChannel.read(buffer);
                    buffer.flip();
                    data = StandardCharsets.UTF_8.decode(buffer).toString();
                    if (dataCallback != null) {
                        dataCallback.dataChanged(data);
                    }
                    buffer.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            String connect_host = intent.getStringExtra("connect_host");
            String msg = intent.getStringExtra("msg");
            if (msg != null && !msg.equals("")) {
                int port = Integer.parseInt(preferences.getString("port", "1212"));
                try (SocketChannel socketChannel = SocketChannel.open()) {
                    socketChannel.socket().connect(new InetSocketAddress(connect_host, port), TIMEOUT);
                    socketChannel.socket().setSoTimeout(TIMEOUT);
                    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                    buffer.put(msg.getBytes());
                    buffer.flip();
                    socketChannel.write(buffer);
                    buffer.clear();
                } catch (IOException e) {
                    msg = getString(R.string.connect_host) + "逾時無回應";
                } finally {
                    if (dataCallback != null) {
                        dataCallback.dataChanged(msg);
                    }
                }
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public DataCallback getDataCallback() {
        return dataCallback;
    }

    public void setDataCallback(DataCallback dataCallback) {
        this.dataCallback = dataCallback;
    }

    public class ServiceBinder extends Binder {
        public SocketServerService getService() {
            return SocketServerService.this;
        }

        public void setData(String data) {
            SocketServerService.this.data = data;
        }
    }

    public interface DataCallback {
        void dataChanged(String str);
    }

}