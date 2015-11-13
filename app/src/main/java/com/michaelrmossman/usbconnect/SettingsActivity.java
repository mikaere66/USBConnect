package com.michaelrmossman.usbconnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.RadioButton;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences mSharedPreferences;
    private RadioButton protocolPref;
    private RadioButton rbYesRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rbYesRoot = (RadioButton) findViewById(R.id.rbYesRoot);
        RadioButton rbNoRoot = (RadioButton) findViewById(R.id.rbNoRoot);
        protocolPref = (RadioButton) findViewById(R.id.rbPTP);

        String PREFS = "Prefs";
        mSharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Get root at Startup defaults to false if NOT found
        // Better for detecting NO root access, e.g. emulator
        Boolean rootPref = mSharedPreferences.getBoolean("rootPref", false);
        if(rootPref) {
            rbYesRoot.setChecked(true);
        } else {
            rbNoRoot.setChecked(true);
        }
        // Preferred communications protocol defaults to MTP if NOT found
        String protocolPref = mSharedPreferences.getString("protocolPref", "mtp");
        if (protocolPref.equals("ptp")) this.protocolPref.setChecked(true);
    }

    public void saveSharedPrefs(View view) {
        SharedPreferences.Editor e = mSharedPreferences.edit();
        // Save checked Root preference RadioButton to SharedPreferences
        e.putBoolean("rootPref", rbYesRoot.isChecked());
        // Save preferred communications protocol to use when UMS is off
        if (protocolPref.isChecked()) {
            e.putString("protocolPref", "ptp");
        } else {
            e.putString("protocolPref", "mtp");
        }
        e.apply();
        finish();
    }

    public void cancelSharedPrefs(View view) {
        finish();
    }
}