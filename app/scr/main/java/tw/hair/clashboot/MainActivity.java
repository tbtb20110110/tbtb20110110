cat > app/src/main/java/tw/haier/clashboot/MainActivity.java <<'EOF'
package tw.haier.clashboot;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;

public class MainActivity extends Activity {
    TextView status;
    TextView logs;
    Button refresh;
    Button start;

    void localLog(String s) {
        try (FileOutputStream f =
                 openFileOutput("helper.log", MODE_APPEND)) {
            f.write((s + "\n").getBytes("UTF-8"));
        } catch (Exception ignored) {}
    }

    String readLocalLog() {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            FileInputStream f = openFileInput("helper.log");
            byte[] buf = new byte[2048];
            int n;
            while ((n = f.read(buf)) > 0) {
                b.write(buf, 0, n);
            }
            f.close();
            return b.toString("UTF-8");
        } catch (Exception e) {
            return "(暂无应用日志)\n";
        }
    }

    void refreshLocalLog() {
        logs.setText(readLocalLog());
    }

    void checkAdb() {
        status.setText("正在检查 ADB 和 Clash……");
        refresh.setEnabled(false);

        new Thread(() -> {
            try {
                String cmd =
                    "echo ADB_SHELL_OK; " +
                    "date; " +
                    "if dumpsys activity services " +
                    "com.github.metacubex.clash.meta 2>/dev/null " +
                    "| grep -q TunService; then " +
                    "echo CLASH_TUN_OK; " +
                    "else echo CLASH_TUN_MISSING; fi; " +
                    "if ps -A | grep -q '[c]lash-watch.sh'; then " +
                    "echo WATCHER_PROCESS_OK; " +
                    "else echo WATCHER_PROCESS_MISSING; fi; " +
                    "echo ====WATCHER_LOG====; " +
                    "tail -n 8 /data/local/tmp/clash-watch.log; " +
                    "echo ====HELPER_LOG====; " +
                    "tail -n 20 /data/local/tmp/clash-boot-helper.log";

                String result = AdbShell.exec(cmd);
                localLog("ADB check succeeded:\n" + result);

                runOnUiThread(() -> {
                    String adb = result.contains("ADB_SHELL_OK")
                        ? "ADB：shell 执行成功\n"
                        : "ADB：未收到 shell 确认\n";

                    String clash = result.contains("CLASH_TUN_OK")
                        ? "Clash：TunService 存在\n"
                        : "Clash：未检测到 TunService\n";

                    String watcher =
                        result.contains("WATCHER_PROCESS_OK")
                        ? "Watcher：进程存在\n"
                        : "Watcher：未检测到进程\n";

                    status.setText(adb + clash + watcher);
                    logs.setText(
                        readLocalLog() +
                        "\n\n==== 本次电视端检查 ====\n" +
                        result
                    );
                    refresh.setEnabled(true);
                });
            } catch (Exception e) {
                String err = "ADB 检查失败：" + e.toString();
                localLog(err);

                runOnUiThread(() -> {
                    status.setText("ADB：连接或 shell 执行失败");
                    logs.setText(readLocalLog() + "\n\n" + err);
                    refresh.setEnabled(true);
                });
            }
        }).start();
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 16, 20, 16);

        TextView title = new TextView(this);
        title.setText("Clash 开机助手");
        title.setTextSize(24);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(18);
        status.setText("应用已打开，点击刷新检查状态");
        root.addView(status);

        refresh = new Button(this);
        refresh.setText("刷新状态 / 查看日志");
        root.addView(refresh);

        start = new Button(this);
        start.setText("启动 Clash 和 Watcher");
        root.addView(start);

        logs = new TextView(this);
        logs.setTextSize(14);
        logs.setTextIsSelectable(true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(logs);

        root.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1
        ));

        setContentView(root);

        localLog("MainActivity opened");
        refreshLocalLog();

        refresh.setOnClickListener(v -> checkAdb());

        start.setOnClickListener(v -> {
            try {
                Intent i = new Intent(this, BootService.class);
                i.setAction("MANUAL_START");

                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(i);
                } else {
                    startService(i);
                }

                Toast.makeText(
                    this,
                    "已请求启动 Clash 和 Watcher",
                    Toast.LENGTH_SHORT
                ).show();

                localLog("Manual service start requested");
                refreshLocalLog();
            } catch (Exception e) {
                Toast.makeText(
                    this,
                    "启动失败：" + e.toString(),
                    Toast.LENGTH_LONG
                ).show();

                localLog("Manual start failed: " + e);
                refreshLocalLog();
            }
        });
    }
}
EOF
