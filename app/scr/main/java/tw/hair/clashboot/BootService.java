package tw.haier.clashboot;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;

public class BootService extends Service {
    private static final String CHANNEL = "clash_boot";
    private static final String LOG = "/data/local/tmp/clash-boot-helper.log";

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(7, new Notification.Builder(this, CHANNEL)
            .setContentTitle("Clash 开机助手")
            .setContentText("正在通过本机 ADB 启动 Clash 和 Watcher")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            try {
                Thread.sleep(30000);
                String command =
                    "echo '--- boot helper $(date) ---' >> " + LOG + "; " +
                    "if [ ! -f /data/local/tmp/clash-watch.sh ]; then " +
                    "echo 'ERROR: watcher script missing' >> " + LOG + "; " +
                    "else " +
                    "am start -a com.github.metacubex.clash.meta.action.START_CLASH " +
                    "-p com.github.metacubex.clash.meta >> " + LOG + " 2>&1; " +
                    "sleep 5; " +
                    "/vendor/xbin/busybox nohup /vendor/xbin/busybox setsid " +
                    "/system/bin/sh /data/local/tmp/clash-watch.sh " +
                    "</dev/null >> /data/local/tmp/clash-watch.out 2>&1 & " +
                    "echo 'ADB command submitted; watcher launch requested' >> " + LOG + "; " +
                    "fi";
                String output = AdbShell.exec(command);
                append("ADB shell result: " + output);
            } catch (Throwable t) {
                append("ERROR: " + android.util.Log.getStackTraceString(t));
            } finally {
                stopForeground(true);
                stopSelf();
            }
        }, "adb-boot-start").start();
        return START_NOT_STICKY;
    }

    private void append(String s) {
        try (FileOutputStream f = new FileOutputStream(new File(LOG), true)) {
            f.write((s + "\n").getBytes("UTF-8"));
        } catch (Exception ignored) {}
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Clash 开机助手", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
