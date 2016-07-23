package com.github.jinsen47.pokefaker.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Created by Jinsen on 16/7/13.
 */
public class LocationHolder {
    private static final String TAG = LocationHolder.class.getSimpleName();
    private static LocationHolder sInstance;

    private static long REPEAT_TIME = 1000;

    private Context mContext;
    private Handler mHandler;
    private SharedPreferences mPreference = null;
    private XSharedPreferences mXPreference = null;
    private LatLng mCacheLatLng;

    private List<DataSetObserver> mObervers = new ArrayList<>();

    private boolean isRunning = false;

    private LocationHolder(Context context) {
        if (context != null) {
            mHandler = new Handler(Looper.getMainLooper());
            mPreference = context.getSharedPreferences("location", Context.MODE_WORLD_READABLE);
        } else {
            mXPreference = new XSharedPreferences("com.github.jinsen47.pokefaker", "location");
            mXPreference.makeWorldReadable();
        }
    }

    public void start() {
        if (!isRunning) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyDataObserver();
                    if (isRunning) {
                        mHandler.postDelayed(this, REPEAT_TIME);
                    }
                }
            }, REPEAT_TIME);
            isRunning = true;
        }
    }

    public void stop() {
        mHandler.removeCallbacks(null);
        isRunning = false;
    }

    public static LocationHolder getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LocationHolder.class) {
                if (sInstance == null) {
                    sInstance = new LocationHolder(context);
                }
            }
        }
        return sInstance;
    }

    public synchronized void postLocation(LatLng latLng) {
        if (latLng != null) {
            writeToPreference(latLng);
            mCacheLatLng = latLng;
            notifyDataObserver();
        }
    }

    private void notifyDataObserver() {
        Log.v(TAG, "posting data:" + mCacheLatLng.toString());
        for (DataSetObserver o : mObervers) {
            o.onChanged();
        }
    }

    private void writeToPreference(LatLng latLng) {
        if (mPreference != null) {
            SharedPreferences.Editor editor = mPreference.edit();
            editor.putString("latitude", latLng.latitude + "");
            editor.putString("longitude", latLng.longitude + "");
            editor.apply();
        }
    }


    public LatLng pollLatLng() {
        if (mXPreference != null) {
            mXPreference.reload();
            mCacheLatLng = restoreFromXpreference();
        }
        return mCacheLatLng;
    }

    public Location pollLocation(String provider) {
        Location l = new LocationBuilder().defaultBuilder().location(pollLatLng()).build();
        l.setProvider(provider);
        return l;
    }

    private LatLng restoreFromXpreference() {
        if (mXPreference != null) {
            mXPreference.reload();
            double latitude = Double.parseDouble(mXPreference.getString("latitude", "0"));
            double longitude = Double.parseDouble(mXPreference.getString("longitude", "0"));
            return new LatLng(latitude, longitude);
        }
        return null;
    }

    public void registerObserver(DataSetObserver o) {
        if (o != null && !mObervers.contains(o)) {
            mObervers.add(o);
        }
    }

    public void unregisterObserver(DataSetObserver o) {
        if (mObervers.contains(o)) {
            mObervers.remove(o);
        }
    }

    private static class LocationBuilder {
        Location location;
        public LocationBuilder() {
            location = new Location("gps");
        }
        public LocationBuilder provider(String provider) {
            location.setProvider(provider);
            return this;
        }
        public LocationBuilder location(LatLng latLng) {
            location.setLatitude(latLng.latitude);
            location.setLongitude(latLng.longitude);
            return this;
        }
        public LocationBuilder accuracy(float acc) {
            location.setAccuracy(acc);
            return this;
        }
        public LocationBuilder defaultBuilder() {
            location.reset();
            location.setProvider("gps");
            location.setAccuracy(8.0f);
            location.setSpeed(0.0f);
            location.setBearing(0.0f);
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            location.setTime(System.currentTimeMillis());
            return this;
        }
        public Location build() {
            return location;
        }
    }
}
