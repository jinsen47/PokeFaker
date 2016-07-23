package com.github.jinsen47.pokefaker;

import android.content.ContentResolver;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Jinsen on 16/7/16.
 * Mock by add mock providers
 */
public class MockProviderFaker implements IXposedHookLoadPackage {

    private static final Set<String> mPackageSet = new HashSet<String>() {{
       add("com.nianticlabs.pokemongo");
    }};
    private static final String TAG = MockProviderFaker.class.getSimpleName();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        preventGetMockLocationSetting(lpparam);
        preventCheckFromMockProvider(lpparam);
//        dumpLocation(lpparam);
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
        }
    }

    private void preventGetMockLocationSetting(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!mPackageSet.contains(lpparam.packageName)) return;
        // hook Settings.Secure.getInt(ContentResolver, String)
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
                            Log.d(TAG, "Bingo, find its calling getInt");
                            param.setResult(0);
                        }
                    }
                });
        // hook Settings.Secure.getString(ContentResolver, String)
        XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getString",
                ContentResolver.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String name = ((String) param.args[1]);
                        if (name.equals(Settings.Secure.ALLOW_MOCK_LOCATION)) {
                            Log.d(TAG, "Bingo, find its calling getString");
                            param.setResult("0");
                        }
                    }
                });
        // hook Settings.Secure.getStringForUser(ContentResolver, String, int)
        if (Build.VERSION.SDK_INT >= 17) {
            XposedHelpers.findAndHookMethod(
                    Settings.Secure.class,
                    "getStringForUser",
                    ContentResolver.class,
                    String.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String name = ((String) param.args[1]);
                            if (name.equals(Settings.Secure.ALLOW_MOCK_LOCATION)) {
                                Log.d(TAG, "Bingo, find its calling getStringForUser");
                                param.setResult("0");
                            }
                        }
                    });
        }
    }

    private void preventCheckFromMockProvider(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (Build.VERSION.SDK_INT >= 18) {
            // hook Location.isFromMockProvider()
            XposedHelpers.findAndHookMethod(
                    Location.class,
                    "isFromMockProvider",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedHelpers.setBooleanField(param.thisObject, "mIsFromMockProvider", false);
                            Log.d(TAG, param.thisObject.getClass().getName() + "is calling from mock provider");
                            param.setResult(false);
                        }
            });
        }
    }
}
