package cn.ctkqiang.huaxiahongke.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Objects;

import cn.ctkqiang.huaxiahongke.R;
import cn.ctkqiang.huaxiahongke.adpters.MenuAdapter;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "华夏红客工具";
    private static final String PREFS_NAME = "UserAgreementPrefs";
    private static final String AGREEMENT_ACCEPTED = "Accepted";

    private static final int 请求蓝牙权限码 = 10086;

    private String[] 功能列表 = {"DDOS 攻击", "蓝牙", "WIFI", "检测监控摄像头", "加密消息"};

    private GridView gridView;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(this.getSupportActionBar()).hide();
        this.setContentView(R.layout.activity_main);


        if (!isAgreementAccepted())
        {
            this.showUserAgreement();
        } else
        {
            this.初始化应用();
        }
    }

    private boolean isAgreementAccepted()
    {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(AGREEMENT_ACCEPTED, false);
    }

    private void setAgreementAccepted()
    {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(AGREEMENT_ACCEPTED, true);
        editor.apply();


        Log.d(TAG, "setAgreementAccepted: ");
    }

    private void showUserAgreement()
    {
        new AlertDialog.Builder(this)
                .setTitle("用户协议")
                .setMessage("本工具由中国开发者制作，旨在服务于中国用户的安全防护与学习研究用途。\n\n本工具仅限于合法合规的安全测试与教育研究，严禁任何形式的非法攻击、入侵或损害他人利益的行为。\n\n特别强调：\n- 禁止用于针对中国同胞/企业及无辜个体的一切未经授权的渗透或攻击行为。\n- 本工具只为保护、提升我国的信息安全能力，不得以任何方式用于伤害中国人民或破坏中国互联网环境。\n\n使用者应对自己的操作和行为负责，任何违法滥用行为，法律责任由使用者个人承担。\n\n使用本工具，即视为已阅读、理解并同意以上所有条款。")
                .setCancelable(false)
                .setPositiveButton("接受", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        MainActivity.this.setAgreementAccepted();
                        MainActivity.this.初始化应用();
                    }
                })
                .setNegativeButton("拒绝", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Toast.makeText(MainActivity.this, "您必须接受协议才能使用本应用。", Toast.LENGTH_LONG).show();
                        finish(); // 结束当前 Activity，退出应用
                    }
                })
                .show();
    }

    private void 初始化应用() {
        setContentView(R.layout.activity_main);
        初始化功能菜单();
    }

    private void 初始化功能菜单() {
        gridView = findViewById(R.id.gridView);
        MenuAdapter adapter = new MenuAdapter(this, Arrays.asList(功能列表));
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            String 功能 = 功能列表[position];
            Toast.makeText(this, "你点击了：" + 功能 + " [" + position + "]", Toast.LENGTH_SHORT).show();
            处理功能点击(position);
        });
    }

    @SuppressLint("MissingPermission")
    private void 处理功能点击(int 位置) {
        switch (位置) {
            case 0:
                startActivity(new Intent(this, DDOSActivity.class));
                break;
            case 1:
                检查蓝牙权限并处理();
                break;
            default:
                Toast.makeText(this, "该功能正在施工中～", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void 检查蓝牙权限并处理() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12及以上，需要动态申请BLUETOOTH_CONNECT和SCAN
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, 请求蓝牙权限码);
                return;
            }
            // 有权限了继续
            检查蓝牙状态();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0到11（API 23-30），蓝牙权限是安装时拿到的，直接检查蓝牙状态
            检查蓝牙状态();
        } else {
            // Android 5.x 及以下，连动态权限系统都没有，直接做逻辑
            检查蓝牙状态();
        }
    }


    private void 检查蓝牙状态() {
        BluetoothAdapter 蓝牙适配器 = BluetoothAdapter.getDefaultAdapter();
        if (蓝牙适配器 == null) {
            Toast.makeText(this, "你的设备不支持蓝牙功能哦～", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!蓝牙适配器.isEnabled()) {
            尝试开启蓝牙();
        } else {
            进入蓝牙页面();
        }
    }

    private void 尝试开启蓝牙() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "没有蓝牙权限，无法打开蓝牙...", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent 开启蓝牙意图 = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(开启蓝牙意图, 请求蓝牙权限码);
    }

    private void 进入蓝牙页面() {
        startActivity(new Intent(this, BluetoothActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 请求蓝牙权限码) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "蓝牙已开启，出发喽！", Toast.LENGTH_SHORT).show();
                进入蓝牙页面();
            } else {
                Toast.makeText(this, "不开蓝牙就玩不了啦...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 请求蓝牙权限码) {
            if (grantResults.length >= 2 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "授权成功，继续！", Toast.LENGTH_SHORT).show();
                检查蓝牙状态();
            } else {
                Toast.makeText(this, "不给权限就不陪你玩蓝牙咯～", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
