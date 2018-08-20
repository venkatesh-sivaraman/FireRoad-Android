package com.base12innovations.android.fireroad.models;

import android.content.Context;
import android.content.SharedPreferences;
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
    private static String databaseSemestersURL = BASE_URL + "courseupdater/semesters";
    private static String databaseVersionURL = BASE_URL + "courseupdater/check";
    private static String recommenderLoginURL = BASE_URL + "login/";
    private static String recommenderSignupURL = BASE_URL + "signup/";

    interface LoginAPI {
        @GET("verify")
        Call<HashMap<String, Object>> verifyLogin(@Header("Authorization") String authorization);
    }

    // Log in

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

    private JsonObjectRequest authorizedObjectRequest(int method, String url, JSONObject body, RequestFuture<JSONObject> future) {
        JsonObjectRequest request = new JsonObjectRequest(method, url, body, future, future) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> myMap = new HashMap<>();
                String auth = getAuthorizationString();
                if (auth != null) {
                    myMap.put("Authorization", auth);
                }
                return myMap;
            }
        };
        return request;
    }

    private JsonArrayRequest authorizedArrayRequest(int method, String url, JSONArray body, RequestFuture<JSONArray> future) {
        JsonArrayRequest request = new JsonArrayRequest(method, url, body, future, future) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> myMap = new HashMap<>();
                String auth = getAuthorizationString();
                if (auth != null) {
                    myMap.put("Authorization", auth);
                }
                return myMap;
            }
        };
        return request;
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

    /*
    This function saves the current version to the preferences, and the completion block is passed
    a list of paths to update.
     */
    public Response<JSONObject> determineDatabaseVersion(String semester, int version, int reqVersion) {
        String url = databaseVersionURL + String.format(Locale.US, "?sem=%s&v=%d&rv=%d", Uri.encode(semester), version, reqVersion);

        RequestFuture<JSONObject> requestFuture=RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
                url,new JSONObject(),requestFuture,requestFuture);

        requestQueue.add(request);
        try {
            JSONObject object = requestFuture.get(10, TimeUnit.SECONDS);
            return Response.success(object);
        } catch (InterruptedException | ExecutionException e ) {
            if (e.getCause() instanceof VolleyError) {
                //grab the volley error from the throwable
                VolleyError volleyError = (VolleyError)e.getCause();
                NetworkResponse resp = volleyError.networkResponse;
                return Response.error(resp.statusCode, null, false);
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return Response.error(0, null, false);
    }

    public Response<String> determineCurrentSemester() {
        String url = databaseSemestersURL;
        RequestFuture<JSONArray> requestFuture=RequestFuture.newFuture();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET,
                url,new JSONArray(),requestFuture,requestFuture);

        requestQueue.add(request);
        try {
            JSONArray object = requestFuture.get(10, TimeUnit.SECONDS);
            JSONObject element = object.getJSONObject(object.length() - 1);
            String currentSemester = element.getString("sem");

            return Response.success(currentSemester);
        } catch (JSONException e) {
            return Response.error(JSON_ERROR, null, false);
        } catch (InterruptedException | ExecutionException e ) {
            if (e.getCause() instanceof VolleyError) {
                //grab the volley error from the throwable
                VolleyError volleyError = (VolleyError)e.getCause();
                NetworkResponse resp = volleyError.networkResponse;
                return Response.error(resp.statusCode, null, false);
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return Response.error(0, null, false);
    }

    // Ratings

    interface RatingAPI {
        @POST("recommend/rate")
        Call<HashMap<String, Object>> submitRatings(@Header("Authorization") String auth, @Body List<Object> body);
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
                HashMap<String, Object> val = response.body();
                Log.d("NetworkManager", "Succeeded at rating submission: " + val.toString());
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
    /*


    func submitUserRatingsImmediately(ratings: [String: Int], completion: ((Bool) -> Void)? = nil, tryOnce: Bool = false) {
        guard AppSettings.shared.allowsRecommendations == true, ratings.count > 0,
                let url = URL(string: CourseManager.recommenderSubmitURL) else {
            return
        }

        let parameters: [[String: Any]] = ratings.map {
            ["s": $0.key,
                    "v": $0.value]
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: parameters)
        } catch {
            print(error.localizedDescription)
        }

        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        applyBasicAuthentication(to: &request)

        loginAndSendDataTask(with: request, errorHandler: {
            completion?(false)
        }, successHandler: { _ in
            completion?(true)
        })
    }

    func submitUserRatings(ratings: [String: Int], completion: ((Bool) -> Void)? = nil, tryOnce: Bool = false) {
        guard AppSettings.shared.allowsRecommendations == true, ratings.count > 0 else {
            return
        }
        if userRatingsToSubmit != nil {
            userRatingsToSubmit?.merge(ratings, uniquingKeysWith: { $1 })
        } else {
            userRatingsToSubmit = ratings
            DispatchQueue.global().asyncAfter(deadline: .now() + 0.5, execute: {
                guard let toSubmit = self.userRatingsToSubmit else {
                    return
                }
                self.userRatingsToSubmit = nil
                self.submitUserRatingsImmediately(ratings: toSubmit, completion: completion, tryOnce: tryOnce)
            })
        }
    }*/

}
