package com.base12innovations.android.fireroad.models;

import android.content.Context;
import android.content.SharedPreferences;

import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class User {

    private RoadDocument currentDocument;
    private ScheduleDocument currentSchedule;

    public RoadDocument getCurrentDocument() {
        return currentDocument;
    }

    public void setCurrentDocument(final RoadDocument newDocument) {
        this.currentDocument = newDocument;
        setRecentRoad(newDocument.file.getPath());
        if (roadChangedListeners != null) {
            TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    for (RoadChangedListener listener : roadChangedListeners) {
                        listener.roadChanged(newDocument);
                    }
                }
            });
        }
    }

    public void loadRecentDocuments() {
        if (currentDocument == null && getRecentRoad() != null) {
            currentDocument = new RoadDocument(new File(getRecentRoad()));
            if (currentDocument.file.exists()) {
                currentDocument.read();
            }
        }
        if (currentSchedule == null && getRecentSchedule() != null) {
            currentSchedule = new ScheduleDocument(new File(getRecentSchedule()));
            if (currentSchedule.file.exists()) {
                currentSchedule.read();
            }
        }
    }

    public ScheduleDocument getCurrentSchedule() {
        /*if (currentSchedule == null && getRecentSchedule() != null) {
            currentSchedule = new ScheduleDocument(new File(getRecentSchedule()));
            if (CourseManager.sharedInstance().isLoaded()) {
                currentSchedule.readInBackground();
            } else {
                CourseManager.sharedInstance().waitForLoad(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        currentSchedule.readInBackground();
                        return null;
                    }
                });
            }
        }*/
        return currentSchedule;
    }

    public void setCurrentSchedule(final ScheduleDocument currentSchedule) {
        this.currentSchedule = currentSchedule;
        setRecentSchedule(currentSchedule.file.getPath());
        if (scheduleChangedListeners != null) {
            TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    for (ScheduleChangedListener listener : scheduleChangedListeners) {
                        listener.scheduleChanged(currentSchedule);
                    }
                }
            });
        }
    }

    private static User _currentUser;
    private SharedPreferences prefs;

    public static User currentUser() {
        if (_currentUser == null) {
            _currentUser = new User();
        }
        return _currentUser;
    }

    public void initialize(Context context) {
        prefs = context.getSharedPreferences(USER_DOCUMENT_PREFS, Context.MODE_PRIVATE);
    }

    public interface RoadChangedListener {
        void roadChanged(RoadDocument newDocument);
    }

    private List<RoadChangedListener> roadChangedListeners;

    public void addRoadChangedListener(RoadChangedListener listener) {
        if (roadChangedListeners == null) {
            roadChangedListeners = new ArrayList<>();
        }
        roadChangedListeners.add(listener);
    }

    public void removeRoadChangedListener(RoadChangedListener listener) {
        if (roadChangedListeners != null) {
            roadChangedListeners.remove(listener);
        }
    }

    public interface ScheduleChangedListener {
        void scheduleChanged(ScheduleDocument newDocument);
    }

    private List<ScheduleChangedListener> scheduleChangedListeners;

    public void addScheduleChangedListener(ScheduleChangedListener listener) {
        if (scheduleChangedListeners == null) {
            scheduleChangedListeners = new ArrayList<>();
        }
        scheduleChangedListeners.add(listener);
    }

    public void removeScheduleChangedListener(ScheduleChangedListener listener) {
        if (scheduleChangedListeners != null) {
            scheduleChangedListeners.remove(listener);
        }
    }

    // Recents

    private static String USER_DOCUMENT_PREFS = "com.base12innovations.android.fireroad.userDocumentPrefs";
    private static String RECENT_ROAD_KEY = "recentRoad";
    private static String RECENT_SCHEDULE_KEY = "recentSched";

    public void setRecentRoad(String roadPath) {
        prefs.edit().putString(RECENT_ROAD_KEY, roadPath).apply();
    }

    public String getRecentRoad() {
        return prefs.getString(RECENT_ROAD_KEY, null);
    }

    public void setRecentSchedule(String schedulePath) {
        prefs.edit().putString(RECENT_SCHEDULE_KEY, schedulePath).apply();
    }

    public String getRecentSchedule() {
        return prefs.getString(RECENT_SCHEDULE_KEY, null);
    }
}
