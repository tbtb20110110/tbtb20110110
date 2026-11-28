cat > app/src/main/java/tw/haier/clashboot/BootService.java <<'EOF'
package tw.haier.clashboot;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;

public class BootService extends Service {
    static final String SHELL_LOG =
        "/data/local/tmp/clash-boot-helper.log";

    void log(String s) {
        String line = System.currentTimeMillis() + " " + s + "\n";
        try (FileOutputStream f =
                 openFileOutput("helper.log", MODE_APPEND)) {
            f.write(line.getBytes("UTF-8"));
        } catch (Exception ignored) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                "boot",
                "Clash开机助手",
                NotificationManager.IMPORTANCE_LOW
            );

            ((NotificationManager)getSystemService(
                NOTIFICATION_SERVICE
            )).createNotificationChannel(ch);
        }

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, "boot")
            : new Notification.Builder(this);

        startForeground(
            1,
            b.setContentTitle("Clash开机助手")
                .setContentText("正在检查本机 ADB 并启动 Clash")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build()
        );

        log("BootService created");
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        String action = i == null ? "" : i.getAction();
        log("Service start action=" + action);

        new Thread(() -> {
            try {
                Thread.sleep(30000);

                String cmd =
                    "echo 'Helper shell started: $(date)' >> " +
                    SHELL_LOG + "; " +

                    "echo 'ADB shell command reached' >> " +
                    SHELL_LOG + "; " +

                    "am start -a " +
                    "com.github.metacubex.clash.meta.action.START_CLASH " +
                    "-p com.github.metacubex.clash.meta >> " +
                    SHELL_LOG + " 2>&1; " +

                    "sleep 5; " +

                    "if ps -A | grep -q '[c]lash-watch.sh'; then " +
                    "echo 'Watcher process already present' >> " +
                    SHELL_LOG + "; " +

                    "else " +

                    "/vendor/xbin/busybox nohup " +
                    "/vendor/xbin/busybox setsid " +
                    "/system/bin/sh " +
                    "/data/local/tmp/clash-watch.sh " +
                    "</dev/null >> " +
                    "/data/local/tmp/clash-watch.out 2>&1 & " +

                    "echo 'Watcher launch requested' >> " +
                    SHELL_LOG + "; " +
                    "fi; " +

                    "echo 'Helper finished: $(date)' >> " +
                    SHELL_LOG + "; " +

                    "tail -n 30 " + SHELL_LOG;

                String result = AdbShell.exec(cmd);
                log("ADB shell succeeded:\n" + result);

            } catch (Throwable e) {
                log("ADB ERROR: " + e.toString());
            }

            stopForeground(true);
            stopSelf();
        }).start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
EOF
