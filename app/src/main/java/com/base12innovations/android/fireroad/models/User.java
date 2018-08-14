package com.base12innovations.android.fireroad.models;

import java.util.ArrayList;
import java.util.List;

public class User {

    private RoadDocument currentDocument;

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
}
