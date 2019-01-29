package com.base12innovations.android.fireroad.models.course;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.doc.NetworkManager;
import com.base12innovations.android.fireroad.models.req.RequirementsListManager;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CourseManager {

    private static CourseManager _shared;

    // Preferences
    private static String COURSE_DATABASE_PREFERENCES = "com.base12innovations.android.fireroad.courseDatabasePreferences";
    private SharedPreferences dbPreferences;
    private static String prefsDatabaseVersionKey = "databaseVersionKey";
    private static String prefsRequirementsVersionKey = "requirementsVersionKey";
    private static String prefsDatabaseSemesterKey = "databaseSemesterKey";
    // This preferences flag allows the load to restart if it crashed in the middle
    private static String hasPerformedFullLoad = "hasPerformedFullLoad";

    private static String RATINGS_PREFS = "com.base12innovations.android.fireroad.ratingsPreferences";
    private SharedPreferences ratingsPreferences;

    private Context context;

    // During debugging, use to force a database update
    private boolean forceUpdate = false;

    // Stored until a successful database update
    private String newSemester;
    private int newDatabaseVersion;
    private int newRequirementsVersion;

    private boolean _isLoading = false;
    private boolean _isUpdatingDB = false;
    public boolean isLoading() { return _isLoading; }
    public boolean isUpdatingDatabase() { return _isUpdatingDB; }
    private float loadingProgress = 0.0f;
    private boolean _isLoaded = false;
    public boolean isLoaded() { return _isLoaded; }
    public float getLoadingProgress() { return loadingProgress; }
    private List<Callable<Void>> loadingCompletionHandlers;
    public Callable<Void> postLoadBlock; // Called after loading but before posting notifications

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
        this.context = context;
        courseDatabase = Room.databaseBuilder(context,
                CourseDatabase.class, DATABASE_NAME).fallbackToDestructiveMigration().build();

        dbPreferences = context.getSharedPreferences(COURSE_DATABASE_PREFERENCES, Context.MODE_PRIVATE);
        ratingsPreferences = context.getSharedPreferences(RATINGS_PREFS, Context.MODE_PRIVATE);

        // Initialize other singletons
        AppSettings.initialize(context);
        CourseSearchEngine.sharedInstance().initialize(context);
        RequirementsListManager.sharedInstance().initialize(context);
        NetworkManager.sharedInstance().initialize(context);
    }

    // Generic courses

    public static Map<String, Course> genericCourses;
    static {
        genericCourses = new HashMap<>();
        for (Course.GIRAttribute gir : Course.GIRAttribute.values())
            genericCourses.put(gir.rawValue, createGenericCourse(gir.rawValue, gir, null, null));
        for (Course.HASSAttribute hass : Course.HASSAttribute.values()) {
            if (hass == Course.HASSAttribute.ANY) continue;
            genericCourses.put(hass.rawValue, createGenericCourse(hass.rawValue, null, hass, null));
            String hassID = Course.CommunicationAttribute.CI_H.rawValue + " " + hass.rawValue;
            genericCourses.put(hassID, createGenericCourse(hassID, null, hass, Course.CommunicationAttribute.CI_H));
        }
        for (Course.CommunicationAttribute ci : Course.CommunicationAttribute.values())
            genericCourses.put(ci.rawValue, createGenericCourse(ci.rawValue, null, Course.HASSAttribute.ANY, ci));
    }

    private static Course createGenericCourse(String id, Course.GIRAttribute gir, Course.HASSAttribute hass, Course.CommunicationAttribute ci) {
        Course course = new Course();
        course.setSubjectID(id);
        StringBuilder b = new StringBuilder();
        b.append("Generic");
        if (ci != null)
            b.append(" ").append(ci.rawValue);
        if (hass != null && (ci == null || hass != Course.HASSAttribute.ANY))
            b.append(" ").append(hass.rawValue);
        if (gir != null)
            b.append(" ").append(gir.toString());
        course.subjectTitle = b.toString();
        course.girAttribute = gir != null ? gir.rawValue : "";
        course.hassAttribute = hass != null ? hass.rawValue : "";
        course.communicationRequirement = ci != null ? ci.rawValue : "";
        course.subjectDescription = "Use this generic subject to indicate that you are fulfilling a requirement, but do not yet have a specific subject selected.";
        course.isOfferedFall = true;
        course.isOfferedIAP = true;
        course.isOfferedSpring = true;
        course.isGeneric = true;
        course.totalUnits = 12;
        return course;

    }

    // Loading the database

    public interface LoadCoursesListener {
        void completion();
        void error(String userMessage);
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
                List<URL> myURLs;
                try {
                    myURLs = determineURLsToUpdate();
                } catch (ConnectException e) {
                    _isLoading = false;
                    listener.error(context.getResources().getString(R.string.course_network_error_message));
                    return;
                }
                final List<URL> urls = myURLs;
                if (urls == null) {
                    _isLoading = false;
                    listener.error(null);
                    return;
                }

                TaskDispatcher.perform(new TaskDispatcher.Task<Void>() {
                    @Override
                    public Void perform() {
                        Log.d("CourseManager", "Updating " + Integer.toString(urls.size()) + " URLs");
                        if (urls.size() > 0) {
                            listener.needsFullLoad();
                            // Mark that a database update has been started
                            dbPreferences.edit().putBoolean(hasPerformedFullLoad, true).apply();
                            courseDatabase.daoAccess().clearCourses();
                            loadingProgress = 0.0f;
                            _isUpdatingDB = true;

                            ExecutorService exec = Executors.newFixedThreadPool(3);
                            for (int i = 0; i < urls.size(); i++) {
                                final int index = i;
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!_isLoading) // Has broken because of an error
                                            return;

                                        // Determine the type of file and send to the appropriate parser
                                        URLLoadResult result;
                                        if (urls.get(index).getPath().contains(requirementsPrefix)) {
                                            result = downloadRequirementsFile(urls.get(index));
                                        } else {
                                            result = loadCoursesFromURL(urls.get(index));
                                        }
                                        if (result == URLLoadResult.CONNECT_ERROR && _isLoading) {
                                            _isLoading = false;
                                            listener.error(context.getResources().getString(R.string.course_network_error_message));
                                            return;
                                        }
                                        loadingProgress += 1.0f / (float) urls.size();
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
                            if (!_isLoading) {
                                _isUpdatingDB = false;
                                return null;
                            }
                        }

                        SharedPreferences.Editor editor = dbPreferences.edit();
                        if (newDatabaseVersion != 0)
                            editor.putInt(prefsDatabaseVersionKey, newDatabaseVersion);
                        if (newRequirementsVersion != 0)
                            editor.putInt(prefsRequirementsVersionKey, newRequirementsVersion);
                        if (newSemester != null && newSemester.length() > 0)
                            editor.putString(prefsDatabaseSemesterKey, newSemester);
                        editor.putBoolean(hasPerformedFullLoad, true);
                        editor.apply();
                        setUpdatedOnLaunch();

                        RequirementsListManager.sharedInstance().loadRequirementsFiles();
                        _isUpdatingDB = false;
                        if (postLoadBlock != null) {
                            try {
                                postLoadBlock.call();
                            } catch (Exception e) { }
                        }
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

    private static String NEEDS_UPDATE_ON_LAUNCH_KEY = "needsUpdateOnLaunch";
    // Increment the cutoff value to set update on launch
    private static int UPDATE_ON_LAUNCH_CUTOFF = 3;

    public boolean needsUpdateOnLaunch() {
        return dbPreferences.getInt(NEEDS_UPDATE_ON_LAUNCH_KEY, 0) < UPDATE_ON_LAUNCH_CUTOFF;
    }

    private void setUpdatedOnLaunch() {
        dbPreferences.edit().putInt(NEEDS_UPDATE_ON_LAUNCH_KEY, UPDATE_ON_LAUNCH_CUTOFF).apply();
    }

    public void setNeedsDatabaseUpdate() {
        dbPreferences.edit().putInt(NEEDS_UPDATE_ON_LAUNCH_KEY, 0).apply();
    }

    // Internet

    private static String catalogDownloadURL = NetworkManager.CATALOG_BASE_URL + "catalogs/";
    private static String requirementsPrefix = "requirements/";
    private static String relatedCoursesFileIdentifier = "related";

    /*
    This function saves the current version to the preferences, and the completion block is passed
    a list of paths to update.
     */
    private List<String> determineDatabaseVersion(String semester) {
        // We always use version 0, because this implementation needs to rebuild the entire database every time
        // there is an update.
        int version = 0; //dbPreferences.getInt(prefsDatabaseVersionKey, 0);
        int reqVersion = dbPreferences.getInt(prefsRequirementsVersionKey, 0);
        NetworkManager.Response<HashMap<String, Object>> resp = NetworkManager.sharedInstance().determineDatabaseVersion(semester, version, reqVersion);
        if (resp.result == null) {
            return null;
        }

        HashMap<String, Object> response = resp.result;

        int currentVersion = (int)Math.round((Double)response.get("v"));
        List<String> delta = new ArrayList<>();
        if (response.containsKey("delta") && response.get("delta") instanceof List) {
            delta.addAll((List<String>) response.get("delta"));
        }
        List<String> deltaList = new ArrayList<>();
        deltaList.addAll(delta);

        reqVersion = (int)Math.round((Double)response.get("rv"));
        if (reqVersion != 0) {
            delta = new ArrayList<>();
            if (response.containsKey("r_delta") && response.get("r_delta") instanceof List) {
                delta.addAll((List<String>) response.get("r_delta"));
            }
            deltaList.addAll(delta);
            newRequirementsVersion = reqVersion;
        }

        newDatabaseVersion = currentVersion;
        return deltaList;
    }

    private List<URL> determineURLsToUpdate() throws ConnectException {
        newSemester = NetworkManager.sharedInstance().determineCurrentSemester().result;
        if (newSemester == null)
            return new ArrayList<>();

        List<String> delta = determineDatabaseVersion(newSemester.replace("-", ","));
        if (delta == null)
            return new ArrayList<>();

        boolean needUpdate = !dbPreferences.getBoolean(hasPerformedFullLoad, false) || forceUpdate || needsUpdateOnLaunch();
        int currentDBVersion = needUpdate ? 0 : dbPreferences.getInt(prefsDatabaseVersionKey, 0);
        int currentReqVersion = needUpdate ? 0 : dbPreferences.getInt(prefsRequirementsVersionKey, 0);
        List<URL> urls = new ArrayList<>();
        if (newDatabaseVersion != currentDBVersion || newRequirementsVersion != currentReqVersion) {
            String relatedPath = null;
            for (String path : delta) {
                /*if (newDatabaseVersion == currentDBVersion && !path.contains(requirementsPrefix)) {
                    continue;
                }*/
                if (path.contains(relatedCoursesFileIdentifier)) {
                    relatedPath = path;
                    continue;
                }
                try {
                    urls.add(new URL(catalogDownloadURL + path));
                } catch (MalformedURLException e) {
                    Log.e("CourseManager", "Malformed URL, " + catalogDownloadURL + path);
                }
            }
            if (relatedPath != null) {
                try {
                    urls.add(new URL(catalogDownloadURL + relatedPath));
                } catch (MalformedURLException e) {
                    Log.e("CourseManager", "Malformed URL, " + catalogDownloadURL + relatedPath);
                }
            }
        }
        return urls;

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
                case "Prereqs":
                    course.prerequisites = component;
                    break;
                case "Coreqs":
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
                    break;
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
                    break;*/
                case "Schedule":
                    course.rawSchedule = component;
                    break;
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

    private void loadRelatedSubjectsFromLine(String line) {
        String comps[] = line.trim().split(",");
        String subjectID = comps[0];
        Course course = getSubjectByID(subjectID);
        if (course == null) {
            return;
        }
        // Remove the relevance values from the components
        String[] idsOnly = new String[(comps.length - 1) / 2];
        int index = 0;
        for (int i = 1; i < comps.length; i += 2) {
            idsOnly[index] = comps[i];
            index += 1;
        }
        course.relatedSubjects = TextUtils.join(",", idsOnly);
        courseDatabase.daoAccess().updateCourse(course);
    }

    private enum URLLoadResult {
        SUCCESS, CONNECT_ERROR, OTHER_ERROR, SKIPPING;
    }

    private URLLoadResult loadCoursesFromURL(URL urlToRead) {
        String path = urlToRead.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        if (fileName.equals("courses.txt") || fileName.equals("features.txt") || fileName.equals("enrollment.txt")
                || fileName.contains("condensed")) {
            return URLLoadResult.SKIPPING;
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(urlToRead.openStream()));
            String inputLine;
            String[] header = null;
            while ((inputLine = in.readLine()) != null) {
                if (fileName.contains("related")) {
                    loadRelatedSubjectsFromLine(inputLine);
                } else if (header == null) {
                    header = inputLine.split(",");
                } else {
                    loadCourseFromLine(inputLine, header);
                }
            }
            in.close();
            return URLLoadResult.SUCCESS;
        } catch (IOException e) {
            Log.d("CourseManager", "Error loading from URL " + urlToRead.toString());
            e.printStackTrace();
            if (e instanceof ConnectException)
                return URLLoadResult.CONNECT_ERROR;
            else
                return URLLoadResult.OTHER_ERROR;
        }
    }

    private URLLoadResult downloadRequirementsFile(URL url) {
        try {
            URLConnection ucon = url.openConnection();
            ucon.setReadTimeout(5000);
            ucon.setConnectTimeout(10000);

            InputStream is = ucon.getInputStream();
            BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

            String fileName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
            File file = RequirementsListManager.sharedInstance().getPathForRequirementsFile(fileName);

            boolean success;
            if (file.exists()) {
                success = file.delete();
            }
            success = file.createNewFile();
            if (!success) {
                Log.d("CourseManager", "Failed to create file to download requirements file");
                return URLLoadResult.OTHER_ERROR;
            }

            FileOutputStream outStream = new FileOutputStream(file);
            byte[] buff = new byte[5 * 1024];

            int len;
            while ((len = inStream.read(buff)) != -1) {
                outStream.write(buff, 0, len);
            }

            outStream.flush();
            outStream.close();
            inStream.close();

            return URLLoadResult.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof ConnectException)
                return URLLoadResult.CONNECT_ERROR;
            return URLLoadResult.OTHER_ERROR;
        }
    }

    // Serving courses

    public Course getSubjectByID(final String id) {
        Course result = courseDatabase.daoAccess().findCourseWithSubjectID(id);
        if (result == null && genericCourses.containsKey(id)) {
            result = genericCourses.get(id);
        }
        return result;
    }

    // Ratings

    public static int NO_RATING = 123;

    public void setRatingForCourse(Course course, int rating) {
        ratingsPreferences.edit().putInt(course.getSubjectID(), rating).apply();
        Map<String, Integer> ratingMap = new HashMap<>();
        ratingMap.put(course.getSubjectID(), rating);
        NetworkManager.sharedInstance().submitUserRatings(ratingMap);
    }

    public int getRatingForCourse(Course course) {
        return ratingsPreferences.getInt(course.getSubjectID(), NO_RATING);
    }

    private Map<String, Map<Course, Double>> subjectRecommendations;
    public static String RECOMMENDATION_KEY_FOR_YOU = "for-you";

    public interface RecommendationsFetchCompletion {
        void completed(Map<String, Map<Course, Double>> result);
        void error(int code);
    }

    public Map<String, Map<Course, Double>> getSubjectRecommendations() {
        return subjectRecommendations;
    }

    public void fetchRecommendations(final RecommendationsFetchCompletion completion) {
        NetworkManager.sharedInstance().fetchRecommendations(new NetworkManager.RecommendationsFetchCompletion() {
            @Override
            public void completed(final Map<String, Object> result) {
                // Parse out the map and get courses
                TaskDispatcher.perform(new TaskDispatcher.Task<Map<String, Map<Course, Double>>>() {
                    @Override
                    public Map<String, Map<Course, Double>> perform() {
                        subjectRecommendations = new HashMap<>();
                        for (String key : result.keySet()) {
                            Map<String, Double> recSet = (Map<String, Double>) result.get(key);
                            subjectRecommendations.put(key, new HashMap<Course, Double>());
                            for (String subj : recSet.keySet()) {
                                Course course = getSubjectByID(subj);
                                subjectRecommendations.get(key).put(course, recSet.get(subj));
                            }
                        }
                        return subjectRecommendations;
                    }
                }, new TaskDispatcher.CompletionBlock<Map<String, Map<Course, Double>>>() {
                    @Override
                    public void completed(Map<String, Map<Course, Double>> arg) {
                        completion.completed(arg);
                    }
                });

            }

            @Override
            public void error(int code) {
                completion.error(code);
            }
        });
    }

    // Synced Preferences

    public void syncPreferences() {
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                NetworkManager.Response<List<String>> resp = NetworkManager.sharedInstance().getFavorites();
                if (resp.result != null && resp.result.size() > 0) {
                    favoriteCourses = new ArrayList<>(resp.result);
                    if (favoritesChangedListener != null) {
                        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                            @Override
                            public void perform() {
                                favoritesChangedListener.changed(favoriteCourses);
                            }
                        });
                    }
                } else if (favoriteCourses != null && favoriteCourses.size() > 0) {
                    NetworkManager.sharedInstance().setFavorites(new ArrayList<>(favoriteCourses));
                }
            }
        });
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                NetworkManager.Response<Map<String, Object>> resp = NetworkManager.sharedInstance().getNotes();
                if (resp.result != null && resp.result.size() > 0) {
                    notes = new HashMap<>();
                    for (String key : resp.result.keySet()) {
                        if (resp.result.get(key) instanceof String)
                            notes.put(key, (String)resp.result.get(key));
                    }
                } else if (notes != null && notes.size() > 0) {
                    NetworkManager.sharedInstance().setNotes(new HashMap<>(notes));
                }
            }
        });
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                NetworkManager.Response<Map<String, Object>> resp = NetworkManager.sharedInstance().getProgressOverrides();
                if (resp.result != null && resp.result.size() > 0) {
                    progressOverrides = new HashMap<>();
                    for (String key : resp.result.keySet()) {
                        progressOverrides.put(key, (int)Math.round((Double)resp.result.get(key)));
                    }
                } else if (progressOverrides != null && progressOverrides.size() > 0) {
                    NetworkManager.sharedInstance().setProgressOverrides(new HashMap<>(progressOverrides));
                }
            }
        });
    }

    private static String FAVORITE_COURSES_KEY = "favoriteCourses";
    private static String NOTES_KEY = "notes";
    private static String PROGRESS_OVERRIDES_KEY = "progressOverrides";
    private List<String> favoriteCourses;
    private Map<String, String> notes;
    private Map<String, Integer> progressOverrides;

    public interface FavoritesChangedListener {
        void changed(List<String> newCourses);
    }

    private FavoritesChangedListener favoritesChangedListener;

    public List<String> getFavoriteCourses() {
        if (favoriteCourses == null) {
            String raw = dbPreferences.getString(FAVORITE_COURSES_KEY, "");
            if (raw.length() == 0) {
                favoriteCourses = new ArrayList<>();
            } else {
                favoriteCourses = new ArrayList<>(Arrays.asList(raw.split(",")));
            }
        }
        return favoriteCourses;
    }

    public void addCourseToFavorites(Course course) {
        if (favoriteCourses == null)
            favoriteCourses = new ArrayList<>();
        if (!favoriteCourses.contains(course.getSubjectID()))
            favoriteCourses.add(course.getSubjectID());
        dbPreferences.edit().putString(FAVORITE_COURSES_KEY, TextUtils.join(",", favoriteCourses)).apply();
        NetworkManager.sharedInstance().setFavorites(new ArrayList<>(favoriteCourses));
        if (favoritesChangedListener != null)
            favoritesChangedListener.changed(favoriteCourses);
    }

    public void removeCourseFromFavorites(Course course) {
        if (favoriteCourses == null)
            favoriteCourses = new ArrayList<>();
        if (favoriteCourses.contains(course.getSubjectID()))
            favoriteCourses.remove(course.getSubjectID());
        dbPreferences.edit().putString(FAVORITE_COURSES_KEY, TextUtils.join(",", favoriteCourses)).apply();
        NetworkManager.sharedInstance().setFavorites(new ArrayList<>(favoriteCourses));
        if (favoritesChangedListener != null)
            favoritesChangedListener.changed(favoriteCourses);
    }

    public void setFavoritesChangedListener(FavoritesChangedListener listener) {
        favoritesChangedListener = listener;
    }

    public String getNotes(Course course) {
        if (notes == null) {
            String raw = dbPreferences.getString(NOTES_KEY, "");
            if (raw.length() == 0) {
                notes = new HashMap<>();
            } else {
                notes = new HashMap<>();
                for (String comp : raw.split(";")) {
                    String[] subcomps = comp.split(",");
                    if (subcomps.length >= 2)
                        notes.put(subcomps[0], subcomps[1]);
                }
            }
        }
        if (!notes.containsKey(course.getSubjectID()))
            return "";
        return notes.get(course.getSubjectID());
    }

    public void setNotes(Course course, String note) {
        notes.put(course.getSubjectID(), note);
        List<String> comps = new ArrayList<>();
        for (String key : notes.keySet()) {
            comps.add(key + "," + notes.get(key));
        }
        dbPreferences.edit().putString(NOTES_KEY, TextUtils.join(";", comps)).apply();
        NetworkManager.sharedInstance().setNotes(new HashMap<>(notes));
    }

    public int getProgressOverrides(String keyPath) {
        if (progressOverrides == null) {
            String raw = dbPreferences.getString(PROGRESS_OVERRIDES_KEY, "");
            if (raw.length() == 0) {
                progressOverrides = new HashMap<>();
            } else {
                progressOverrides = new HashMap<>();
                for (String comp : raw.split(";")) {
                    String[] subcomps = comp.split(",");
                    progressOverrides.put(subcomps[0], Integer.parseInt(subcomps[1]));
                }
            }
        }
        if (!progressOverrides.containsKey(keyPath))
            return 0;
        return progressOverrides.get(keyPath);
    }

    public void setProgressOverride(String keyPath, int value) {
        progressOverrides.put(keyPath, value);
        List<String> comps = new ArrayList<>();
        for (String key : progressOverrides.keySet()) {
            comps.add(key + "," + Integer.toString(progressOverrides.get(key)));
        }
        dbPreferences.edit().putString(PROGRESS_OVERRIDES_KEY, TextUtils.join(";", comps)).apply();
        NetworkManager.sharedInstance().setProgressOverrides(new HashMap<>(progressOverrides));
    }

    // Current semester

    public void updateCurrentSemester(String newClassYear) {
        int month = Calendar.getInstance().get(Calendar.MONTH);
        int yearNumber = Integer.parseInt(newClassYear);
        int oldSemester = AppSettings.shared().getInt(AppSettings.CURRENT_SEMESTER, 1);
        int newSemester;
        if (month >= 4 && month <= 10) {
            newSemester = 1 + (yearNumber - 1) * 3;
        } else {
            newSemester = yearNumber * 3;
        }

        if (newSemester != oldSemester) {
            AppSettings.shared().edit().putInt(AppSettings.CURRENT_SEMESTER, newSemester).apply();
            NetworkManager.sharedInstance().updateCurrentSemester(newSemester);
        }
    }

}
