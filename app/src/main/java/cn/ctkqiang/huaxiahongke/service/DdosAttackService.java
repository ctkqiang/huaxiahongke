package cn.ctkqiang.huaxiahongke.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DdosAttackService extends Service {

    private static final int NOTIFICATION_ID = 1;  // 通知ID
    private NotificationManager notificationManager;  // 通知管理器

    private ExecutorService 执行器服务;  // 用于执行攻击任务的线程池
    private int 请求数 = 1000;  // 示例请求数（可以通过Intent传递）

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 初始化通知管理器和执行器服务
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        执行器服务 = Executors.newFixedThreadPool(10);  // 示例线程池大小

        // 显示初始通知，表示DDoS攻击正在进行
        showNotification(0);

        // 在一个新的线程中启动DDoS攻击
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 请求数; i++) {
                    执行器服务.execute(new Runnable() {
                        @Override
                        public void run() {
                            执行攻击();  // 执行实际的攻击
                        }
                    });

                    final int progress = i + 1;

                    // 更新通知中的进度
                    updateNotification(progress);

                    try {
                        Thread.sleep(10); // 延迟0.01秒
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // 攻击完成后，更新通知并停止服务
                stopForeground(true);
                stopSelf();
                showCompletionNotification();
            }
        }).start();

        return START_STICKY;  // 服务持续运行
    }

    // 显示初始的带进度条的通知
    private void showNotification(int progress) {
        Notification notification = new NotificationCompat.Builder(this, "DdosAttackChannel")
                .setContentTitle("DDoS攻击进行中")
                .setContentText("已发送 " + progress + " 请求...")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setProgress(请求数, progress, false)
                .setOngoing(true)  // 设置为进行中，无法滑动关闭
                .build();

        startForeground(NOTIFICATION_ID, notification);  // 作为前台服务启动并显示通知
    }

    // 更新通知中的进度
    private void updateNotification(int progress) {
        Notification notification = new NotificationCompat.Builder(this, "DdosAttackChannel")
                .setContentTitle("DDoS攻击进行中")
                .setContentText("已发送 " + progress + " 请求...")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setProgress(请求数, progress, false)
                .setOngoing(true)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);  // 更新通知
    }

    // 显示攻击完成的通知
    private void showCompletionNotification() {
        Notification notification = new NotificationCompat.Builder(this, "DdosAttackChannel")
                .setContentTitle("DDoS攻击已完成")
                .setContentText("攻击完成！")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setOngoing(false)  // 设置为不再进行中
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);  // 更新完成通知
    }

    // 实际执行攻击的方法
    private void 执行攻击() {
        // 这里是攻击的实际执行逻辑（例如，发送HTTP请求）
        // 当前仅为模拟攻击，您可以根据需要进行实现
        try {
            // 模拟网络操作
            Thread.sleep(100);  // 暂停100毫秒，模拟攻击延迟
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // 不支持绑定
    }
}
