cat > app/src/main/java/tw/haier/clashboot/BootReceiver.java <<'EOF'
package tw.haier.clashboot;

import android.content.*;
import android.os.*;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
            Toast.makeText(
                c,
                "Clash 开机助手已启动",
                Toast.LENGTH_LONG
            ).show();

            Intent s = new Intent(c, BootService.class);
            s.setAction("BOOT_START");

            if (Build.VERSION.SDK_INT >= 26) {
                c.startForegroundService(s);
            } else {
                c.startService(s);
            }
        }
    }
}
EOF
