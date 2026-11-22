package com.piergabory.minimalhdmi;

import android.app.Activity;
import android.os.Bundle;

/** Handles the remote's standard Android TV input-panel action. */
public final class InputActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new android.content.Intent(this, MainActivity.class)
                .setAction("com.piergabory.minimalhdmi.SHOW_INPUTS"));
        finish();
    }
}
