package net.pierrox.lightning_launcher.views;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class SystemUIHelper {

    public static void setStatusBarVisibility(Window w, boolean visible, boolean overlap) {
        View decorView = w.getDecorView();
        decorView.setSystemUiVisibility(visible ? 0 : View.SYSTEM_UI_FLAG_FULLSCREEN);

    }
}
