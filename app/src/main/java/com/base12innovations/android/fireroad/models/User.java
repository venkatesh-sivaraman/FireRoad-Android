package com.base12innovations.android.fireroad.models;

import java.util.ArrayList;
import java.util.List;

public class User {

    private RoadDocument currentDocument;
    private ScheduleDocument currentSchedule;

    public RoadDocument getCurrentDocument() {
        return currentDocument;
    }

    public void setCurrentDocument(RoadDocument newDocument) {
        this.currentDocument = newDocument;
        if (roadChangedListeners != null) {
            for (RoadChangedListener listener : roadChangedListeners) {
                listener.roadChanged(newDocument);
            }
        }
    }

    public ScheduleDocument getCurrentSchedule() { return currentSchedule; }

    public void setCurrentSchedule(ScheduleDocument currentSchedule) {
        this.currentSchedule = currentSchedule;
        if (scheduleChangedListeners != null) {
            for (ScheduleChangedListener listener : scheduleChangedListeners) {
                listener.scheduleChanged(currentSchedule);
            }
        }
    }

    private static User _currentUser;

    public static User currentUser() {
        if (_currentUser == null) {
            _currentUser = new User();
        }
        return _currentUser;
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
}
