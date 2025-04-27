package cn.ctkqiang.huaxiahongke;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Objects;

import cn.ctkqiang.huaxiahongke.adpters.MenuAdapter;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "华夏红客工具";
    private static final String PREFS_NAME = "UserAgreementPrefs";
    private static final String AGREEMENT_ACCEPTED = "Accepted";

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
            this.initAppNormally();
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
                        MainActivity.this.initAppNormally();
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

    private void initAppNormally()
    {
        this.setContentView(R.layout.activity_main);
        this.initGridView();
    }

    private void initGridView() {
        gridView = findViewById(R.id.gridView);

        MenuAdapter adapter = new MenuAdapter(this, Arrays.asList(MainActivity.this.功能列表));
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String 功能 = 功能列表[position];
                Toast.makeText(MainActivity.this, "你点击了：" + 功能 + position, Toast.LENGTH_SHORT).show();

                switch(position) {
                    case 0:
                        Intent intent = new Intent(MainActivity.this, DDOSActivity.class);
                        startActivity(intent);
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    default:
                        break;
                }
            }
        });
    }

}
