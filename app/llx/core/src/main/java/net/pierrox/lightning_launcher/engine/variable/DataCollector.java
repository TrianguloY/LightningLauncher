package net.pierrox.lightning_launcher.engine.variable;

import android.content.res.Resources;

public interface DataCollector {
    void onPause();

    void onResume();

    void end();

    BuiltinVariable[] getBuiltinVariables(Resources resources);
}
