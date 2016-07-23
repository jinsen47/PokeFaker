package com.github.jinsen47.pokefaker.app.util;

/**
 * Created by Jinsen on 16/7/23.
 */
public class Build extends android.os.Build {
    public static class VERSION_CODES extends android.os.Build.VERSION_CODES {
        /**
         * August 2014: MIUI 6
         */
        public static final int MIUI_6 = 4;

        /**
         * August 2015: MIUI 7
         */
        public static final int MIUI_7 = 5;
    }
    public static class VERSION extends android.os.Build.VERSION {
        /**
         * The user-visible SDK version of the framework; its possible
         * values are defined in {@link Build.VERSION_CODES}.
         */
        public static final int MIUI_INT = SystemProperties.getInt(
                "ro.miui.ui.version.code", 0);
    }

    public static boolean IS_MIUI = Build.VERSION.MIUI_INT >= Build.VERSION_CODES.MIUI_6;
}
