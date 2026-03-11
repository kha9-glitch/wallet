package com.example.smartwallet.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CurrencyUtils {
    private static final String PREF_NAME = "currency_prefs";
    private static final String KEY_CURRENCY_SYMBOL = "currency_symbol";
    private static final String DEFAULT_SYMBOL = "₹";

    public static String getCurrencySymbol(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENCY_SYMBOL, DEFAULT_SYMBOL);
    }

    public static void setCurrencySymbol(Context context, String symbol) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply();
    }
}
