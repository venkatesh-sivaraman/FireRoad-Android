package com.base12innovations.android.fireroad.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleDocument extends Document {

    protected static class ScheduleJSON {
        static String selectedSubjects = "selectedSubjects";
        static String subjectID = "id";
        static String subjectTitle = "title";
        static String allowedSections = "allowedSections";
        static String selectedSections = "selectedSections";

    }

    public List<Course> courses = new ArrayList<>();
    public Map<Course, Map<String, List<Integer>>> allowedSections;

    private int displayedScheduleIndex = -1;
    public int getDisplayedScheduleIndex() { return displayedScheduleIndex; }
    public void setDisplayedScheduleIndex(int newValue) {
        displayedScheduleIndex = newValue;
        save();
    }

    public ScheduleConfiguration selectedSchedule;

    /// Defines the selected sections before schedules are loaded
    public Map<Course, Map<String, Integer>> preloadSections;

    public ScheduleDocument(File location) {
        super(location);
    }
    public ScheduleDocument(File location, boolean readOnly) { super(location, readOnly); }

    @Override
    public List<Course> getAllCourses() {
        return courses;
    }

    @Override
    public void parse(String contents) {

        try {
            JSONObject json = new JSONObject(contents);
            Log.d("ScheduleDocument", "Loading");

            JSONArray selectedSubjects = json.getJSONArray(ScheduleJSON.selectedSubjects);
            Map<Course, Map<String, List<Integer>>> newAllowedSections = null;
            List<Course> newCourses = new ArrayList<>();
            Map<Course, Map<String, Integer>> newPreloadSections = null;

            for (int i = 0; i < selectedSubjects.length(); i++) {
                JSONObject subjectInfo = selectedSubjects.getJSONObject(i);
                String subjectID = subjectInfo.getString(ScheduleJSON.subjectID);

                Course course = CourseManager.sharedInstance().getSubjectByID(subjectID);
                if (course != null) {
                    newCourses.add(course);

                    if (subjectInfo.has(ScheduleJSON.allowedSections)) {
                        JSONObject allowedInfo = subjectInfo.getJSONObject(ScheduleJSON.allowedSections);
                        Iterator<String> it = allowedInfo.keys();
                        while (it.hasNext()) {
                            String section = it.next();
                            JSONArray allowed = allowedInfo.getJSONArray(section);
                            List<Integer> res = new ArrayList<>();
                            for (int j = 0; j < allowed.length(); j++) {
                                res.add(allowed.getInt(j));
                            }

                            if (newAllowedSections == null)
                                newAllowedSections = new HashMap<>();
                            if (!newAllowedSections.containsKey(course))
                                newAllowedSections.put(course, new HashMap<String, List<Integer>>());
                            newAllowedSections.get(course).put(section, res);
                        }
                    }

                    if (subjectInfo.has(ScheduleJSON.selectedSections)) {
                        JSONObject allowedInfo = subjectInfo.getJSONObject(ScheduleJSON.selectedSections);
                        Iterator<String> iterator = allowedInfo.keys();
                        while (iterator.hasNext()) {
                            String section = iterator.next();
                            int selectedSect = allowedInfo.getInt(section);

                            if (newPreloadSections == null)
                                newPreloadSections = new HashMap<>();
                            if (!newPreloadSections.containsKey(course))
                                newPreloadSections.put(course, new HashMap<String, Integer>());
                            newPreloadSections.get(course).put(section, selectedSect);
                        }
                        Log.d("ScheduleDocument","Restoring selected schedule " + newPreloadSections);
                    }

                } else {
                    Log.d("ScheduleDocument", "Couldn't find course with ID " + subjectID);
                }
            }

            Log.d("ScheduleDocument", "Loaded");

            courses = newCourses;
            allowedSections = newAllowedSections;
            preloadSections = newPreloadSections;

        } catch (JSONException e) {
            Log.d("JSON Error", String.format(Locale.US, "Invalid JSON: %s", contents));
            e.printStackTrace();
        }
    }

    @Override
    public String contentsString() {
        try {
            JSONObject parentObject = new JSONObject();

            // Write courses
            JSONArray subjects = new JSONArray();
            for (Course course : courses) {
                JSONObject courseObj = new JSONObject();
                courseObj.put(ScheduleJSON.subjectID, course.getSubjectID());
                courseObj.put(ScheduleJSON.subjectTitle, course.subjectTitle);

                if (allowedSections != null && allowedSections.containsKey(course)) {
                    JSONObject jAllowed = new JSONObject();
                    for (String section: allowedSections.get(course).keySet()) {
                        JSONArray jSections = new JSONArray();
                        for (Integer val: allowedSections.get(course).get(section)) {
                            jSections.put(val);
                        }
                        jAllowed.put(section, jSections);
                    }
                    courseObj.put(ScheduleJSON.allowedSections, jAllowed);
                }

                if (selectedSchedule != null) {
                    JSONObject jSelected = new JSONObject();
                    for (ScheduleUnit unit : selectedSchedule.scheduleItems) {
                        if (!unit.course.equals(course)) continue;
                        if (course.getSchedule().containsKey(unit.sectionType) &&
                                course.getSchedule().get(unit.sectionType).contains(unit.scheduleItems)) {
                            jSelected.put(unit.sectionType, course.getSchedule().get(unit.sectionType).indexOf(unit.scheduleItems));
                        }
                    }
                    Log.d("ScheduleDocument","Saving selected schedule " + selectedSchedule);
                    courseObj.put(ScheduleJSON.selectedSections, jSelected);
                }

                subjects.put(courseObj);
            }
            parentObject.put(ScheduleJSON.selectedSubjects, subjects);

            return parentObject.toString();

        } catch (JSONException e) {
            Log.e("JSON Write", "Failed to write road to file");
            e.printStackTrace();

            return "";
        }
    }

    public void addCourse(Course course) {
        courses.add(course);
        save();
    }

    public void removeCourse(Course course) {
        courses.remove(course);
        save();
    }
}
