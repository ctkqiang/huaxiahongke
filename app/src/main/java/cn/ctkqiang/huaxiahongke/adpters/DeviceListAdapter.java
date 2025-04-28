package cn.ctkqiang.huaxiahongke.adpters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import cn.ctkqiang.huaxiahongke.R;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice>
{
    private static final UUID 通用UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final String TAG = "BluetoothActivity";

    private final Context context;
    private final List<BluetoothDevice> devices;

    public DeviceListAdapter(Context context, List<BluetoothDevice> devices)
    {
        super(context, 0, devices);
        this.context = context;
        this.devices = devices;
    }

    @SuppressLint({"MissingPermission", "SetTextI18n", "NewApi"})  // 忽略未声明权限的警告
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            // 创建视图
            convertView = LayoutInflater.from(context).inflate(R.layout.device_item, parent, false);
        }

        BluetoothDevice device = getItem(position);

        BluetoothClass bluetoothClass = device.getBluetoothClass();

        TextView deviceName = convertView.findViewById(R.id.deviceName);
        TextView deviceaddr = convertView.findViewById(R.id.address);
        TextView bondstate = convertView.findViewById(R.id.bondState);
        TextView devicetype = convertView.findViewById(R.id.deviceType);
        TextView systemtype = convertView.findViewById(R.id.systemType);

        Button connectButton = convertView.findViewById(R.id.connectButton);

        if (device != null)
        {
            // 如果设备名称为空，显示“未知设备”
            deviceName.setText(device.getName() != null ? "名称: " + device.getName() : "未知设备");
            deviceaddr.setText(device.getAddress() != null ? "地址: " + device.getAddress() : "未知");
            devicetype.setText(device.getType() != BluetoothDevice.DEVICE_TYPE_UNKNOWN ? "蓝牙类型: " + this.Get设备类型(device.getType()) : "未知");

            bondstate.setText("配对状态: " + this.Get配对状态(device.getBondState()));
            systemtype.setText("设备类型: " + this.Get设备(bluetoothClass));

            Log.i(TAG, "蓝牙: " + device);

            // 设置连接按钮的点击事件
            connectButton.setOnClickListener(v ->
            {
                // 在进行连接前检查蓝牙权限
                if (checkBluetoothPermissions())
                {
                    Toast.makeText(context, "正在连接到 " + device.getName(), Toast.LENGTH_SHORT).show();

                    if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE)
                    {
                        // 如果是BLE设备，使用BLE连接的方法
                        this.connectToBLEDevice(device);
                    } else
                    {
                        // 如果是传统蓝牙设备，使用经典蓝牙连接的方法
                        this.connectToClassicBluetoothDevice(device);
                    }

                } else
                {
                    Toast.makeText(context, "需要蓝牙权限", Toast.LENGTH_SHORT).show();
                }
            });


            convertView.findViewById(R.id.bluetooth_item).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Context context = view.getContext();
                    String serviceInfo;

                    if (device.getUuids() != null && device.getUuids().length > 0)
                    {
                        StringBuilder services = new StringBuilder();

                        for (ParcelUuid uuid : device.getUuids())
                        {
                            String uuidStr = uuid.toString();
                            String name = getServiceName(uuidStr);
                            services.append(name).append("\n[").append(uuidStr.toUpperCase()).append("]\n\n");
                        }

                        serviceInfo = services.toString().trim();
                    } else
                    {
                        serviceInfo = "没有可用的服务 UUID";
                    }

                    new AlertDialog.Builder(context)
                            .setTitle("服务列表")
                            .setMessage(serviceInfo)
                            .setCancelable(true)
                            .setPositiveButton("我知道了", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            });
        }

        return convertView;
    }

    // 检查蓝牙权限的函数
    private boolean checkBluetoothPermissions()
    {
        // 在这里实现实际的权限检查（例如，检查 BLUETOOTH_CONNECT 和 BLUETOOTH_SCAN 权限）
        return true;
    }

    // 连接经典蓝牙设备的修复版本
    @SuppressLint("MissingPermission")
    private void connectToClassicBluetoothDevice(BluetoothDevice device)
    {
        new Thread(() ->
        {
            try
            {
                // 停止设备扫描（关键修复）
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

                BluetoothSocket socket = null;
                try
                {
                    // 优先尝试标准连接方式
                    socket = device.createRfcommSocketToServiceRecord(通用UUID);
                    socket.connect();
                } catch (IOException e)
                {
                    // 备用连接方法（针对中国厂商设备）
                    Log.e(TAG, "标准连接失败，尝试备用方法", e);
                    Method method = device.getClass().getMethod("createRfcommSocket", int.class);
                    socket = (BluetoothSocket) method.invoke(device, 1);

                    assert socket != null;

                    socket.connect();
                }

                final BluetoothSocket finalSocket = socket;
                ((Activity) context).runOnUiThread(() ->
                {
                    try
                    {
                        Log.i(TAG, "经典蓝牙连接成功: " + device.getName());
                        Toast.makeText(context, "连接成功", Toast.LENGTH_SHORT).show();

                        // 这里可以添加数据通信逻辑
                        finalSocket.close();
                    } catch (IOException e)
                    {
                        Log.e(TAG, "关闭套接字失败", e);
                    }
                });

            } catch (Exception e)
            {
                ((Activity) context).runOnUiThread(() ->
                {
                    Log.e(TAG, "连接失败: " + device.getName(), e);

                    Toast.makeText(context, "连接失败: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // 连接BLE设备的方法
    @SuppressLint("MissingPermission")
    private void connectToBLEDevice(BluetoothDevice device)
    {
        final BluetoothGatt[] gattHolder = new BluetoothGatt[1];
        final Handler handler = new Handler();
        final Runnable timeoutRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                if (gattHolder[0] != null)
                {
                    Log.w(TAG, "BLE连接超时");
                    gattHolder[0].disconnect();
                    gattHolder[0].close();
                    Toast.makeText(context, "连接超时", Toast.LENGTH_SHORT).show();
                }
            }
        };

        BluetoothGattCallback gattCallback = new BluetoothGattCallback()
        {
            @SuppressLint("MissingPermission")
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED)
                {
                    Log.i(TAG, "成功连接到BLE设备: " + gatt.getDevice().getName());

                    // 连接成功后，可以进行服务发现等操作
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    Log.i(TAG, "断开连接: " + gatt.getDevice().getName());
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    Log.i(TAG, "服务发现成功");
                } else
                {
                    Log.e(TAG, "服务发现失败");
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                super.onCharacteristicRead(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS)
                {
                    Log.i(TAG, "成功读取特征值");
                }
            }
        };

        gattHolder[0] = device.connectGatt(context, false, gattCallback);
        handler.postDelayed(timeoutRunnable, 15000); // 15秒超时
    }


    private String Get设备类型(int 实际数值)
    {
        switch (实际数值)
        {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "经典";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "BLE";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "双模";
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
            default:
                return "未知类型";
        }
    }

    private String Get配对状态(int 实际数值)
    {
        switch (实际数值)
        {
            case BluetoothDevice.BOND_BONDING:
                return "正在配对";
            case BluetoothDevice.BOND_BONDED:
                return "已配对";
            case BluetoothDevice.BOND_NONE:
            default:
                return "没配对";

        }
    }

    private String getServiceName(String uuid)
    {
        if (uuid == null) return "未知服务";

        uuid = uuid.toLowerCase();

        switch (uuid)
        {
            case "00001800-0000-1000-8000-00805f9b34fb":
                return "通用访问服务";
            case "00001801-0000-1000-8000-00805f9b34fb":
                return "通用属性服务";
            case "0000180a-0000-1000-8000-00805f9b34fb":
                return "设备信息服务";
            case "0000180f-0000-1000-8000-00805f9b34fb":
                return "电池服务";
            case "0000180d-0000-1000-8000-00805f9b34fb":
                return "心率服务";
            case "0000180c-0000-1000-8000-00805f9b34fb":
                return "人机接口设备服务 (HID)";
            case "0000180e-0000-1000-8000-00805f9b34fb":
                return "电话警报状态服务";
            case "00001802-0000-1000-8000-00805f9b34fb":
                return "立即警报服务";
            case "00001803-0000-1000-8000-00805f9b34fb":
                return "链路丢失服务";
            case "00001804-0000-1000-8000-00805f9b34fb":
                return "发射功率服务";
            case "00001805-0000-1000-8000-00805f9b34fb":
                return "当前时间服务";
            case "00001806-0000-1000-8000-00805f9b34fb":
                return "参考时间更新服务";
            case "00001807-0000-1000-8000-00805f9b34fb":
                return "下一DST变化服务";
            case "00001808-0000-1000-8000-00805f9b34fb":
                return "血压服务";
            case "00001809-0000-1000-8000-00805f9b34fb":
                return "健康温度服务";
            case "00001810-0000-1000-8000-00805f9b34fb":
                return "血氧服务";
            case "00001812-0000-1000-8000-00805f9b34fb":
                return "人体成分分析服务";
            case "00001813-0000-1000-8000-00805f9b34fb":
                return "跑步机数据服务";
            case "00001814-0000-1000-8000-00805f9b34fb":
                return "定位导航服务";
            case "00001816-0000-1000-8000-00805f9b34fb":
                return "循环器械数据服务";
            case "00001818-0000-1000-8000-00805f9b34fb":
                return "血糖服务";
            case "00001819-0000-1000-8000-00805f9b34fb":
                return "身体组合服务";
            case "0000181c-0000-1000-8000-00805f9b34fb":
                return "用户数据服务";
            case "0000181e-0000-1000-8000-00805f9b34fb":
                return "重量秤服务";
            case "0000181f-0000-1000-8000-00805f9b34fb":
                return "运动力量服务";
            case "00001820-0000-1000-8000-00805f9b34fb":
                return "互联网协议支持服务";
            case "00001821-0000-1000-8000-00805f9b34fb":
                return "室内定位服务";
            case "00001823-0000-1000-8000-00805f9b34fb":
                return "跑步机、椭圆机服务";
            case "00001824-0000-1000-8000-00805f9b34fb":
                return "电话访问服务";
            case "00001825-0000-1000-8000-00805f9b34fb":
                return "媒体控制服务";
            case "00001826-0000-1000-8000-00805f9b34fb":
                return "位置和导航服务";
            default:
                return "未知服务";
        }
    }

    private String Get设备(BluetoothClass bluetoothClass)
    {
        switch (bluetoothClass.getMajorDeviceClass())
        {
            case BluetoothClass.Device.Major.PHONE:
                return "手机";
            case BluetoothClass.Device.Major.COMPUTER:
                return "笔记本";
            case BluetoothClass.Device.Major.MISC:
                return "IOT";
            default:
                return "未知";
        }
    }

}
