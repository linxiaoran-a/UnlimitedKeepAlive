import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import java.io.File;
import java.io.FileOutputStream;

public class Main {
    public static void main(String[] args) {
        try {
            // ActivityThread is hidden; reach it via reflection (app_process runtime has it)
            android.os.Looper.prepareMainLooper();
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("systemMain").invoke(null);
            Context ctx = (Context) at.getMethod("getSystemContext").invoke(thread);
            PackageManager pm = ctx.getPackageManager();
            String outDir = args.length > 0 ? args[0] : "/data/local/tmp/ka_icons";
            new File(outDir).mkdirs();
            for (int i = 1; i < args.length; i++) {
                String pkg = args[i];
                File out = new File(outDir, pkg + ".png");
                if (out.exists()) continue;
                try {
                    Drawable d = pm.getApplicationIcon(pkg);
                    int size = 108;
                    Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                    Canvas cv = new Canvas(bm);
                    d.setBounds(0, 0, size, size);
                    d.draw(cv);
                    FileOutputStream fo = new FileOutputStream(out);
                    bm.compress(Bitmap.CompressFormat.PNG, 100, fo);
                    fo.close();
                    System.out.println("OK " + pkg);
                } catch (Throwable t) {
                    System.out.println("FAIL " + pkg + " " + t);
                }
            }
        } catch (Throwable t) {
            Throwable c = t;
            StringBuilder sb = new StringBuilder("FATAL ");
            while (c != null) { sb.append(c).append(" <- "); c = c.getCause(); }
            System.out.println(sb);
        }
    }
}
