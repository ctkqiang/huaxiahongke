package cn.ctkqiang.huaxiahongke.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import cn.ctkqiang.huaxiahongke.R;
import cn.ctkqiang.huaxiahongke.adpters.DeviceListAdapter;
import cn.ctkqiang.huaxiahongke.constants.Constants;

@SuppressWarnings("NonAsciiCharacters")
public class BluetoothActivity extends AppCompatActivity
{
    private static final String TAG = Constants.TAG_NAME;

    private BluetoothAdapter 蓝牙适配器;
    private Set<BluetoothDevice> 已扫描设备集合;
    private ListView 设备列表视图;
    private DeviceListAdapter 设备列表适配器;
    private ArrayList<BluetoothDevice> 设备列表;

    private SwipeRefreshLayout swipeRefreshLayout;

    private static final UUID 通用UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int 蓝牙权限请求码 = 1001;

    // 蓝牙广播接收器
    private final BroadcastReceiver 蓝牙广播接收器 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context 上下文, Intent 意图)
        {
            String 动作 = 意图.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(动作))
            {

                // 获取扫描到的设备
                BluetoothDevice 设备 = 意图.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (设备 != null && !已扫描设备集合.contains(设备))
                {
                    已扫描设备集合.add(设备);
                    设备列表.add(设备);
                    设备列表适配器.notifyDataSetChanged();
                }
            }
        }
    };

    // 启动蓝牙请求的结果处理器
    private final ActivityResultLauncher<Intent> 启动蓝牙启动器 = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>()
            {
                @Override
                public void onActivityResult(ActivityResult 结果)
                {
                    if (蓝牙适配器.isEnabled())
                    {
                        BluetoothActivity.this.开始扫描蓝牙设备真执行();
                    } else
                    {
                        Toast.makeText(BluetoothActivity.this, "蓝牙未开启，无法扫描设备！", Toast.LENGTH_SHORT).show();
                        BluetoothActivity.this.finish();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle 保存状态包)
    {
        super.onCreate(保存状态包);
        setContentView(R.layout.activity_bluetooth);

        Objects.requireNonNull(this.getSupportActionBar()).hide(); // 隐藏ActionBar

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);  // 初始化 SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                BluetoothActivity.this.重新扫描设备();
            }
        });

        初始化蓝牙();
    }

    private void 重新扫描设备()
    {
        // 清空已扫描设备列表
        已扫描设备集合.clear();
        设备列表.clear();
        设备列表适配器.notifyDataSetChanged();

        // 重新开始扫描设备
        开始扫描蓝牙设备真执行();

        // 执行完刷新后，停止刷新动画
        swipeRefreshLayout.setRefreshing(false);
    }

    private void 初始化蓝牙()
    {
        设备列表视图 = findViewById(R.id.deviceListView);

        // 获取默认蓝牙适配器
        蓝牙适配器 = BluetoothAdapter.getDefaultAdapter();
        if (蓝牙适配器 == null)
        {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化扫描设备集合和列表
        已扫描设备集合 = new HashSet<>();
        设备列表 = new ArrayList<>();
        设备列表适配器 = new DeviceListAdapter(this, 设备列表);
        设备列表视图.setAdapter(设备列表适配器);

        Log.d(TAG, "初始化蓝牙: " + 设备列表);

        // 检查并请求蓝牙权限
        检查并申请蓝牙权限();
    }

    private void 检查并申请蓝牙权限()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {

            // 请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    蓝牙权限请求码
            );
        } else
        {
            开始扫描蓝牙设备();
        }
    }

    @Override
    public void onRequestPermissionsResult(int 请求码, String[] 权限数组, int[] 授权结果数组)
    {
        super.onRequestPermissionsResult(请求码, 权限数组, 授权结果数组);

        if (请求码 == 蓝牙权限请求码)
        {
            boolean 全部授权 = true;
            for (int 授权结果 : 授权结果数组)
            {
                if (授权结果 != PackageManager.PERMISSION_GRANTED)
                {
                    全部授权 = false;
                    break;
                }
            }

            if (全部授权)
            {
                开始扫描蓝牙设备();
            } else
            {
                Toast.makeText(this, "必须授权才能使用蓝牙功能喔～", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void 开始扫描蓝牙设备()
    {
        if (!蓝牙适配器.isEnabled())
        {
            // 请求启用蓝牙
            Intent 开启蓝牙意图 = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            启动蓝牙启动器.launch(开启蓝牙意图);
        } else
        {
            开始扫描蓝牙设备真执行();
        }
    }

    private void 开始扫描蓝牙设备真执行()
    {
        // 设置广播过滤器，监听扫描结果
        IntentFilter 过滤器 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(蓝牙广播接收器, 过滤器);

        // 启动蓝牙设备扫描
        @SuppressLint("MissingPermission") boolean 开始扫描成功 = 蓝牙适配器.startDiscovery();
        if (!开始扫描成功)
        {
            Toast.makeText(this, "蓝牙扫描启动失败😭", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        try
        {
            unregisterReceiver(蓝牙广播接收器); // 注销广播接收器
        } catch (IllegalArgumentException e)
        {
            // 异常捕获，防止出现非法操作
            e.printStackTrace();
        }
    }
}
