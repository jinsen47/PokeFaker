package com.github.jinsen47.pokefaker.app.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * Created by Jinsen on 16/7/17.
 */
public class StateCheckUtil {

    public static boolean isLocationServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager am = ((ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningServiceInfo> tasks = am.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo info : tasks) {
           if (info.service.getClassName().equals(serviceClass.getName()))
            return true;
        }
        return false;
    }
}
