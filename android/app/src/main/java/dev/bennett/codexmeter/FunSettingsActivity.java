package dev.bennett.codexmeter;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** Compatibility entry for existing settings links; retains all stored historical rules/notes. */
public final class FunSettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        Ui.applySelectedTheme(this);super.onCreate(state);
        startActivity(new Intent(this,MeterSettingsActivity.class));finish();
    }
}
