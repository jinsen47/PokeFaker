package com.github.jinsen47.pokefaker.app;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import com.github.jinsen47.pokefaker.R;
import com.github.jinsen47.pokefaker.app.event.MapPickEvent;
import com.google.android.gms.maps.model.LatLng;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class LocationService extends Service  implements DirectionLayout.ServiceListener {
    private static final String TAG = LocationService.class.getSimpleName();
    private static final long CHECK_INTERVAL = 2000;
    private static final String POKEMON_PACKAGE = "com.nianticlabs.pokemongo";
    private Intent mIntent;
    private DirectionLayout mDirectionLayout;
    private DirectionLayout.onDirectionListener mDirectionListener;
    private HandlerThread mHandlerThread = new HandlerThread("dpad_service");
    private Handler mHandler;

    private LatLng mCurrentLatLng;
    private List<MockProvider> mMockProviders = new ArrayList<>();
    private boolean hasWindowAdded = false;

    public LocationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDirectionLayout = new DirectionLayout(this,this);

        mMockProviders.add(new MockProvider(this, LocationManager.GPS_PROVIDER));
        mMockProviders.add(new MockProvider(this, LocationManager.NETWORK_PROVIDER));

        setListener(mDirectionLayout);

        EventBus.getDefault().register(this);

        mCurrentLatLng = new LatLng(0, 0);
        fetchSavedLocation();
        LocationHolder.getInstance(this).start();

        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPokemonRunning()) {
                    if (!hasWindowAdded) {
                        addAlertWindow();
                        hasWindowAdded = true;
                    }
                } else {
                    if (hasWindowAdded) {
                        removeAlertWindow();
                        hasWindowAdded = false;
                    }
                }
                mHandler.postDelayed(this, CHECK_INTERVAL);
            }
        }, CHECK_INTERVAL);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.service_running_title))
                .setSmallIcon(R.mipmap.icon_10)
                .setContentText(getString(R.string.service_running_content))
                .setAutoCancel(false)
                .setOngoing(true)
                .build();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.contentIntent = pendingIntent;
        startForeground(1, notification);

        // TODO: 16/7/13 refactor this ugly impl
        updateLocation();
        for (MockProvider m: mMockProviders) {
            m.start();
        }
    }

    private void fetchSavedLocation() {
        SharedPreferences mSp = getSharedPreferences("location", MODE_PRIVATE);
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
        mCurrentLatLng = new LatLng(latitude, longitude);
    }

    private boolean isPokemonRunning() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager am = ((ActivityManager) getSystemService(ACTIVITY_SERVICE));
            List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();
            if (list != null && !list.isEmpty()) {
                if (list.get(0).processName.equals(POKEMON_PACKAGE)) {
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            List<AndroidAppProcess> list = AndroidProcesses.getRunningAppProcesses();
            if (list != null && !list.isEmpty()) {
                for (AndroidAppProcess p : list) {
                    if (p.getPackageName().equals(POKEMON_PACKAGE) && p.foreground) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    private void addAlertWindow() {
        WindowManager windowManager = ((WindowManager) getSystemService(WINDOW_SERVICE));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.END | Gravity.BOTTOM;
        lp.y = (int) (100 * getResources().getDisplayMetrics().density);
        lp.alpha = 0.5f;

        windowManager.addView(mDirectionLayout, lp);
    }

    private void removeAlertWindow() {
        WindowManager windowManager = ((WindowManager) getSystemService(WINDOW_SERVICE));

        windowManager.removeView(mDirectionLayout);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        LocationHolder.getInstance(this).stop();
        for (MockProvider m : mMockProviders) {
            m.remove();
        }
        mHandler.removeCallbacks(null);
    }

    @Subscribe
    public void onPickEvent(MapPickEvent e) {
        mCurrentLatLng = e.latLng;
        updateLocation();
    }

    private void updateLocation() {
        LocationHolder.getInstance(this).postLocation(mCurrentLatLng);
    }

    private void setListener(DirectionLayout layout) {
        mDirectionListener = new DirectionLayout.onDirectionListener() {
            @Override
            public void onDirection(double agle) {
                move(mCurrentLatLng, agle);
            }
        };
        layout.setOnDirectionLisener(mDirectionListener);
    }

    private void move(LatLng ori, double agle) {
        double BASE = 0.0000008;
        double latitude = ori.latitude;
        double longitude = ori.longitude;
        latitude += BASE*Math.sin(agle);
        longitude += BASE*Math.cos(agle);
        mCurrentLatLng = new LatLng(latitude, longitude);
        updateLocation();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIntent = intent;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void OnCloseService() {
        stopService(mIntent);
    }
}
