package com.github.jinsen47.pokefaker.app;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.jinsen47.pokefaker.BuildConfig;
import com.github.jinsen47.pokefaker.R;

public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener{
    private TextView mTextInfo;
    private Button mBtnStart;

    private boolean isMockable;
    private boolean isPermissionGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        }

        mTextInfo = ((TextView) findViewById(R.id.text_info));
        mBtnStart = ((Button) findViewById(R.id.btn_start));

        mBtnStart.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // service is running, just go to main activity
        if (StateCheckUtil.isLocationServiceRunning(this, LocationService.class)) {
            startActivity(new Intent(this, MainActivity.class));
            this.finish();
        }

        isMockable = checkMockSetting();
        isPermissionGranted = checkPermission();

        mTextInfo.setText("");
        if (!isMockable) {
            mTextInfo.append(getString(R.string.mock_failed));
            mBtnStart.setText(getString(R.string.jump_setting));
        } else {
            mTextInfo.append(getString(R.string.mock_success));
            mBtnStart.setText(getString(R.string.jump_map));
        }

        if (!isPermissionGranted) {
            mTextInfo.append(getString(R.string.permission_failed));
        } else {
            mTextInfo.append(getString(R.string.permission_success));
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        try {
            return Settings.canDrawOverlays(this);
        } catch (NoSuchMethodError e) {
            return false;
        }
    }

    private boolean checkMockSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AppOpsManager opsManager = (AppOpsManager) this.getSystemService(Context.APP_OPS_SERVICE);
            return (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID)== AppOpsManager.MODE_ALLOWED);
        } else {
            try {
                int ret = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION);
                return ret == 1;
            } catch (Settings.SettingNotFoundException e) {
                return false;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                if (!isMockable) {
                    startActivity(new Intent().setClassName("com.android.settings", "com.android.settings.DevelopmentSettings"));
                } else {
                    startService(new Intent(this, LocationService.class));
                    startActivity(new Intent(this, MainActivity.class));
                }
        }
    }
}
