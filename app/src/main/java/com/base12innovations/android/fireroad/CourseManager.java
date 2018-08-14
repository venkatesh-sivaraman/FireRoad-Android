package com.base12innovations.android.fireroad;

import android.app.DownloadManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CourseManager {

    private static CourseManager _shared;

    // Preferences
    private static String COURSE_DATABASE_PREFERENCES = "com.base12innovations.android.fireroad.courseDatabasePreferences";
    private SharedPreferences dbPreferences;
    private static String prefsDatabaseVersionKey = "databaseVersionKey";
    private static String prefsDatabaseSemesterKey = "databaseSemesterKey";
    private RequestQueue requestQueue;
    private static int JSON_ERROR = 1001;

    // During debugging, use to force a database update
    private boolean forceUpdate = false;

    // Stored until a successful database update
    private String newSemester;
    private int newDatabaseVersion;

    private boolean _isLoading = false;
    public boolean isLoading() { return _isLoading; }
    private float loadingProgress = 0.0f;
    private boolean _isLoaded = false;
    public boolean isLoaded() { return _isLoaded; }
    public float getLoadingProgress() { return loadingProgress; }
    private List<Callable<Void>> loadingCompletionHandlers;

    private static final String DATABASE_NAME = "course_db";
    public CourseDatabase courseDatabase;
    private CourseManager() { }

    public static CourseManager sharedInstance() {
        if (_shared == null) {
            _shared = new CourseManager();
        }
        return _shared;
    }

    public void initializeDatabase(Context context) {
        if (courseDatabase != null) {
            Log.d("CourseManager", "Already initialized this database!");
            return;
        }
        courseDatabase = Room.databaseBuilder(context,
                CourseDatabase.class, DATABASE_NAME).fallbackToDestructiveMigration().build();

        dbPreferences = context.getSharedPreferences(COURSE_DATABASE_PREFERENCES, Context.MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(context);

        CourseSearchEngine.sharedInstance().initialize(context);
    }

    public interface LoadCoursesListener {
        void completion();
        void error();
        void needsFullLoad();
    }

    public void loadCourses(final LoadCoursesListener listener) {
        if (_isLoading)
            return;
        _isLoading = true;
        loadingCompletionHandlers = new ArrayList<>();
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                determineURLsToUpdate(new URLsToUpdateCompletion() {
                    @Override
                    public void completed(final List<URL> urls) {
                        TaskDispatcher.perform(new TaskDispatcher.Task<Void>() {
                            @Override
                            public Void perform() {
                                Log.d("CourseManager", "Updating " + Integer.toString(urls.size()) + " URLs");
                                if (urls.size() > 0) {
                                    listener.needsFullLoad();
                                    courseDatabase.daoAccess().clearCourses();
                                    loadingProgress = 0.0f;

                                    ExecutorService exec = Executors.newFixedThreadPool(3);
                                    for (int i = 0; i < urls.size(); i++) {
                                        final int index = i;
                                        exec.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                loadingProgress += 1.0f / (float) urls.size();
                                                loadCoursesFromURL(urls.get(index));
                                            }
                                        });
                                    }
                                    exec.shutdown();
                                    try {
                                        exec.awaitTermination(15, TimeUnit.MINUTES);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        Log.e("CourseManagerUpdate", e.getMessage());
                                    }
                                }

                                SharedPreferences.Editor editor = dbPreferences.edit();
                                if (newDatabaseVersion != 0)
                                    editor.putInt(prefsDatabaseVersionKey, newDatabaseVersion);
                                if (newSemester != null && newSemester.length() > 0)
                                    editor.putString(prefsDatabaseSemesterKey, newSemester);
                                editor.apply();

                                return null;
                            }
                        }, new TaskDispatcher.CompletionBlock<Void>() {
                            @Override
                            public void completed(Void arg) {
                                    _isLoading = false;
                                    _isLoaded = true;
                                    listener.completion();
                                    for (Callable<Void> comp : loadingCompletionHandlers) {
                                        try {
                                            comp.call();
                                        } catch (Exception e) { }
                                    }
                                    loadingCompletionHandlers = null;
                            }
                        });
                    }

                    @Override
                    public void error(int statusCode) {
                        _isLoading = false;
                        listener.error();
                    }
                });

                /*if (courseDatabase.daoAccess().getNumberOfCourses() == 0) {
                    String[] subjectIDs = new String[] {
                            "6.003", "18.03", "7.013", "6.046"
                    };
                    String[] subjectTitles = new String[] {
                            "Signals and Systems", "Differential Equations", "Introductory Biology", "Advanced Algorithms"
                    };
                    for (int i = 0; i < subjectIDs.length; i++) {
                        Course course = new Course();
                        course.setSubjectID(subjectIDs[i]);
                        course.setSubjectTitle(subjectTitles[i]);
                        courseDatabase.daoAccess().insertCourse(course);
                    }
                } else {
                    Log.d("CourseManager", "Didn't need to load courses");
                }

                Log.d("CourseManager", "Has " + Integer.toString(courseDatabase.daoAccess().getNumberOfCourses()) + " courses");
                return null;*/
            }
        });
    }

    public void waitForLoad(final Callable<Void> completionHandler) {
        if (_isLoaded) {
            try {
                completionHandler.call();
            } catch (Exception e) { }
            return;
        }
        if (loadingCompletionHandlers != null) {
            loadingCompletionHandlers.add(completionHandler);
        }
    }

    // Internet

    private interface JSONRequestCompletion <T> {
        void completed(T response);
        void error(int statusCode);
    }

    private static String databaseSemestersURL = "https://venkats.scripts.mit.edu/fireroad_dev/courseupdater/semesters";
    private static String databaseVersionURL = "https://venkats.scripts.mit.edu/fireroad_dev/courseupdater/check";
    private static String catalogDownloadURL = "https://venkats.scripts.mit.edu/catalogs/";

    private void determineCurrentSemester(final JSONRequestCompletion<String> completion) {
        String url = databaseSemestersURL;
        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            JSONObject element = response.getJSONObject(response.length() - 1);
                            String currentSemester = element.getString("sem");

                            newSemester = currentSemester;
                            completion.completed(currentSemester);

                        } catch (JSONException e) {
                            completion.error(JSON_ERROR);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse == null) {
                            error.printStackTrace();
                            completion.error(500);
                        } else {
                            completion.error(networkResponse.statusCode);
                        }
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    /*
    This function saves the current version to the preferences, and the completion block is passed
    a list of paths to update.
     */
    private void determineDatabaseVersion(String semester, final JSONRequestCompletion<List<String>> completion) {
        // We always use version 0, because this implementation needs to rebuild the entire database every time
        // there is an update.
        int version = 0; //dbPreferences.getInt(prefsDatabaseVersionKey, 0);
        String url = databaseVersionURL + String.format(Locale.US, "?sem=%s&v=%d", Uri.encode(semester), version);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int currentVersion = response.getInt("v");
                            JSONArray delta = response.getJSONArray("delta");
                            List<String> deltaList = new ArrayList<>();
                            for (int i = 0; i < delta.length(); i++) {
                                String item = delta.getString(i);
                                deltaList.add(item);
                            }

                            newDatabaseVersion = currentVersion;
                            completion.completed(deltaList);
                        } catch (JSONException e) {
                            completion.error(JSON_ERROR);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse networkResponse = error.networkResponse;
                        completion.error(networkResponse.statusCode);
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private interface URLsToUpdateCompletion {
        void completed(List<URL> urls);
        void error(int statusCode);
    }

    private void determineURLsToUpdate(final URLsToUpdateCompletion completion) {
        determineCurrentSemester(new JSONRequestCompletion<String>() {
            @Override
            public void completed(String semester) {
                determineDatabaseVersion(semester.replace("-", ","), new JSONRequestCompletion<List<String>>() {
                    @Override
                    public void completed(List<String> response) {
                        int currentDBVersion = forceUpdate ? 0 : dbPreferences.getInt(prefsDatabaseVersionKey, 0);
                        if (newDatabaseVersion != currentDBVersion) {
                            List<URL> urls = new ArrayList<>();
                            for (String path : response) {
                                try {
                                    urls.add(new URL(catalogDownloadURL + path));
                                } catch (MalformedURLException e) {
                                    Log.e("CourseManager", "Malformed URL, " + catalogDownloadURL + path);
                                }
                            }
                            completion.completed(urls);
                        } else {
                            completion.completed(new ArrayList<URL>());
                        }
                    }

                    @Override
                    public void error(int statusCode) {
                        Log.e("CourseManager", String.format(Locale.US, "Error %d determining version", statusCode));
                        completion.error(statusCode);
                    }
                });
            }

            @Override
            public void error(int statusCode) {
                completion.completed(new ArrayList<URL>());
                //Log.e("CourseManager", String.format(Locale.US, "Error %d determining semester", statusCode));
                //completion.error(statusCode);
            }
        });
    }

    // Loading courses from URL

    private void loadCourseFromLine(String line, String[] header) {
        // Thanks to https://stackoverflow.com/questions/15738918/splitting-a-csv-file-with-quotes-as-text-delimiter-using-string-split
        String[] comps = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        Course course = new Course();
        for (int i = 0; i < comps.length; i++) {
            if (i >= header.length || comps[i].length() == 0) {
                continue;
            }
            String component = comps[i];
            if (component.charAt(0) == '"' && component.charAt(component.length() - 1) == '"')
                component = component.substring(1, component.length() - 1);
            switch (header[i]) {
                case "Subject Id":
                    course.setSubjectID(component);
                    break;
                case "Subject Title":
                    course.subjectTitle = component;
                    break;
                case "Subject Description":
                    course.subjectDescription = component;
                    break;
                case "Total Units":
                    course.totalUnits = Integer.parseInt(component);
                    break;
                case "Subject Level":
                    course.subjectLevel = component;
                    break;
                case "Equivalent Subjects":
                    course.setEquivalentSubjects(component.replace(" ", ""));
                    break;
                case "Joint Subjects":
                    course.setJointSubjects(component.replace(" ", ""));
                    break;
                case "Meets With Subjects":
                    course.setMeetsWithSubjects(component.replace(" ", ""));
                    break;
                /*case "Prerequisites":
                    course.prerequisites = component;
                    break;
                case "Corequisites":
                    course.corequisites = component;
                    break;
                case "Gir Attribute":
                    course.girAttribute = component;
                    break;
                case "Comm Req Attribute":
                    course.communicationRequirement = component;
                    break;
                case "Hass Attribute":
                    course.hassAttribute = component;
                    break;*/
                case "Grade Rule":
                    course.gradeRule = component;
                    break;
                case "Grade Type":
                    course.gradeType = component;
                    break;
                case "Instructors":
                    course.setInstructors(component.replace("\\n", "\n"));
                    break;
                case "Is Offered Fall Term":
                    course.isOfferedFall = course.parseBoolean(component);
                    break;
                case "Is Offered Iap":
                    course.isOfferedIAP = course.parseBoolean(component);
                    break;
                case "Is Offered Spring Term":
                    course.isOfferedSpring = course.parseBoolean(component);
                    break;
                case "Is Offered Summer Term":
                    course.isOfferedSummer = course.parseBoolean(component);
                    break;
                case "Is Offered This Year":
                    course.setOfferedThisYear(course.parseBoolean(component));
                    break;
                case "Is Variable Units":
                    course.variableUnits = course.parseBoolean(component);
                    break;
                case "Lab Units":
                    course.labUnits = Integer.parseInt(component);
                    break;
                case "Lecture Units":
                    course.lectureUnits = Integer.parseInt(component);
                    break;
                case "Design Units":
                    course.designUnits = Integer.parseInt(component);
                    break;
                case "Preparation Units":
                    course.preparationUnits = Integer.parseInt(component);
                    break;
                case "PDF Option":
                    course.pdfOption = course.parseBoolean(component);
                    break;
                case "Has Final":
                    course.hasFinal = course.parseBoolean(component);
                    break;
                case "Not Offered Year":
                    course.setNotOfferedYear(component);
                    break;
                case "Quarter Information":
                    course.setQuarterInformation(component);
                    break;
                case "Enrollment Number":
                case "Enrollment":
                    course.enrollmentNumber = (int)Math.round(Double.parseDouble(component));
                    break;
                /*case "Related Subjects":
                    course.relatedSubjects = component;
                    break;
                case "Schedule":
                    course.schedule = component;
                    break;*/
                case "URL":
                    course.url = component;
                    break;
                case "Rating":
                    course.rating = Double.parseDouble(component);
                    break;
                case "In-Class Hours":
                    course.inClassHours = Double.parseDouble(component);
                    break;
                case "Out-of-Class Hours":
                    course.outOfClassHours = Double.parseDouble(component);
                    break;
                case "Prereq or Coreq":
                    course.setEitherPrereqOrCoreq(course.parseBoolean(component));
                    break;


                default:
                    break;
            }
        }
        courseDatabase.daoAccess().insertCourse(course);
    }

    private void loadCoursesFromURL(URL urlToRead) {
        Log.d("CourseManager", "Reading from " + urlToRead.toString());
        String path = urlToRead.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        if (fileName.equals("courses.txt") || fileName.equals("features.txt") || fileName.equals("enrollment.txt")
                || fileName.contains("condensed") || fileName.equals("related.txt")) {
            return;
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(urlToRead.openStream()));
            String inputLine;
            String[] header = null;
            while ((inputLine = in.readLine()) != null) {
                if (header == null) {
                    header = inputLine.split(",");
                } else {
                    loadCourseFromLine(inputLine, header);
                }
            }
            in.close();
        } catch (IOException e) {
            Log.d("CourseManager", "Error loading from URL " + urlToRead.toString());
            e.printStackTrace();
        }

    }

    public Course getSubjectByID(final String id) {
        return courseDatabase.daoAccess().findCourseWithSubjectID(id);
    }

}
