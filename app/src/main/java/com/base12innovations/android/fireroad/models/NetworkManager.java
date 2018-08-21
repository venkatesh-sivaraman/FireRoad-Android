package com.base12innovations.android.fireroad.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.Rating;
import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class NetworkManager {

    private static NetworkManager _shared;
    private static String NETWORK_MANAGER_PREFS = "com.base12innovations.android.fireroad.networkManagerPreferences";
    private SharedPreferences preferences;
    private Retrofit retrofit;

    private NetworkManager() { }
    private Context context;
    private RequestQueue requestQueue;

    public static int JSON_ERROR = 1001;


    public static NetworkManager sharedInstance() {
        if (_shared == null) {
            _shared = new NetworkManager();
        }
        return _shared;
    }

    public void initialize(Context context) {
        this.context = context;
        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        requestQueue = Volley.newRequestQueue(context);
        preferences = context.getSharedPreferences(NETWORK_MANAGER_PREFS, Context.MODE_PRIVATE);
    }

    public static class Response<E> {
        public E result;
        public int errorCode;
        public String errorMessage;
        public boolean isUserFacing;

        private Response() { }

        public static <E> Response<E> error(int code, String message, boolean isUserFacing) {
            Response<E> r = new Response<>();
            r.result = null;
            r.errorCode = code;
            r.errorMessage = message;
            r.isUserFacing = isUserFacing;
            return r;
        }

        public static <E> Response<E> success(E result) {
            Response<E> r = new Response<>();
            r.result = result;
            return r;
        }
    }

    // URLs

    public static String CATALOG_BASE_URL = "https://venkats.scripts.mit.edu";
    public static String BASE_URL = "https://venkats.scripts.mit.edu/fireroad/";
    private static String recommenderLoginURL = BASE_URL + "login/";
    private static String recommenderSignupURL = BASE_URL + "signup/";

    // Log in

    interface LoginAPI {
        @GET("verify")
        Call<HashMap<String, Object>> verifyLogin(@Header("Authorization") String authorization);
    }

    public interface AuthenticationListener {
        void showAuthenticationView(String url, AsyncResponse<JSONObject> completion);
    }

    public AuthenticationListener authenticationListener;
    private boolean isLoggedIn = false;
    public boolean isLoggedIn() { return isLoggedIn; }

    private static String ACCESS_TOKEN_PREFS_KEY = "accessToken";
    private static String SHOWN_SIGNUP_PREFS_KEY = "hasShownSignup";

    private void setAccessToken(String token) {
        preferences.edit().putString(ACCESS_TOKEN_PREFS_KEY, token).apply();
    }

    private String getAccessToken() {
        return preferences.getString(ACCESS_TOKEN_PREFS_KEY, "");
    }

    private void setHasShownSignup(boolean hasShownSignup) {
        preferences.edit().putBoolean(SHOWN_SIGNUP_PREFS_KEY, hasShownSignup).apply();
    }

    private boolean hasShownSignup() {
        return preferences.getBoolean(SHOWN_SIGNUP_PREFS_KEY, false);
    }

    private String getAuthorizationString() {
        if (getAccessToken() == null)
            return null;
        return "Bearer " + getAccessToken();
    }

    private String loginURL(boolean signup) {
        String base = signup ? recommenderSignupURL : recommenderLoginURL;
        int semester = AppSettings.shared().getInt(AppSettings.CURRENT_SEMESTER, 0);
        if (semester != 0) {
            base += "?sem=" + Integer.toString(semester);
        }
        return base;
    }

    public interface AsyncResponse<T> {
        void success(T result);
        void failure();
    }

    public void loginIfNeeded(final AsyncResponse<Boolean> completion) {
        if (isLoggedIn) {
            completion.success(true);
            return;
        }
        int recsFlag = AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE);
        if (recsFlag == AppSettings.RECOMMENDATIONS_DISALLOWED) {
            completion.failure();
            return;
        }

        final AsyncResponse<JSONObject> authListener = new AsyncResponse<JSONObject>() {
            @Override
            public void success(JSONObject response) {
                boolean worked = extractAccessInfo(response);
                isLoggedIn = worked;
                AppSettings.shared().edit().putInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_ALLOWED).apply();
                setHasShownSignup(worked);
                completion.success(true);
            }

            @Override
            public void failure() {
                AppSettings.shared().edit().putInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_DISALLOWED).apply();
                completion.failure();
            }
        };

        if (getAccessToken() == null) {
            if (authenticationListener == null) {
                completion.failure();
                return;
            }
            authenticationListener.showAuthenticationView(loginURL(!hasShownSignup() || recsFlag == AppSettings.RECOMMENDATIONS_NO_VALUE), authListener);
            return;
        }

        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                LoginAPI loginAPI = retrofit.create(LoginAPI.class);
                Call<HashMap<String, Object>> req = loginAPI.verifyLogin(getAuthorizationString());
                try {
                    retrofit2.Response<HashMap<String, Object>> resp = req.execute();
                    if (resp.isSuccessful() && resp.body() != null) {
                        HashMap<String, Object> result = resp.body();
                        if (!result.containsKey("success") || !(Boolean)result.get("success")) {
                            completion.failure();
                        }
                        if (result.containsKey("current_semester")) {
                            Object semObj = result.get("current_semester");
                            int sem = 0;
                            if (semObj instanceof Integer)
                                sem = (Integer)semObj;
                            else if (semObj instanceof Double)
                                sem = (int)Math.round((Double)semObj);
                            if (sem != 0)
                                AppSettings.shared().edit().putInt(AppSettings.CURRENT_SEMESTER, sem).apply();
                        }
                        Log.d("NetworkManager", "Successfully logged in");
                        isLoggedIn = true;
                        completion.success(true);
                    } else {
                        Log.d("NetworkManager", "Received status code " + Integer.toString(resp.code()));
                        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                            @Override
                            public void perform() {
                                if (authenticationListener == null) {
                                    completion.failure();
                                    return;
                                }
                                authenticationListener.showAuthenticationView(loginURL(!hasShownSignup()), authListener);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    completion.failure();
                }
            }
        });
    }

    private boolean extractAccessInfo(JSONObject response) {
        try {
            if (!response.has("success") || !response.getBoolean("success"))
                return false;

            String token = response.getString("access_token");
            Log.d("NetworkManager", "Access token: " + token);
            setAccessToken(token);

            if (response.has("academic_id"))
                AppSettings.shared().edit().putString(AppSettings.RECOMMENDER_USERNAME, response.getString("academic_id")).apply();

            if (response.has("sub"))
                AppSettings.shared().edit().putString(AppSettings.RECOMMENDER_USER_ID, response.getString("sub")).apply();

            if (response.has("current_semester"))
                AppSettings.shared().edit().putInt(AppSettings.CURRENT_SEMESTER, response.getInt("current_semester")).apply();

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Course updater

    interface CourseUpdaterAPI {
        @GET("courseupdater/semesters")
        Call<List<HashMap<String, Object>>> getSemesters();

        @GET("courseupdater/check")
        Call<HashMap<String, Object>> getCurrentSemesterInfo(@Query("sem") String semester, @Query("v") int v, @Query("rv") int rv);
    }

    /*
    This function saves the current version to the preferences, and the completion block is passed
    a list of paths to update.
     */
    public Response<HashMap<String, Object>> determineDatabaseVersion(String semester, int version, int reqVersion) {
        CourseUpdaterAPI api = retrofit.create(CourseUpdaterAPI.class);

        Call<HashMap<String, Object>> req = api.getCurrentSemesterInfo(semester, version, reqVersion);
        try {
            retrofit2.Response<HashMap<String, Object>> resp = req.execute();
            if (resp.isSuccessful() && resp.body() != null) {
                HashMap<String, Object> result = resp.body();
                return Response.success(result);
            } else {
                return Response.error(resp.code(), null, false);
            }
        } catch (IOException e) {
            return Response.error(JSON_ERROR, null, false);
        }
    }

    public Response<String> determineCurrentSemester() {
        CourseUpdaterAPI api = retrofit.create(CourseUpdaterAPI.class);

        Call<List<HashMap<String, Object>>> req = api.getSemesters();
        try {
            retrofit2.Response<List<HashMap<String, Object>>> resp = req.execute();
            if (resp.isSuccessful() && resp.body() != null && resp.body().size() > 0) {
                return Response.success((String)resp.body().get(resp.body().size() - 1).get("sem"));
            } else {
                return Response.error(resp.code(), null, false);
            }
        } catch (IOException e) {
            return Response.error(JSON_ERROR, null, false);
        }
    }

    // Ratings

    interface RatingAPI {
        @POST("recommend/rate")
        Call<HashMap<String, Object>> submitRatings(@Header("Authorization") String auth, @Body List<Object> body);

        @GET("recommend/get")
        Call<Map<String, Object>> getRecommendations(@Header("Authorization") String auth);
    }

    private Map<String, Integer> userRatingsToSubmit;

    public void submitUserRatingsImmediately(Map<String, Integer> ratings) {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                ratings.size() == 0 || !isLoggedIn)
            return;

        RatingAPI api = retrofit.create(RatingAPI.class);

        List<Object> body = new ArrayList<>();
        for (String key : ratings.keySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("s", key);
            item.put("v", ratings.get(key));
            body.add(item);
        }

        Call<HashMap<String, Object>> req = api.submitRatings(getAuthorizationString(), body);
        req.enqueue(new Callback<HashMap<String, Object>>() {
            @Override
            public void onResponse(Call<HashMap<String, Object>> call, retrofit2.Response<HashMap<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HashMap<String, Object> val = response.body();
                    Log.d("NetworkManager", "Succeeded at rating submission: " + val.toString());
                }
            }

            @Override
            public void onFailure(Call<HashMap<String, Object>> call, Throwable t) {
                Log.d("NetworkManager","Failed to submit ratings");
            }
        });
    }

    public void submitUserRatings(Map<String, Integer> ratings) {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                ratings.size() == 0 || !isLoggedIn)
            return;

        if (userRatingsToSubmit == null) {
            userRatingsToSubmit = new HashMap<>();
        }
        userRatingsToSubmit.putAll(ratings);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (userRatingsToSubmit == null)
                    return;
                Map<String, Integer> ratingsToSubmit = userRatingsToSubmit;
                userRatingsToSubmit = null;
                submitUserRatingsImmediately(ratingsToSubmit);
            }
        }, 500);
    }

    public interface RecommendationsFetchCompletion {
        void completed(Map<String, Object> result);
        void error(int code);
    }

    public void fetchRecommendations(final RecommendationsFetchCompletion completion) {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                !isLoggedIn) {
            completion.error(0);
            return;
        }

        RatingAPI api = retrofit.create(RatingAPI.class);
        Call<Map<String, Object>> req = api.getRecommendations(getAuthorizationString());
        req.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> val = response.body();
                    completion.completed(val);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.d("NetworkManager","Failed to get recommendations");
                completion.error(0);
            }
        });
    }

    // Synced preferences

    interface SyncedPreferenceAPI {
        @GET("prefs/favorites")
        Call<HashMap<String, Object>> getFavorites(@Header("Authorization") String authorization);
        @POST("prefs/set_favorites")
        Call<HashMap<String, Object>> setFavorites(@Header("Authorization") String authorization, @Body ArrayList<String> subjectIDs);

        @GET("prefs/notes")
        Call<HashMap<String, Object>> getNotes(@Header("Authorization") String authorization);
        @POST("prefs/set_notes")
        Call<HashMap<String, Object>> setNotes(@Header("Authorization") String authorization, @Body HashMap<String, String> subjectIDs);

        @GET("prefs/progress_overrides")
        Call<HashMap<String, Object>> getProgressOverrides(@Header("Authorization") String authorization);
        @POST("prefs/set_progress_overrides")
        Call<HashMap<String, Object>> setProgressOverrides(@Header("Authorization") String authorization, @Body HashMap<String, Integer> overrides);
    }

    public Response<List<String>> getFavorites() {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                !isLoggedIn)
            return Response.error(0, null, false);

        SyncedPreferenceAPI api = retrofit.create(SyncedPreferenceAPI.class);
        Call<HashMap<String, Object>> req = api.getFavorites(getAuthorizationString());
        try {
            retrofit2.Response<HashMap<String, Object>> resp = req.execute();
            if (resp.isSuccessful() && resp.body() != null &&
                    (Boolean)resp.body().get("success")) {
                return Response.success((List<String>)resp.body().get("favorites"));
            } else {
                return Response.error(resp.code(), null, false);
            }
        } catch (IOException | ClassCastException e) {
            e.printStackTrace();
            return Response.error(JSON_ERROR, null, false);
        }
    }

    public void setFavorites(ArrayList<String> subjectIDs) {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                !isLoggedIn)
            return;

        SyncedPreferenceAPI api = retrofit.create(SyncedPreferenceAPI.class);
        Call<HashMap<String, Object>> req = api.setFavorites(getAuthorizationString(), subjectIDs);
        req.enqueue(new Callback<HashMap<String, Object>>() {
            @Override
            public void onResponse(Call<HashMap<String, Object>> call, retrofit2.Response<HashMap<String, Object>> response) {
                Log.d("NetworkManager", "Successfully set favorites");
            }

            @Override
            public void onFailure(Call<HashMap<String, Object>> call, Throwable t) {
                Log.d("NetworkManager", "Failed to set favorites");
            }
        });
    }

    public Response<Map<String, Object>> getNotes() {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                !isLoggedIn)
            return Response.error(0, null, false);

        SyncedPreferenceAPI api = retrofit.create(SyncedPreferenceAPI.class);
        Call<HashMap<String, Object>> req = api.getNotes(getAuthorizationString());
        try {
            retrofit2.Response<HashMap<String, Object>> resp = req.execute();
            if (resp.isSuccessful() && resp.body() != null &&
                    (Boolean)resp.body().get("success")) {
                return Response.success((Map<String, Object>)resp.body().get("notes"));
            } else {
                return Response.error(resp.code(), null, false);
            }
        } catch (IOException | ClassCastException e) {
            return Response.error(JSON_ERROR, null, false);
        }
    }

    public void setNotes(HashMap<String, String> notes) {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                !isLoggedIn)
            return;

        SyncedPreferenceAPI api = retrofit.create(SyncedPreferenceAPI.class);
        Call<HashMap<String, Object>> req = api.setNotes(getAuthorizationString(), notes);
        req.enqueue(new Callback<HashMap<String, Object>>() {
            @Override
            public void onResponse(Call<HashMap<String, Object>> call, retrofit2.Response<HashMap<String, Object>> response) {
                Log.d("NetworkManager", "Successfully set notes");
            }

            @Override
            public void onFailure(Call<HashMap<String, Object>> call, Throwable t) {
                Log.d("NetworkManager", "Failed to set notes");
            }
        });
    }

    public Response<Map<String, Object>> getProgressOverrides() {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                !isLoggedIn)
            return Response.error(0, null, false);

        SyncedPreferenceAPI api = retrofit.create(SyncedPreferenceAPI.class);
        Call<HashMap<String, Object>> req = api.getProgressOverrides(getAuthorizationString());
        try {
            retrofit2.Response<HashMap<String, Object>> resp = req.execute();
            if (resp.isSuccessful() && resp.body() != null &&
                    (Boolean)resp.body().get("success")) {
                return Response.success((Map<String, Object>)resp.body().get("progress_overrides"));
            } else {
                return Response.error(resp.code(), null, false);
            }
        } catch (IOException | ClassCastException e) {
            return Response.error(JSON_ERROR, null, false);
        }
    }

    public void setProgressOverrides(HashMap<String, Integer> overrides) {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                !isLoggedIn)
            return;

        SyncedPreferenceAPI api = retrofit.create(SyncedPreferenceAPI.class);
        Call<HashMap<String, Object>> req = api.setProgressOverrides(getAuthorizationString(), overrides);
        req.enqueue(new Callback<HashMap<String, Object>>() {
            @Override
            public void onResponse(Call<HashMap<String, Object>> call, retrofit2.Response<HashMap<String, Object>> response) {
                Log.d("NetworkManager", "Successfully set progress overrides");
            }

            @Override
            public void onFailure(Call<HashMap<String, Object>> call, Throwable t) {
                Log.d("NetworkManager", "Failed to set progress overrides");
            }
        });
    }
}
