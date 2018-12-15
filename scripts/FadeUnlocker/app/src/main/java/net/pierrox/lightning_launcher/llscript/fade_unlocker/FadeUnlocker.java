package net.pierrox.lightning_launcher.llscript.fade_unlocker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class FadeUnlocker extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent data = new Intent();
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_ID, R.raw.fade_unlocker_init);
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_FLAGS, Constants.FLAG_APP_MENU);
        data.putExtra(Constants.INTENT_EXTRA_EXECUTE_ON_LOAD, true);
        data.putExtra(Constants.INTENT_EXTRA_DELETE_AFTER_EXECUTION, true);
        setResult(RESULT_OK, data);
        finish();
    }
}
