package com.base12innovations.android.fireroad;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoadDocument extends Document {

    public static String[] semesterNames = new String[] {
            "Prior Credit",
            "1st Year Fall",
            "1st Year IAP",
            "1st Year Spring",
            "2nd Year Fall",
            "2nd Year IAP",
            "2nd Year Spring",
            "3rd Year Fall",
            "3rd Year IAP",
            "3rd Year Spring",
            "4th Year Fall",
            "4th Year IAP",
            "4th Year Spring",
            "5th Year Fall",
            "5th Year IAP",
            "5th Year Spring"
    };

    protected static class RoadJSON {
        static String coursesOfStudy = "coursesOfStudy";
        static String selectedSubjects = "selectedSubjects";
        static String overrideWarnings = "overrideWarnings";
        static String semester = "semester";
        static String subjectTitle = "title";
        static String subjectID = "id";
        static String units = "units";
    }

    Map<Integer, List<Course>> courses = new HashMap<>();
    List<String> coursesOfStudy = new ArrayList<>();
    Map<Course, Boolean> overrides = new HashMap<>();

    public RoadDocument(File location) {
        super(location);
    }

    @Override
    public void parse(String contents) {

        try {
            JSONObject json = new JSONObject(contents);

            // load courses of study
            JSONArray majors = json.getJSONArray(RoadJSON.coursesOfStudy);
            coursesOfStudy = new ArrayList<>();
            for (int i = 0; i < majors.length(); i++) {
                coursesOfStudy.add(majors.getJSONObject(i).toString());
            }

            // load selected subjects
            JSONArray selectedSubjects = json.getJSONArray(RoadJSON.selectedSubjects);
            courses = new HashMap<>();
            overrides = new HashMap<>();
            for (int i = 0; i < selectedSubjects.length(); i++) {
                JSONObject subjectInfo = selectedSubjects.getJSONObject(i);
                String subjectID = subjectInfo.getString(RoadJSON.subjectID);
//                String subjectTitle = subjectInfo.getString(RoadJSON.subjectTitle);
//                int units = subjectInfo.getInt(RoadJSON.units);
                int semester = subjectInfo.getInt(RoadJSON.semester);
                boolean ignoreWarnings = subjectInfo.getBoolean(RoadJSON.overrideWarnings);

                if (semester < 0 || semester >= semesterNames.length) {
                    continue;
                }
                if (!courses.containsKey(semester)) {
                    courses.put(semester, new ArrayList<Course>());
                }
                Course course = CourseManager.sharedInstance().getSubjectByID(subjectID);
                if (course != null) {
                    courses.get(semester).add(course);
                    overrides.put(course, ignoreWarnings);
                } else {
                    Log.d("RoadDocument", "Couldn't find course with ID " + subjectID);
                }
            }

        } catch (JSONException e) {
            Log.d("JSON Error", String.format(Locale.US, "Invalid JSON: %s", contents));
            e.printStackTrace();
        }
    }

    @Override
    public String contentsString() {
        try {
            JSONObject parentObject = new JSONObject();

            // Write majors and minors
            JSONArray majors = new JSONArray();
            for (String major : coursesOfStudy) {
                majors.put(major);
            }
            parentObject.put(RoadJSON.coursesOfStudy, majors);

            // Write courses
            JSONArray subjects = new JSONArray();
            for (int semesterIndex : courses.keySet()) {
                List<Course> semCourses = courses.get(semesterIndex);
                for (Course course : semCourses) {
                    JSONObject courseObj = new JSONObject();
                    courseObj.put(RoadJSON.subjectID, course.getSubjectID());
                    courseObj.put(RoadJSON.subjectTitle, course.subjectTitle);
                    courseObj.put(RoadJSON.units, course.totalUnits);
                    courseObj.put(RoadJSON.semester, semesterIndex);
                    if (overrides.containsKey(course)) {
                        courseObj.put(RoadJSON.overrideWarnings, overrides.get(course));
                    } else {
                        courseObj.put(RoadJSON.overrideWarnings, false);
                    }
                    subjects.put(courseObj);
                }
            }
            parentObject.put(RoadJSON.selectedSubjects, subjects);

            Log.d("JSON write result", parentObject.toString());
            return parentObject.toString();

        } catch (JSONException e) {
            Log.e("JSON Write", "Failed to write road to file");
            e.printStackTrace();

            return "";
        }
    }

    public List<Course> getAllCourses() {
        List<Course> allCourses = new ArrayList<>();
        for (int semester : this.courses.keySet()) {
            allCourses.addAll(this.courses.get(semester));
        }
        return allCourses;
    }

    public List<Course> coursesForSemester(int semester) {
        if (courses.containsKey(semester)) {
            return courses.get(semester);
        }
        return new ArrayList<Course>();
    }

    public boolean addCourse(Course course, int semester) {
        if (semester < 0 || semester >= semesterNames.length) {
            return false;
        }
        if (!courses.containsKey(semester)) {
            courses.put(semester, new ArrayList<Course>());
        } else {
            return false;
        }
        courses.get(semester).add(course);
        save();
        return true;
    }

    public boolean removeCourse(Course course, int semester) {
        if (semester < 0 || semester >= semesterNames.length) {
            return false;
        }
        if (!courses.containsKey(semester)) {
            courses.put(semester, new ArrayList<Course>());
        }
        boolean ret = courses.get(semester).remove(course);
        save();
        return ret;
    }

    public void moveCourse(int startSemester, int startPos, int endSemester, int endPos) {
        if (!courses.containsKey(startSemester)) {
            return;
        }
        List<Course> semCourses = courses.get(startSemester);
        Course course = semCourses.get(startPos);
        semCourses.remove(startPos);
        if (startSemester == endSemester) {
            semCourses.add(endPos, course);
        } else {
            if (!courses.containsKey(endSemester)) {
                courses.put(endSemester, new ArrayList<Course>());
            }
            courses.get(endSemester).add(endPos, course);
        }
        save();
    }
}
