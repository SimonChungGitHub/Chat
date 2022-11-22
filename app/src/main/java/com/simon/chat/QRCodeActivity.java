package com.simon.chat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class QRCodeActivity extends MainActivity {
    protected SharedPreferences preferences;
    protected String local_host;
    protected int port;
    private CaptureManager captureManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }

        local_host = preferences.getString("local_host", "");
        port = Integer.parseInt(preferences.getString("port", "1212"));
        genCode();

        DecoratedBarcodeView barcodeView = findViewById(R.id.barcode_scanner);
        captureManager = new CaptureManager(this, barcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);
        barcodeView.setFocusedByDefault(true);
        barcodeView.decodeSingle(result -> {
            sounds.play(beep, 5.0F, 5.0F, 1, 0, 1.0F);
            String[] arr = result.getText().split(":");
            preferences.edit()
                    .putString("connect_host", arr[0])
                    .putString("port", arr[1])
                    .apply();
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    new AlertDialog.Builder(this)
                            .setTitle("提示")
                            .setMessage("開啟相機權限進行掃描")
                            .setNegativeButton(android.R.string.ok, (dialog, which) -> {
                                dialog.cancel();
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
                            })
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("提示")
                            .setMessage("開啟相機權限進行掃描")
                            .setNegativeButton(android.R.string.ok, (dialog, which) -> dialog.cancel())
                            .show();
                }
            }
        }
    }

    public void genCode() {
        String content = local_host + ":" + port;
        ImageView ivCode = findViewById(R.id.qrcode);
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.putIfAbsent(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8);
        BarcodeEncoder encoder = new BarcodeEncoder();
        try {
            Bitmap bit = encoder.encodeBitmap(content, BarcodeFormat.QR_CODE,
                    500, 500, hints);
            ivCode.setImageBitmap(bit);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

}