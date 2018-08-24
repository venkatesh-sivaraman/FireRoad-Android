package com.base12innovations.android.fireroad.models.doc;

import android.text.TextUtils;
import android.util.Log;

import com.base12innovations.android.fireroad.models.schedule.ScheduleConfiguration;
import com.base12innovations.android.fireroad.models.schedule.ScheduleUnit;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

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

    private List<Course> courses = new ArrayList<>();

    public List<Course> getCourses() { return courses; }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
        save();
    }

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
                    }

                } else {
                    Log.d("ScheduleDocument", "Couldn't find course with ID " + subjectID);
                }
            }


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

    @Override
    public String plainTextRepresentation() {
        StringBuilder builder = new StringBuilder();
        String base = file.getName();
        builder.append(base.substring(0, base.lastIndexOf('.')));
        builder.append("\n");
        for (Course course: courses) {
            builder.append(course.getSubjectID()).append(" - ").append(course.subjectTitle).append("\n");
            if (selectedSchedule != null) {
                for (String sectionType : Course.ScheduleType.ordering) {
                    for (ScheduleUnit unit : selectedSchedule.scheduleItems) {
                        if (unit.course.equals(course) && unit.sectionType.equals(sectionType)) {
                            builder.append("\t").append(sectionType).append(":");
                            List<String> comps = new ArrayList<>();
                            String location = null;
                            for (Course.ScheduleItem item : unit.scheduleItems) {
                                comps.add(item.toString());
                                if (item.location != null)
                                    location = item.location;
                            }
                            builder.append(TextUtils.join(", ", comps));
                            if (location != null) {
                                builder.append(" (").append(location).append(")");
                            }
                            builder.append("\n");
                            break;
                        }
                    }
                }
            }
        }
        return builder.toString();
    }

    @Override
    public void save() {
        super.save();

        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                NetworkManager.sharedInstance().getScheduleManager().syncDocument(ScheduleDocument.this, true, false, true, null);
            }
        });
    }

    public void addCourse(Course course) {
        if (course.isGeneric) return;
        courses.add(course);
        save();
    }

    public void removeCourse(Course course) {
        courses.remove(course);
        save();
    }
}
