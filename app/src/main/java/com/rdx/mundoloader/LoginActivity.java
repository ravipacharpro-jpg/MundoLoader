package com.rdx.mundoloader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {
    private static final String KEY_PANEL_URL = "https://featurestic-key-panel.vercel.app/connect";
    private static final String LOADER_SLUG = "rdx-loader";

    private EditText etKey;
    private Button btnSignIn;
    private ProgressBar progress;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etKey = findViewById(R.id.etKey);
        btnSignIn = findViewById(R.id.btnSignIn);
        progress = findViewById(R.id.progressLogin);
        tvStatus = findViewById(R.id.tvLoginStatus);

        SharedPreferences sp = getSharedPreferences("mundo_loader_prefs", MODE_PRIVATE);
        String saved = sp.getString("saved_key", "");
        if (!saved.isEmpty()) etKey.setText(saved);

        btnSignIn.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String key = etKey.getText().toString().trim();
        if (key.isEmpty()) {
            Toast.makeText(this, "Enter license key", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSignIn.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        tvStatus.setText("Authenticating...");

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String serial = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                if (serial == null || serial.isEmpty()) serial = "MUNDO_ANDROID_DEV";

                String body = "user_key=" + URLEncoder.encode(key, "UTF-8")
                        + "&serial=" + URLEncoder.encode(serial, "UTF-8")
                        + "&game=com.pubg.imobile"
                        + "&slug=" + URLEncoder.encode(LOADER_SLUG, "UTF-8");

                conn = (HttpURLConnection) new URL(KEY_PANEL_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder sb = new StringBuilder();
                if (is != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                }

                JSONObject json = new JSONObject(sb.toString());
                boolean ok = json.optBoolean("status", false);
                if (!ok) {
                    String reason = json.optString("reason", "Authentication failed");
                    runOnUiThread(() -> {
                        btnSignIn.setEnabled(true);
                        progress.setVisibility(View.GONE);
                        tvStatus.setText(reason);
                        Toast.makeText(LoginActivity.this, reason, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                JSONObject data = json.optJSONObject("data");
                String exp = data != null ? data.optString("EXP", "-") : "-";
                String modname = data != null ? data.optString("modname", "Mundo Loader") : "Mundo Loader";

                getSharedPreferences("mundo_loader_prefs", MODE_PRIVATE)
                        .edit().putString("saved_key", key).apply();

                runOnUiThread(() -> {
                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    i.putExtra("license_key", key);
                    i.putExtra("expiry", exp);
                    i.putExtra("modname", modname);
                    startActivity(i);
                    finish();
                });
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    btnSignIn.setEnabled(true);
                    progress.setVisibility(View.GONE);
                    tvStatus.setText(t.getMessage() != null ? t.getMessage() : "Network error");
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}
