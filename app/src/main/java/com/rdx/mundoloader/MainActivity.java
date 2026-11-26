package com.rdx.mundoloader;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.featurestic.mundo.core.MundoNative;
import com.featurestic.mundo.core.MundoStreamproofGuard;

import net_62v.external.MetaActivationManager;
import net_62v.external.MetaActivityManager;
import net_62v.external.MetaApplicationInstaller;
import net_62v.external.MetaPackageManager;

public class MainActivity extends AppCompatActivity {
    private static final String TARGET_PKG = "com.pubg.imobile";

    private TextView tvHeader, tvExpiry, tvEngineStatus, tvGameState;
    private Switch switchHideEsp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHeader = findViewById(R.id.tvBrandHeader);
        tvExpiry = findViewById(R.id.tvExpiryValue);
        tvEngineStatus = findViewById(R.id.tvEngineStatus);
        tvGameState = findViewById(R.id.tvGameState);
        switchHideEsp = findViewById(R.id.switchHideEsp);

        String modname = getIntent().getStringExtra("modname");
        String expiry = getIntent().getStringExtra("expiry");
        if (modname != null && !modname.isEmpty()) tvHeader.setText(modname);
        if (expiry != null && !expiry.isEmpty()) tvExpiry.setText("Expires: " + expiry);

        refreshStatus();

        switchHideEsp.setOnCheckedChangeListener((btn, isChecked) -> {
            MundoStreamproofGuard.setEnabled(isChecked, MainActivity.this);
            Toast.makeText(MainActivity.this,
                    isChecked ? "Hide ESP Enabled" : "Hide ESP Disabled", Toast.LENGTH_SHORT).show();
        });

        Button btnClone = findViewById(R.id.btnCloneGame);
        btnClone.setOnClickListener(v -> {
            int res = MetaApplicationInstaller.cloneApp(TARGET_PKG, "0");
            Toast.makeText(this,
                    res == 0 ? "Linked com.pubg.imobile (Zero-Copy)" : "Install BGMI (com.pubg.imobile) on phone first",
                    Toast.LENGTH_SHORT).show();
            refreshStatus();
        });

        Button btnLaunch = findViewById(R.id.btnLaunchGame);
        btnLaunch.setOnClickListener(v -> {
            if (!MetaPackageManager.isInnerAppInstalled(TARGET_PKG)) {
                int res = MetaApplicationInstaller.cloneApp(TARGET_PKG, "0");
                if (res != 0) {
                    Toast.makeText(this, "com.pubg.imobile not found on device", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            MetaActivityManager.launchApp(TARGET_PKG);
        });

        Button btnClean = findViewById(R.id.btnCleanData);
        btnClean.setOnClickListener(v -> {
            MetaPackageManager.cleanPackageDataAsUser(TARGET_PKG, "0");
            Toast.makeText(this, "Virtual sandbox data cleaned", Toast.LENGTH_SHORT).show();
        });

        Button btnLogout = findViewById(R.id.btnSignOut);
        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void refreshStatus() {
        String ver = MundoNative.isLoaded() ? MundoNative.nativeGetEngineVersion() : "Java-Fallback";
        boolean sdkOk = MetaActivationManager.getActivationStatus();
        tvEngineStatus.setText("Engine: " + ver + " | SDK: " + (sdkOk ? "Active" : MetaActivationManager.getActivationMessage()));

        boolean installed = MetaPackageManager.isInnerAppInstalled(TARGET_PKG);
        tvGameState.setText("Target: " + TARGET_PKG + " (" + (installed ? "Ready" : "Not Found on Host") + ")");
    }
}
