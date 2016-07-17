package com.github.jinsen47.pokefaker;

import android.content.ContentResolver;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.provider.Settings;

import com.github.jinsen47.pokefaker.app.LocationHolder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Jinsen on 16/7/12.
 * This class was some attempted from hooking system, but failed.
 * Keep it here for further use.
 * @hide
 */
public class HookLocationFaker implements IXposedHookLoadPackage {
    private Object mNianticLocationManager;
    private LocationHolder mLocationHolder;

    private static final String POKEMON_GO = "com.nianticlabs.pokemongo";

    public HookLocationFaker() {
        mLocationHolder = LocationHolder.getInstance(null);

    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        dumpLocation(lpparam);
//        hook_createProviders(lpparam);
//        hook_LocationManager(lpparam);
//        hook_gpsStatusUpdate(lpparam);
//        hook_ContentProvider_getInt(lpparam);
    }

    private void hook_ContentProvider_getInt(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(POKEMON_GO)) return;
        XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getInt",
                ContentResolver.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String name = ((String) param.args[1]);
                        if (name.equals(Settings.Secure.ALLOW_MOCK_LOCATION)) {
                            XposedBridge.log("Bingo, find its calling!");
                            param.setResult(0);
                        }
                    }
                });
    }

    private void dumpLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("com.nianticlabs.pokemongo")) {
            XposedHelpers.findAndHookMethod(
                    "com.nianticlabs.nia.location.NianticLocationManager", /* class name*/
                    lpparam.classLoader,
                    "locationUpdate", /* method name */
                    Location.class, /* params list */
                    int[].class,
                    new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Location location = ((Location) param.args[0]);
                    int[] status = ((int[]) param.args[1]);
                    if (location != null) {
                        XposedBridge.log(location.toString());
                        if (location.getExtras() != null) {
                            XposedBridge.log(location.getExtras().toString());
                        }
                    } else {
                        XposedBridge.log("location == null");
                    }
                    XposedBridge.log("status = " + Arrays.toString(status));
                }
            });

