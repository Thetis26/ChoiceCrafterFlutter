package com.choicecrafter.studentapp.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * Utility helpers that determine whether the device advertises a vendor security patch level
 * that is compatible with hardware attestation / Remote Key Provisioning.
 */
public final class DeviceSecurityPatchInspector {

    private static final String TAG = "DeviceSecurityPatch";
    private static final Pattern PATCH_PATTERN = Pattern.compile("^(\\d{6}|\\d{8})$");
    private static final String DEFAULT_VALUE = "";

    private DeviceSecurityPatchInspector() {
        // Utility class.
    }

    /**
     * Returns {@code true} when the vendor security patch level is present and formatted as
     * either YYYYMM or YYYYMMDD. Remote Provisioning rejects any other value.
     */
    public static boolean hasValidVendorPatchLevel() {
        String vendorPatch = getVendorSecurityPatch();
        if (vendorPatch == null || vendorPatch.isEmpty()) {
            return false;
        }
        if (!PATCH_PATTERN.matcher(vendorPatch).matches()) {
            return false;
        }
        return true;
    }

    /**
     * Attempts to read the vendor security patch level from the system property. When the
     * property cannot be accessed we fall back to the public {@link Build#VERSION#SECURITY_PATCH}
     * value so callers can still log what the platform reports.
     */
    public static String getVendorSecurityPatch() {
        String vendorPatch = getSystemProperty("ro.vendor.build.security_patch");
        if (vendorPatch == null || vendorPatch.isEmpty()) {
            return Build.VERSION.SECURITY_PATCH != null ? Build.VERSION.SECURITY_PATCH : DEFAULT_VALUE;
        }
        return vendorPatch;
    }

    @SuppressLint("PrivateApi")
    private static String getSystemProperty(String key) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getMethod = systemProperties.getMethod("get", String.class, String.class);
            Object value = getMethod.invoke(null, key, DEFAULT_VALUE);
            return value != null ? value.toString() : DEFAULT_VALUE;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.w(TAG, "Unable to read system property " + key + ".", e);
            return DEFAULT_VALUE;
        }
    }
}
