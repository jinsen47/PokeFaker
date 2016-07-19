package com.github.jinsen47.pokefaker.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Process;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.jinsen47.pokefaker.R;
import com.github.jinsen47.pokefaker.app.event.MapPickEvent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.greenrobot.eventbus.EventBus;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private SupportMapFragment mMapFragment;
    private ImageView mZoomIn;
    private ImageView mZoomOut;

    private GoogleMap mMap;
    private MarkerOptions mMarkerOpts;
    private Marker mMarker;

    private SharedPreferences mSp;
    private boolean isServiceRunning = false;

    private AlertDialog mQuitDialog;

    private DataSetObserver mLocationChangeObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            LocationHolder holder = LocationHolder.getInstance(MainActivity.this);
            LatLng l = holder.pollLatLng();
            Log.d(TAG, "receive location:" + (l == null ? "null" : l.toString()));
            if (l != null && mMarker != null) {
                mMarker.setPosition(l);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setLogo(R.mipmap.icon_10);
        }

        mMapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mZoomIn = ((ImageView) findViewById(R.id.icon_zoom_in));
        mZoomOut = ((ImageView) findViewById(R.id.icon_zoom_out));

        mMapFragment.getMapAsync(this);
        mZoomIn.setOnClickListener(this);
        mZoomOut.setOnClickListener(this);

        mSp = getSharedPreferences("location", MODE_PRIVATE);
        mQuitDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(getString(android.R.string.dialog_alert_title))
                .setMessage(getString(R.string.quit))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopService(new Intent(MainActivity.this, LocationService.class));
                        Process.killProcess(Process.myPid());
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isServiceRunning = StateCheckUtil.isLocationServiceRunning(this, LocationService.class);
        invalidateOptionsMenu();
        LocationHolder.getInstance(this).registerObserver(mLocationChangeObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocationHolder.getInstance(this).unregisterObserver(mLocationChangeObserver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isServiceRunning) {
            menu.findItem(R.id.action_service).setTitle(getString(R.string.action_service_stop));
        } else {
            menu.findItem(R.id.action_service).setTitle(getString(R.string.action_service_start));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_service:
                if (isServiceRunning) {
                    stopService(new Intent(this, LocationService.class));
                    isServiceRunning = false;
                } else {
                    startService(new Intent(this, LocationService.class));
                    isServiceRunning = true;
                }
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        mQuitDialog.show();
    }

    private void fetchSavedLocation() {
        String latitudeString = mSp.getString("latitude", null);
        String longitudeString = mSp.getString("longitude", null);
        double latitude = 0.0d;
        double longitude = 0.0d;
        if (!TextUtils.isEmpty(latitudeString)) {
            latitude = Double.parseDouble(latitudeString);
        }
        if (!TextUtils.isEmpty(longitudeString)) {
            longitude = Double.parseDouble(longitudeString);
        }
        mMarker.setPosition(new LatLng(latitude, longitude));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap == null) {
            mMap = googleMap;
            mMap.setOnMapClickListener(this);
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    return true;
                }
            });
        }

        mMarkerOpts = new MarkerOptions().position(new LatLng(0, 0));
        mMarker = googleMap.addMarker(mMarkerOpts);
        fetchSavedLocation();
        if (!(mMarker.getPosition().latitude == 0.0 && mMarker.getPosition().longitude == 0.0)) {
            CameraPosition cp = new CameraPosition.Builder().target(mMarker.getPosition())
                    .zoom(15.5f)
                    .bearing(0)
                    .tilt(25)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mMarker.setPosition(latLng);
        EventBus.getDefault().post(new MapPickEvent(latLng));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.icon_zoom_in:
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
                break;
            case R.id.icon_zoom_out:
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
                break;
        }
    }
}
