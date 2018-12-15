package net.pierrox.lightning_launcher.llscript.compass;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * This is the definition of the counter plugin.
 * It will execute a script (compass_init.js) which will do the setup job.
 * Once executed, the compass_init.js script will not be kept into the launcher: its purpose is
 * to create an item, configure it and install other scripts.
 */
public class Compass extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent data = new Intent();
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_ID, R.raw.compass_init);
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_FLAGS, 0);
        data.putExtra(Constants.INTENT_EXTRA_EXECUTE_ON_LOAD, true);
        data.putExtra(Constants.INTENT_EXTRA_DELETE_AFTER_EXECUTION, true);
        setResult(RESULT_OK, data);
        finish();
    }
}
