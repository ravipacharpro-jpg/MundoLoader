package com.rdx.mundoloader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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
    private static final String[] GAME_LABELS = {
            "BGMI (India)",
            "BGMI Air (com.krafton.bgmilite)",
            "PUBG Mobile (Global)",
            "PUBG Mobile (KR)",
            "PUBG Mobile (TW)",
            "PUBG Mobile (VN)"
    };
    private static final String[] GAME_PKGS = {
            "com.pubg.imobile",
            "com.krafton.bgmilite",
            "com.tencent.ig",
            "com.pubg.krmobile",
            "com.rekoo.pubgm",
            "com.vng.pubgmobile"
    };

    private String selectedPkg = GAME_PKGS[0];
    private TextView tvHeader, tvExpiry, tvEngineStatus, tvGameState, tvSdkPill, tvAnnouncement;
    private android.view.View layoutAnnouncement;
    private Spinner spGameVersion;
    private Switch switchHideEsp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHeader = findViewById(R.id.tvBrandHeader);
        tvExpiry = findViewById(R.id.tvExpiryValue);
        tvEngineStatus = findViewById(R.id.tvEngineStatus);
        tvGameState = findViewById(R.id.tvGameState);
        tvSdkPill = findViewById(R.id.tvSdkPill);
        tvAnnouncement = findViewById(R.id.tvAnnouncement);
        layoutAnnouncement = findViewById(R.id.layoutAnnouncement);
        spGameVersion = findViewById(R.id.spGameVersion);
        switchHideEsp = findViewById(R.id.switchHideEsp);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, GAME_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGameVersion.setAdapter(adapter);

        int defaultIdx = 0;
        for (int i = 0; i < GAME_PKGS.length; i++) {
            if (MetaPackageManager.isInnerAppInstalled(GAME_PKGS[i])) {
                defaultIdx = i;
                break;
            }
        }
        selectedPkg = GAME_PKGS[defaultIdx];
        spGameVersion.setSelection(defaultIdx);
        spGameVersion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                selectedPkg = GAME_PKGS[pos];
                refreshStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String modname = getIntent().getStringExtra("modname");
        String expiry = getIntent().getStringExtra("expiry");
        String announcement = getIntent().getStringExtra("announcement");
        if (modname != null && !modname.isEmpty()) tvHeader.setText(modname);
        if (expiry != null && !expiry.isEmpty()) tvExpiry.setText("Expires: " + expiry);
        if (announcement != null && !announcement.trim().isEmpty()) {
            tvAnnouncement.setText(announcement.trim());
            layoutAnnouncement.setVisibility(android.view.View.VISIBLE);
        }

        refreshStatus();
        ensureStoragePermission();

        switchHideEsp.setOnCheckedChangeListener((btn, isChecked) -> {
            MundoStreamproofGuard.setEnabled(isChecked, MainActivity.this);
            Toast.makeText(MainActivity.this,
                    isChecked ? "Hide ESP Enabled" : "Hide ESP Disabled", Toast.LENGTH_SHORT).show();
        });

        Button btnClone = findViewById(R.id.btnCloneGame);
        btnClone.setOnClickListener(v -> {
            int res = MetaApplicationInstaller.cloneApp(selectedPkg, "0");
            Toast.makeText(this,
                    res == 0 ? "Linked " + selectedPkg + " (Zero-Copy)" : "Install " + selectedPkg + " on phone first",
                    Toast.LENGTH_SHORT).show();
            refreshStatus();
            ensureStoragePermission();
        });

        Button btnLaunch = findViewById(R.id.btnLaunchGame);
        btnLaunch.setOnClickListener(v -> {
            if (!MetaPackageManager.isInnerAppInstalled(selectedPkg)) {
                int res = MetaApplicationInstaller.cloneApp(selectedPkg, "0");
                if (res != 0) {
                    Toast.makeText(this, selectedPkg + " not found on device", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            MetaActivityManager.launchApp(selectedPkg);
        });

        Button btnClean = findViewById(R.id.btnCleanData);
        btnClean.setOnClickListener(v -> {
            MetaPackageManager.cleanPackageDataAsUser(selectedPkg, "0");
            Toast.makeText(this, "Virtual sandbox data cleaned for " + selectedPkg, Toast.LENGTH_SHORT).show();
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
        if (tvSdkPill != null) {
            tvSdkPill.setText(sdkOk ? "Active" : "Offline");
        }

        boolean installed = MetaPackageManager.isInnerAppInstalled(selectedPkg);
        tvGameState.setText("Target: " + selectedPkg + " (" + (installed ? "Ready" : "Not Found on Host") + ")");
    }

    private void ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Grant All Files Access for OBB support", Toast.LENGTH_LONG).show();
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Throwable t) {
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                    } catch (Throwable ignored) {}
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 1001);
            }
        }
    }
}
