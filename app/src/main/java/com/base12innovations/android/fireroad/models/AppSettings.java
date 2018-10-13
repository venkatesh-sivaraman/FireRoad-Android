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
    public static String ALLOWS_RECOMMENDATIONS_BOOL = "allows_recommendations_bool";
    public static int RECOMMENDATIONS_NO_VALUE = 0;
    public static int RECOMMENDATIONS_DISALLOWED = 1;
    public static int RECOMMENDATIONS_ALLOWED = 2;

    public static String SHOWN_INTRO = "shown_intro";

    public static String CLASS_YEAR = "class_year";
    public static String CURRENT_SEMESTER = "current_semester";
    public static String CLASS_YEAR_STRING = "class_year_str";

    public static String RECOMMENDER_USERNAME = "recommender_username";
    public static String RECOMMENDER_USER_ID = "recommender_user_id";

    public static String HIDE_ALL_WARNINGS = "hide_all_warnings";
    public static String ALLOW_COREQUISITES_TOGETHER = "allow_corequisites_together";

    public static void setCurrentSemester(int semester) {
        SharedPreferences.Editor editor = AppSettings.shared().edit();
        int classYear = ((semester - 1) / 3) + 1;
        editor.putString(CLASS_YEAR_STRING, Integer.toString(classYear));
        editor.putInt(CURRENT_SEMESTER, semester);
        editor.apply();
    }

    public static void setAllowsRecommendations(int allowsRecommendations) {
        SharedPreferences.Editor editor = AppSettings.shared().edit();
        editor.putBoolean(ALLOWS_RECOMMENDATIONS_BOOL,allowsRecommendations == RECOMMENDATIONS_ALLOWED);
        editor.putInt(ALLOWS_RECOMMENDATIONS, allowsRecommendations);
        editor.apply();
    }

    public static void setAllowsRecommendationsFromBool(boolean allowsRecommendations) {
        SharedPreferences.Editor editor = AppSettings.shared().edit();
        editor.putInt(ALLOWS_RECOMMENDATIONS, allowsRecommendations ? RECOMMENDATIONS_ALLOWED : RECOMMENDATIONS_DISALLOWED);
        editor.apply();
    }
}
