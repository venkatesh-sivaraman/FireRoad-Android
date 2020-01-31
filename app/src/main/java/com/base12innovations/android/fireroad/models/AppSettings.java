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

    public static final String ALLOWS_RECOMMENDATIONS = "allows_recommendations";
    public static final String ALLOWS_RECOMMENDATIONS_BOOL = "allows_recommendations_bool";
    public static final int RECOMMENDATIONS_NO_VALUE = 0;
    public static final int RECOMMENDATIONS_DISALLOWED = 1;
    public static final int RECOMMENDATIONS_ALLOWED = 2;

    public static final String SHOWN_INTRO = "shown_intro";

    public static final String CLASS_YEAR = "class_year";
    public static final String CURRENT_SEMESTER = "current_semester";
    public static final String CLASS_YEAR_STRING = "class_year_str";

    public static final String RECOMMENDER_USERNAME = "recommender_username";
    public static final String RECOMMENDER_USER_ID = "recommender_user_id";

    public static final String HIDE_ALL_WARNINGS = "hide_all_warnings";
    public static final String ALLOW_COREQUISITES_TOGETHER = "allow_corequisites_together";

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

    private static final String VERSION_MESSAGE = "version_message";
    // Increment this counter and change versionUpdateMessage to show a new "What's New" dialog
    private static final int versionIndex = 3;
    private static final String versionUpdateMessage = "We made a new feature that allows you to substitute (a.k.a. petition) " +
            "requirements in your majors and minors. Check it out in the Requirements tab!";

    public static String getVersionUpdateMessage() {
        // If fresh install, don't show update message
        if (shared().getInt(ALLOWS_RECOMMENDATIONS, RECOMMENDATIONS_NO_VALUE) == RECOMMENDATIONS_NO_VALUE) {
            shared().edit().putInt(VERSION_MESSAGE, versionIndex).apply();
        }

        // If updated and versionIndex is now greater, show message
        if (shared().getInt(VERSION_MESSAGE, 0) < versionIndex) {
            shared().edit().putInt(VERSION_MESSAGE, versionIndex).apply();
            return versionUpdateMessage;
        }

        return null;
    }
}
