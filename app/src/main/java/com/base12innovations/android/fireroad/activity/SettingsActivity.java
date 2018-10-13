package com.base12innovations.android.fireroad.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.doc.NetworkManager;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */

public class SettingsActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(R.id.pref_content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        private static String LOGOUT_KEY = "logout";
        private static String DATABASE_UPDATE_KEY = "database_update";

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            AppSettings.shared().registerOnSharedPreferenceChangeListener(this);
            Preference logoutItem = getPreferenceScreen().findPreference(LOGOUT_KEY);
            logoutItem.setEnabled(NetworkManager.sharedInstance().isLoggedIn());
            logoutItem.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    NetworkManager.sharedInstance().logout();
                    getActivity().finish();

                    setPreferenceScreen(null);
                    addPreferencesFromResource(R.xml.preferences);
                    getPreferenceScreen().findPreference(LOGOUT_KEY).setEnabled(NetworkManager.sharedInstance().isLoggedIn());
                    return true;
                }
            });
            getPreferenceScreen().findPreference(DATABASE_UPDATE_KEY).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CourseManager.sharedInstance().setNeedsDatabaseUpdate();
                    getActivity().finish();
                    return true;
                }
            });
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            AppSettings.shared().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(AppSettings.ALLOWS_RECOMMENDATIONS_BOOL)) {
                boolean newFlag = AppSettings.shared().getBoolean(AppSettings.ALLOWS_RECOMMENDATIONS_BOOL, false);
                AppSettings.setAllowsRecommendationsFromBool(newFlag);
                if (newFlag) {
                    getActivity().finish();
                } else {
                    NetworkManager.sharedInstance().logout();
                    getPreferenceScreen().findPreference(LOGOUT_KEY).setEnabled(NetworkManager.sharedInstance().isLoggedIn());
                }
            } else if (key.equals(AppSettings.CLASS_YEAR_STRING)) {
                String classYear = AppSettings.shared().getString(AppSettings.CLASS_YEAR_STRING, "1");
                //Log.d("Settings", classYear);
                CourseManager.sharedInstance().updateCurrentSemester(classYear);
            }
        }
    }
}