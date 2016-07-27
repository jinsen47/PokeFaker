package com.github.jinsen47.pokefaker.app.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActionMenuView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.github.jinsen47.pokefaker.R;
import com.github.jinsen47.pokefaker.app.event.MapPickEvent;
import com.github.jinsen47.pokefaker.app.service.LocationHolder;
import com.github.jinsen47.pokefaker.app.service.LocationService;
import com.github.jinsen47.pokefaker.app.util.PermissionUtil;
import com.github.jinsen47.pokefaker.app.util.StateCheckUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mypopsy.drawable.SearchArrowDrawable;
import com.mypopsy.drawable.ToggleDrawable;
import com.mypopsy.drawable.util.Bezier;
import com.mypopsy.widget.FloatingSearchView;
import com.mypopsy.widget.internal.ViewUtils;

import org.greenrobot.eventbus.EventBus;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int ICON_SEARCH = 1;
    private static final int ICON_DRAWER = 2;

    private SupportMapFragment mMapFragment;
    private ImageView mZoomIn;
    private ImageView mZoomOut;
    private FloatingSearchView mSearchView;
    private FloatingActionButton mShareButton;
    private FloatingActionButton mGoButton;
    private FloatingActionMenu mMenu;
    private FloatingActionButton mAboutButton;

    private GoogleMap mMap;
    private MarkerOptions mMarkerOpts;
    private Marker mMarker;

    private SharedPreferences mSp;
    private boolean isServiceRunning = false;

    private AlertDialog mQuitDialog;

    private Handler mHandler;

    private DataSetObserver mLocationChangeObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            LocationHolder holder = LocationHolder.getInstance(MainActivity.this);
            LatLng l = holder.pollLatLng();
            if (l != null &&
                mMarker != null &&
                !l.equals(mMarker.getPosition())) {
                mMarker.setPosition(l);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mMapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mZoomIn = ((ImageView) findViewById(R.id.icon_zoom_in));
        mZoomOut = ((ImageView) findViewById(R.id.icon_zoom_out));
        mSearchView = ((FloatingSearchView) findViewById(R.id.view_search));
        mShareButton = ((FloatingActionButton) findViewById(R.id.fab_share));
        mGoButton = ((FloatingActionButton) findViewById(R.id.fab_start));
        mAboutButton = ((FloatingActionButton) findViewById(R.id.fab_about));
        mMenu = ((FloatingActionMenu) findViewById(R.id.fab_menu));

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
        updateNavigationIcon(ICON_SEARCH);
        mSearchView.showLogo(true);
        mSearchView.getMenu().findItem(R.id.menu_clear).setVisible(false);
        mSearchView.setOnSearchListener(mSearchViewListener);
        mSearchView.setOnIconClickListener(mSearchViewListener);
        mSearchView.setOnSearchFocusChangedListener(mSearchViewListener);
        mSearchView.setOnMenuItemClickListener(mSearchViewListener);

        mShareButton.setOnClickListener(this);
        mAboutButton.setOnClickListener(this);
        mGoButton.setOnClickListener(this);

        mHandler = new Handler();
    }

    private void updateNavigationIcon(int itemId) {
        Context context = mSearchView.getContext();
        Drawable drawable = null;

        switch(itemId) {
            case ICON_SEARCH:
                drawable = new SearchArrowDrawable(context);
                break;
            case ICON_DRAWER:
                drawable = new android.support.v7.graphics.drawable.DrawerArrowDrawable(context);
                break;
            }
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ViewUtils.getThemeAttrColor(this, R.attr.colorControlNormal));
        mSearchView.setIcon(drawable);
    }

    @Override
    protected void onStart() {
        super.onStart();
        isServiceRunning = StateCheckUtil.isLocationServiceRunning(this, LocationService.class);
        LocationHolder.getInstance(this).registerObserver(mLocationChangeObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocationHolder.getInstance(this).unregisterObserver(mLocationChangeObserver);
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
            setMap(mMap);
        }

        mMarkerOpts = new MarkerOptions().position(new LatLng(0, 0));
        mMarker = googleMap.addMarker(mMarkerOpts);
        fetchSavedLocation();
        if (!(mMarker.getPosition().latitude == 0.0 && mMarker.getPosition().longitude == 0.0)) {
            moveCamera(true);
        }
    }

    private void setMap(GoogleMap map) {
        float density = getResources().getDisplayMetrics().density;
        map.setPadding(0, ((int) (82* density)), 0, 0);
        map.setOnMapClickListener(this);
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });
    }

    private void moveCamera(boolean animate) {
        CameraPosition cp = new CameraPosition.Builder().target(mMarker.getPosition())
                .zoom(15.5f)
                .bearing(0)
                .tilt(25)
                .build();
        if (animate) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mMarker.setPosition(latLng);
        postLocation(latLng);
    }

    private void postLocation(LatLng latlng) {
        EventBus.getDefault().post(new MapPickEvent(latlng));
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
            case R.id.fab_about:
                startActivity(new Intent(this, AboutActivity.class));
                mMenu.close(false);
                break;
            case R.id.fab_share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title));
                intent.putExtra(Intent.EXTRA_TEXT, mMarker.getPosition().latitude + "," + mMarker.getPosition().longitude);
                startActivity(Intent.createChooser(intent, "Share with"));
                break;
            case R.id.fab_start:
                if (PermissionUtil.checkAndRequestPopupWindowPermission(this)) break;
                if (PermissionUtil.checkAndRequestMockSetting(this)) break;

                if (isServiceRunning) {
                    stopService(new Intent(this, LocationService.class));
                    mGoButton.setColorNormal(ContextCompat.getColor(this, R.color.colorStopped));
                    mGoButton.setImageResource(R.drawable.ic_send);
                    isServiceRunning = false;
                } else {
                    Intent i = new Intent(this, LocationService.class);
                    i.putExtra("position", mMarker.getPosition());
                    startService(i);
                    postLocation(mMarker.getPosition());
                    mGoButton.setColorNormal(ContextCompat.getColor(this, R.color.colorRunning));
                    mGoButton.setImageResource(R.drawable.ic_done);
                    isServiceRunning = true;
                }
                break;
        }
    }

    private static LatLng parseLatlng(String searchText) {
        if (!searchText.contains(",")) return null;
        String[] positionArray = searchText.split(",");
        if (positionArray.length != 2) return null;
        try {
            double lat = Double.valueOf(positionArray[0]);
            double lng = Double.valueOf(positionArray[1]);
            return new LatLng(lat, lng);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private SearchViewListener mSearchViewListener = new SearchViewListener();

    private class SearchViewListener implements FloatingSearchView.OnSearchListener, FloatingSearchView.OnIconClickListener, FloatingSearchView.OnSearchFocusChangedListener, ActionMenuView.OnMenuItemClickListener {

        @Override
        public void onNavigationClick() {
            mSearchView.setActivated(!mSearchView.isActivated());
        }

        @Override
        public void onFocusChanged(final boolean focused) {
            boolean textEmpty = mSearchView.getText().length() == 0;
            mSearchView.getMenu().findItem(R.id.menu_clear).setVisible(focused);
            mSearchView.showLogo(!focused && textEmpty);

            if (focused) {
                mSearchView.showIcon(true);
            }
        }

        @Override
        public void onSearchAction(CharSequence charSequence) {
            LatLng result = parseLatlng(charSequence.toString());
            if (result != null) {
                mMarker.setPosition(result);
                moveCamera(true);
                mSearchView.setActivated(false);
                postLocation(result);
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.invalid_input), Toast.LENGTH_SHORT).show();
                Log.w(TAG, "invalid input = " + charSequence);
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_clear:
                    mSearchView.setText("");
                    break;
            }
            return true;
        }
    }
}
