package com.example.smartwallet.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurityUtils {
    private static final String PREF_NAME = "secure_prefs";
    private static final String KEY_PIN = "app_pin";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    public static SharedPreferences getSecurePrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public static void setPin(Context context, String pin) {
        getSecurePrefs(context).edit().putString(KEY_PIN, pin).apply();
        getSecurePrefs(context).edit().putBoolean(KEY_LOCK_ENABLED, true).apply();
    }

    public static String getPin(Context context) {
        return getSecurePrefs(context).getString(KEY_PIN, null);
    }

    public static boolean isLockEnabled(Context context) {
        return getSecurePrefs(context).getBoolean(KEY_LOCK_ENABLED, false);
    }

    public static void setBiometricEnabled(Context context, boolean enabled) {
        getSecurePrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public static boolean isBiometricEnabled(Context context) {
        return getSecurePrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public static void disableLock(Context context) {
        getSecurePrefs(context).edit().putBoolean(KEY_LOCK_ENABLED, false).apply();
        getSecurePrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, false).apply();
    }
}
