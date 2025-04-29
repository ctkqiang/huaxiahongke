package cn.ctkqiang.huaxiahongke.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.ctkqiang.huaxiahongke.R;
import cn.ctkqiang.huaxiahongke.constants.Constants;
import cn.ctkqiang.huaxiahongke.utilities.AudioPlayer;
import cn.ctkqiang.huaxiahongke.utilities.AudioRecorder;
import cn.ctkqiang.huaxiahongke.utilities.MulticastManager;
import cn.ctkqiang.huaxiahongke.utilities.NetworkUtils;

public class CommActivity extends AppCompatActivity implements MulticastManager.PacketReceiver
{
    private static final String TAG = Constants.TAG_NAME;

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private Button btnPTT;
    private AudioRecorder audioRecorder;
    private AudioPlayer audioPlayer;
    private boolean isTransmitting = false;
    private MulticastManager multicastManager;
    private WifiManager.MulticastLock multicastLock;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
    };

    private void checkPermissions()
    {
        List<String> missingPermissions = new ArrayList<>();
        for (String perm : REQUIRED_PERMISSIONS)
        {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
            {
                missingPermissions.add(perm);
            }
        }

        if (!missingPermissions.isEmpty())
        {
            ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        } else
        {
            // 已有全部权限时直接初始化
            initializeNetworkComponents();
        }
    }

    private BroadcastReceiver networkReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (!NetworkUtils.isNetworkConnected(CommActivity.this))
            {
                Toast.makeText(CommActivity.this, "网络连接已断开", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(networkReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE)
        {
            for (int result : grantResults)
            {
                if (result != PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this, "需要所有权限才能正常工作", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }

            this.initializeNetworkComponents();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comm);

        Objects.requireNonNull(this.getSupportActionBar()).hide(); // 隐藏ActionBar

        this.checkPermissions();
        this.setupMulticastLock();
        this.initializeNetworkComponents();

        this.btnPTT = (Button) findViewById(R.id.btnPTT);
        this.btnPTT.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (multicastManager == null || !multicastManager.isInitialized())
                {
                    Toast.makeText(CommActivity.this,
                            R.string.not_initialized,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        startTransmission();
                        return true;
                    case MotionEvent.ACTION_UP:
                        stopTransmission();
                        return true;
                }
                return true;
            }
        });
    }


    private void startTransmission()
    {
        if (isTransmitting) return;

        try
        {
            audioRecorder.startRecording();
            isTransmitting = true;
            runOnUiThread(() -> showTalkingUI(true));
            Log.d(TAG, "Started transmission");
        } catch (IllegalStateException e)
        {
            Log.e(TAG, "Failed to start recording: " + e.getMessage());
            Toast.makeText(this, "Microphone unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopTransmission()
    {
        if (!isTransmitting) return;

        audioRecorder.stopRecording();
        isTransmitting = false;
        runOnUiThread(() -> showTalkingUI(false));
        Log.d(TAG, "Stopped transmission");
    }

    @SuppressLint("MissingPermission")
    private void setupMulticastLock()
    {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null)
        {
            Log.e(TAG, "无法获取 WifiManager 服务");
            return;
        }

        // 仅在 Android 8.0+ 设备检查多播状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            try
            {
                // 使用反射检查方法是否存在
                Method method = wifiManager.getClass().getMethod("isMulticastEnabled");
                boolean isMulticastEnabled = (boolean) method.invoke(wifiManager);

                if (!isMulticastEnabled)
                {
                    Log.w(TAG, "设备多播功能未启用");
                    showMulticastSettingsDialog();
                    return;
                }
            } catch (Exception e)
            {
                Log.e(TAG, "多播状态检查失败: " + e.getMessage());
            }
        }

        // 创建 MulticastLock
        multicastLock = wifiManager.createMulticastLock("MyAppLock");
        multicastLock.setReferenceCounted(true);

        try
        {
            if (!multicastLock.isHeld())
            {
                multicastLock.acquire();
                Log.i(TAG, "MulticastLock 获取成功");
            }
        } catch (Exception e)
        {
            Log.e(TAG, "获取锁失败: " + e.getMessage());
        }
    }

    private void showMulticastSettingsDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("多播功能未启用");
        builder.setMessage("请在系统设置中开启WiFi多播支持");
        builder.setPositiveButton("前往设置", (dialog, which) ->
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                startActivity(new Intent(Settings.ACTION_WIFI_IP_SETTINGS));
            } else
            {
                Toast.makeText(this, "请在系统WiFi高级设置中启用多播", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("取消", (d, w) -> finish());
        builder.show();
    }


    private void initializeNetworkComponents()
    {
        try
        {
            if (!NetworkUtils.isNetworkConnected(this))
            {
                throw new IOException("No network connection");
            }

            String ssid = NetworkUtils.getCurrentSSID(this);
            String groupIP = NetworkUtils.generateMulticastIP(ssid);

            Log.d(TAG, "Initializing multicast group: " + groupIP);

            this.multicastManager = new MulticastManager(groupIP);
            if (!multicastManager.isInitialized())
            {
                throw new IOException("Multicast initialization failed");
            }

            this.audioPlayer = new AudioPlayer(this);
            this.audioRecorder = new AudioRecorder(this, multicastManager);

            this.multicastManager.startReceiving(new MulticastManager.PacketReceiver()
            {
                @Override
                public void onPacketReceived(byte[] data, int length)
                {
                    if (!audioRecorder.isRecording())
                    {
                        audioPlayer.playAudio(data, length);
                    }
                }
            });

        } catch (Exception e)
        { // Catch all exceptions
            Log.e(TAG, "Initialization error: " + e.getMessage());
            runOnUiThread(() ->
            {
                Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    @Override
    public void onPacketReceived(byte[] data, int length)
    {
        if (audioPlayer != null && !isTransmitting)
        {
            audioPlayer.playAudio(data, length);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (multicastLock != null && multicastLock.isHeld())
        {
            multicastLock.release();
        }
        if (multicastManager != null)
        {
            multicastManager.close();
        }
        if (audioRecorder != null)
        {
            audioRecorder.cleanup();
        }
        if (audioPlayer != null)
        {
            audioPlayer.cleanup();
        }
    }


    private void showTalkingUI(boolean isTalking)
    {
        this.btnPTT.setBackgroundColor(isTalking ? Color.RED : Color.GRAY);
        this.btnPTT.setText(isTalking ? getString(R.string.talking) : getString(R.string.press_to_talk));
        Toast.makeText(this, getString(R.string.init_failed), Toast.LENGTH_LONG).show();
    }

}