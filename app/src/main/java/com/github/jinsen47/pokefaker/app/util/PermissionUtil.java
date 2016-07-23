package com.github.jinsen47.pokefaker.app.util;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

import com.github.jinsen47.pokefaker.R;
import com.github.jinsen47.pokefaker.app.Constants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Jinsen on 16/7/17.
 */
public class PermissionUtil {
    public static final int PERMISSION_REQUEST_PHONE = 0;
    public static final int PERMISSION_REQUEST_CAMERA = 1;
    public static final int PERMISSION_REQUEST_SYSTEM_ALERT_WINDOW = 2;
    public static final int PERMISSION_REQUEST_MOCK_LOCATION = 3;

    /**
     *
     * @param context
     * @return true is mock setting being requested, or false if permission allowed/granted
     */
    public static boolean checkAndRequestMockSetting(final Context context) {
        boolean isGranted = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AppOpsManager opsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                isGranted = (opsManager.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), Constants.PACKAGE_POKEFAKER) == AppOpsManager.MODE_ALLOWED);
            } else {
                int ret = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION);
                isGranted = (ret == 1);
            }
        } catch (Settings.SettingNotFoundException e) {
            isGranted = false;
        } finally {
            if (!isGranted) {
                new AlertDialog.Builder(context)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(context.getString(R.string.need_permission_message, context.getString(R.string.permission_display_mock_location)))
                        .setPositiveButton(R.string.jump_setting, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent();
                                i.setData(Uri.parse("package:" + Constants.PACKAGE_POKEFAKER));
                                PackageManager pm = context.getPackageManager();
                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                                    i.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                } else {
                                    i.setClassName("com.android.settings", "com.android.settings.DevelopmentSettings");
                                }
                                if (!pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                                    context.startActivity(i);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     *
     * @param activity
     * @return true is pop-up window permission requested, or false if permission allowed/granted
     */
    public static boolean checkAndRequestPopupWindowPermission(final Activity activity) {
        boolean isGranted = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isGranted = Settings.canDrawOverlays(activity);
            if (!isGranted) {
                final Intent i = new Intent();
                if (Build.IS_MIUI) {
                    i.setAction("miui.intent.action.APP_PERM_EDITOR");
                    i.putExtra("extra_pkgname", activity.getPackageName());
                } else {
                    i.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    i.setData(Uri.parse("package:" + Constants.PACKAGE_POKEFAKER));
                }
                new AlertDialog.Builder(activity)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(activity.getString(R.string.need_permission_message, activity.getString(R.string.permission_display_window)))
                        .setPositiveButton(R.string.jump_setting, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.startActivity(i);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            isGranted = !checkAndRequestPermission(activity, Manifest.permission.SYSTEM_ALERT_WINDOW, PERMISSION_REQUEST_SYSTEM_ALERT_WINDOW);
        } else {
            isGranted = false;
        }
        return !isGranted;
    }

    /**
     * @param activity
     * @param permission
     * @param requestCode
     * @return true if permission being requested, or false if permission allowed/granted
     */
    public static boolean checkAndRequestPermission(final Activity activity, String permission, int requestCode) {
        boolean isGranted = false;
        Permission per = Permission.findByName(permission);
        if (per == null) {
            // Means cant find this permission, take it as granted
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int p = activity.checkSelfPermission(permission);
            if (p != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{permission}, requestCode);
                isGranted = false;
            } else {
                isGranted = true;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppOpsManager appOpsManager = (AppOpsManager) activity.getSystemService(Context.APP_OPS_SERVICE);
            int result = checkOp(appOpsManager, per.opName);
            switch (result) {
                case AppOpsManager.MODE_ALLOWED:
                    isGranted = true;
                    break;
                default:
                    isGranted = false;
                    break;
        }
        }
        final Intent i = new Intent();
        if (Build.IS_MIUI) {
            i.setAction("miui.intent.action.APP_PERM_EDITOR");
            i.putExtra("extra_pkgname", activity.getPackageName());
        } else {
            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + Constants.PACKAGE_POKEFAKER));
        }
        if (!isGranted) {
            String message = activity.getString(R.string.need_permission_message, activity.getString(per.displayRes));
            new AlertDialog.Builder(activity)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.jump_setting, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.startActivity(i);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }
        return !isGranted;
    }

    private static int checkOp(AppOpsManager manager, String operation) {
        try {
            Class<?> clz = AppOpsManager.class;
            Class[] params = {int.class, int.class, String.class};
            Method method = clz.getMethod("checkOp", params);
            int op = clz.getField(operation).getInt(null);
            Object[] arguments = {op, android.os.Process.myUid(), Constants.PACKAGE_POKEFAKER};
            return (int) method.invoke(manager, arguments);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return AppOpsManager.MODE_IGNORED;
    }

    private enum Permission {
        // Permission for Android 5.0/5.1
        CAMERA(Manifest.permission.CAMERA, "OP_CAMERA", R.string.permission_display_camera),
        WINDOW(Manifest.permission.SYSTEM_ALERT_WINDOW, "OP_SYSTEM_ALERT_WINDOW", R.string.permission_display_window);

        public final String name;
        public final String opName;
        public final int displayRes;

        Permission(String name, String opName, int displayRes) {
            this.name = name;
            this.opName = opName;
            this.displayRes = displayRes;
        }

        public static Permission findByName(String permission) {
            for (Permission p : Permission.values()) {
                if (p.name.equals(permission)) {
                    return p;
                }
            }
            return null;
        }
    }
}