//            XposedHelpers.findAndHookMethod(
//                    "com.nianticlabs.nia.location.NianticLocationManager",
//                    lpparam.classLoader,
//                    "gpsStatusUpdate",
//                    int.class,
//                    GpsSatellite[].class,
//                    new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    super.beforeHookedMethod(param);
//                    int timeToFix = ((int) param.args[0]);
//                    GpsSatellite[] satellites = ((GpsSatellite[]) param.args[1]);
//                    XposedBridge.log("timeToFix = " + timeToFix);
//                    for (GpsSatellite s : satellites) {
//                        XposedBridge.log(satelliteToString(s));
//                    }
//                }
//            });
        }
    }

    private void hook_createProviders(XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException {
        if (lpparam.packageName.equals("com.nianticlabs.pokemongo")) {
            XposedHelpers.findAndHookMethod(
                    "com.nianticlabs.nia.location.NianticLocationManager", /* class name*/
                    lpparam.classLoader,
                    "createProviders", /* method name */
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("after createProviders ");
                            List list = ((List) XposedHelpers.getObjectField(param.thisObject, "providers"));
                            if (list != null) {
                                XposedBridge.log("provider list = " +  list.size());
                                list.remove(0);
                            }
                            if (param.thisObject == null) {
                                XposedBridge.log("NianticLocatoinManager == null");
                            } else {
                                mNianticLocationManager = param.thisObject;
                            }
                        }
                    }
            );
        }
    }

    private void hook_gpsStatusUpdate(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.nianticlabs.pokemongo")) return;

        XposedHelpers.findAndHookMethod(
                    "com.nianticlabs.nia.location.NianticLocationManager",
                    lpparam.classLoader,
                    "gpsStatusUpdate",
                    int.class,
                    GpsSatellite[].class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
//                            int timeToFix = ((int) param.args[0]);
//                            GpsSatellite[] satellites = ((GpsSatellite[]) param.args[1]);
//                            for (GpsSatellite s: satellites) {
//                                XposedBridge.log(satelliteToString(s));
//                            }
                            Location locationGps = mLocationHolder.pollLocation("gps");
                            Location locationNetwork = mLocationHolder.pollLocation("network");
                            call_updateLocation(locationGps);
                            call_updateLocation(locationNetwork);

//                            List<GpsSatellite> fakeSatelliteList = FakeGpsSatellite.fetchGpsSatellites();
//                            GpsSatellite[] fakeSatellites = new GpsSatellite[fakeSatelliteList.size()];
//                            for (int i = 0; i < fakeSatelliteList.size(); i++) {
//                                fakeSatellites[i] = fakeSatelliteList.get(i);
//                            }
//                            param.args[1] = fakeSatellites;
                            return null;
                        }
                    }
        );
    }

    private static String satelliteToString(GpsSatellite s) {
        StringBuilder sb = new StringBuilder();
        if (s != null) {
            sb.append("GpsSatellite[" +
                    "usedInFix = " + s.usedInFix() + ", " +
                    "hasAlmanac = " + s.hasAlmanac() + ", " +
                    "hasEphemeris = " + s.hasEphemeris() + ", " +
                    "prn = " + s.getPrn() + ", " +
                    "snr = " + s.getSnr() + ", " +
                    "elevation = " + s.getElevation() + ", " +
                    "azimuth = " + s.getAzimuth() + "]");
        } else {
            sb.append("GpsSatellite[null]");
        }
        return sb.toString();
    }

    private void hook_LocationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName.equals("com.nianticlabs.pokemongo")) {
            XposedHelpers.findAndHookMethod(
                    "android.location.LocationManager",
                    lpparam.classLoader,
                    "requestLocationUpdates",
                    String.class, long.class, float.class, LocationListener.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args.length == 4 && (param.args[0] instanceof String)) {

                                LocationListener ll = (LocationListener)param.args[3];

                                Class<?> clazz = LocationListener.class;
                                Method m = null;
                                for (Method method : clazz.getDeclaredMethods()) {
                                    if (method.getName().equals("onLocationChanged")) {
                                        m = method;
                                        break;
                                    }
                                }

                                try {
                                    if (m != null) {
                                        Location l = mLocationHolder.pollLocation("gps");
                                        Object[] args = new Object[1];

                                        args[0] = l;

                                        //invoke onLocationChanged directly to pass location infomation
                                        m.invoke(ll, args);

                                        XposedBridge.log("fake location: " + l.toString());
                                    }
                                } catch (Exception e) {
                                    XposedBridge.log(e);
                                }
                            }
                        }
                    }
            );
        }
    }


    private void call_updateLocation(Location location) {
        XposedBridge.log("HookLocationFaker = " + HookLocationFaker.this.toString());
        int[] statusMap = new int[]{3,3,4};
        if (mNianticLocationManager == null) {
            XposedBridge.log("pokemon param == null");
        } else {
            XposedBridge.log("posting: " + location.toString());
            XposedHelpers.callMethod(mNianticLocationManager,
                    "locationUpdate",
                    location, statusMap);        /* object list */
        }

    }

    private static class FakeGpsSatellite {
        boolean mValid;
        boolean mHasEphemeris;
        boolean mHasAlmanac;
        boolean mUsedInFix;
        int mPrn;
        float mSnr;
        float mElevation;
        float mAzimuth;

        static FakeGpsSatellite[] staticList = new FakeGpsSatellite[]{
                new FakeGpsSatellite(true, true, true, false, 5, 0.0f, 5.0f, 112.0f),
                new FakeGpsSatellite(true, true, true, true, 13, 12.4f, 23.0f, 53.0f),
                new FakeGpsSatellite(true, true, true, true, 14, 16.9f, 6.0f, 247.0f),
                new FakeGpsSatellite(true, true, true, true, 15, 22.1f, 58.0f, 45.0f),
                new FakeGpsSatellite(true, true, true, false, 18, 0f, 52.0f, 309.0f),
                new FakeGpsSatellite(true, true, true, true, 20, 21.5f, 54.0f, 105.0f),
                new FakeGpsSatellite(true, true, true, true, 21, 24.1f, 56.0f, 251.0f),
                new FakeGpsSatellite(true, true, true, false, 22, 0f, 14.0f, 299.0f),
                new FakeGpsSatellite(true, true, true, true, 24, 25.9f, 57.0f, 157.0f),
                new FakeGpsSatellite(true, true, true, true, 27, 18.0f, 3.0f, 309.0f),
                new FakeGpsSatellite(true, true, true, true, 28, 18.2f, 3.0f, 42.0f),
                new FakeGpsSatellite(true, true, true, false, 41, 18.2f, 3.0f, 0.0f),
                new FakeGpsSatellite(true, true, true, false, 50, 29.2f, 0.0f, 0.0f),
                new FakeGpsSatellite(true, true, true, true, 67, 14.4f, 2.0f, 92.0f),
                new FakeGpsSatellite(true, true, true, true, 68, 21.2f, 45.0f, 60.0f),
                new FakeGpsSatellite(true, true, true, true, 69, 17.5f, 50.0f, 330.0f),
                new FakeGpsSatellite(true, true, true, true, 70, 22.4f, 7.0f, 291.0f),
                new FakeGpsSatellite(true, true, true, true, 77, 23.8f, 10.0f, 23.0f),
                new FakeGpsSatellite(true, true, true, true, 78, 18.0f, 47.0f, 70.0f),
                new FakeGpsSatellite(true, true, true, true, 79, 22.8f, 41.0f, 142.0f),
                new FakeGpsSatellite(true, true, true, true, 83, 20.2f, 9.0f, 212.0f),
                new FakeGpsSatellite(true, true, true, true, 84, 16.7f, 30.0f, 264.0f),
                new FakeGpsSatellite(true, true, true, true, 85, 12.1f, 20.0f, 317.0f),
        };

        static List<GpsSatellite> cacheGpsSatelliteList;

        public FakeGpsSatellite(boolean mValid, boolean mHasEphemeris, boolean mHasAlmanac, boolean mUsedInFix, int mPrn, float mSnr, float mElevation, float mAzimuth) {
            this.mValid = mValid;
            this.mHasEphemeris = mHasEphemeris;
            this.mHasAlmanac = mHasAlmanac;
            this.mUsedInFix = mUsedInFix;
            this.mPrn = mPrn;
            this.mSnr = mSnr;
            this.mElevation = mElevation;
            this.mAzimuth = mAzimuth;
        }

        public static List<GpsSatellite> fetchGpsSatellites() {
            if (cacheGpsSatelliteList == null) {
                cacheGpsSatelliteList = new ArrayList<>();
                for (FakeGpsSatellite fake: staticList) {
                    GpsSatellite real = ((GpsSatellite) XposedHelpers.newInstance(GpsSatellite.class, fake.mPrn));
                    XposedHelpers.setBooleanField(real, "mValid", fake.mValid);
                    XposedHelpers.setBooleanField(real, "mUsedInFix", fake.mUsedInFix);
                    XposedHelpers.setBooleanField(real, "mHasAlmanac", fake.mHasAlmanac);
                    XposedHelpers.setBooleanField(real, "mHasEphemeris", fake.mHasEphemeris);
                    XposedHelpers.setFloatField(real, "mSnr", fake.mSnr);
                    XposedHelpers.setFloatField(real, "mElevation", fake.mElevation);
                    XposedHelpers.setFloatField(real, "mAzimuth", fake.mAzimuth);
                    cacheGpsSatelliteList.add(real);
                }
            }
            return cacheGpsSatelliteList;

        }
    }

}
