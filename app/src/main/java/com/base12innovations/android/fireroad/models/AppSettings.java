package com.base12innovations.android.fireroad.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppSettings {

    private static SharedPreferences _shared;

    public static SharedPreferences shared() {
        return _shared;
    }

    public static void initialize(Context context) {
        _shared = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String ALLOWS_RECOMMENDATIONS = "allows_recommendations";
    public static int RECOMMENDATIONS_NO_VALUE = 0;
    public static int RECOMMENDATIONS_DISALLOWED = 1;
    public static int RECOMMENDATIONS_ALLOWED = 2;

    public static String CURRENT_SEMESTER = "current_semester";

    public static String RECOMMENDER_USERNAME = "recommender_username";
    public static String RECOMMENDER_USER_ID = "recommender_user_id";
}
