package com.base12innovations.android.fireroad.models.doc;

import android.util.Log;

import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
    public List<String> coursesOfStudy = new ArrayList<>();
    private Map<Course, Boolean> overrides = new HashMap<>();

    public RoadDocument(File location) {
        super(location);
    }
    public RoadDocument(File location, boolean readOnly) {
        super(location, readOnly);
    }

    public static RoadDocument newDocument(File location) {
        RoadDocument doc = new RoadDocument(location);
        doc.coursesOfStudy = Arrays.asList("girs");
        return doc;
    }

    @Override
    public void parse(String contents) {

        try {
            JSONObject json = new JSONObject(contents);

            // load courses of study
            JSONArray majors = json.getJSONArray(RoadJSON.coursesOfStudy);
            coursesOfStudy = new ArrayList<>();
            for (int i = 0; i < majors.length(); i++) {
                coursesOfStudy.add(majors.getString(i));
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
        for (int i = 0; i < semesterNames.length; i++) {
            builder.append(semesterNames[i]);
            List<Course> semCourses = coursesForSemester(i);
            if (semCourses.size() == 0) {
                builder.append(" (no subjects)\n");
            } else {
                builder.append("\n");
                for (Course course: semCourses) {
                    builder.append("\t");
                    builder.append(course.getSubjectID());
                    builder.append(" - ");
                    builder.append(course.subjectTitle);
                    builder.append("\n");
                }
            }
        }
        return builder.toString();
    }

    @Override
    public void save() {
        warningsCache = null;
        super.save();

        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                NetworkManager.sharedInstance().getRoadManager().syncDocument(RoadDocument.this, true, false, true, null);
            }
        });
    }

    @Override
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
        if (semester < 0 || semester >= semesterNames.length)
            return false;
        if (!courses.containsKey(semester))
            courses.put(semester, new ArrayList<Course>());
        if (courses.get(semester).contains(course))
            return false;
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

    public void removeAllCoursesFromSemester(int semester) {
        if (semester < 0 || semester >= semesterNames.length) {
            return;
        }
        courses.get(semester).clear();
        save();
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

    public boolean overrideWarningsForCourse(Course course) {
        if (overrides == null || !overrides.containsKey(course))
            return false;
        return overrides.get(course);
    }

    public void setOverrideWarningsForCourse(Course course, boolean flag) {
        if (overrides == null)
            overrides = new HashMap<>();
        overrides.put(course, flag);
        save();
    }

    public void addCourseOfStudy(String listID) {
        if (!coursesOfStudy.contains(listID)) {
            coursesOfStudy.add(listID);
            save();
        }
    }

    public void removeCourseOfStudy(String listID) {
        if (coursesOfStudy.contains(listID)) {
            coursesOfStudy.remove(listID);
            save();
        }
    }

    // Warnings

    public enum WarningType {
        NOT_OFFERED, UNSATISFIED_PREREQ, UNSATISFIED_COREQ;

        @Override
        public String toString() {
            switch (this) {
                case NOT_OFFERED:
                    return "Not Offered";
                case UNSATISFIED_COREQ:
                    return "Unsatisfied Corequisite";
                case UNSATISFIED_PREREQ:
                    return "Unsatisfied Prerequisite";
                default:
                    return "";
            }
        }
    }

    public static class Warning {
        public WarningType type;
        public List<String> courses;
        public String semester;

        private Warning(WarningType type, List<String> courses, String semester) {
            this.type = type;
            this.courses = courses;
            this.semester = semester;
        }

        public static Warning notOffered(String semester) {
            return new Warning(WarningType.NOT_OFFERED, null, semester);
        }

        public static Warning unsatisfiedPrereq(List<String> courses) {
            return new Warning(WarningType.UNSATISFIED_PREREQ, courses, null);
        }

        public static Warning unsatisfiedCoreq(List<String> courses) {
            return new Warning(WarningType.UNSATISFIED_COREQ, courses, null);
        }
    }

    private List<String> unsatisfiedRequirements(Course course, List<List<String>> reqs, int maxSemester, boolean useQuarter) {
        List<String> unsatisfiedReqs = new ArrayList<>();
        for (List<String> reqList : reqs) {
            boolean satisfied = false, satisfiedByNonAuto = false, containsNonAuto = false;

            for (String prereq : reqList) {
                for (int i = 0; i <= maxSemester + 1; i++) {
                    for (Course otherCourse : coursesForSemester(i)) {
                        if (otherCourse.satisfiesRequirement(prereq, null) &&
                                (i <= maxSemester ||
                                        (useQuarter && course.getQuarterOffered() != Course.QuarterOffered.BeginningOnly &&
                                                otherCourse.getQuarterOffered() == Course.QuarterOffered.BeginningOnly))) {
                            satisfied = true;
                            break;
                        }
                    }
                    if (satisfied)
                        break;
                }
                boolean auto = Course.isRequirementAutomaticallySatisfied(prereq);
                if (!auto) {
                    if (satisfied)
                        satisfiedByNonAuto = true;
                    containsNonAuto = true;
                }
                if (!satisfied) {
                    satisfied = auto;
                }
                if (satisfied)
                    break;
            }
            if (!satisfied || (satisfied && !satisfiedByNonAuto && containsNonAuto)) {
                unsatisfiedReqs.addAll(reqList);
            }
        }
        return unsatisfiedReqs;
    }

    private Map<Course, Map<Integer, List<Warning>>> warningsCache;

    public List<Warning> warningsForCourseCached(Course course, int semester) {
        if (warningsCache != null && warningsCache.containsKey(course) && warningsCache.get(course).containsKey(semester))
            return warningsCache.get(course).get(semester);
        return null;
    }

    public List<Warning> warningsForCourse(Course course, int semester) {
        if (semester == 0) return new ArrayList<>();
        if (warningsCache != null && warningsCache.containsKey(course) && warningsCache.get(course).containsKey(semester))
            return warningsCache.get(course).get(semester);

        List<String> unsatisfiedPrereqs = unsatisfiedRequirements(course, course.getPrerequisitesList(), semester - 1, true);
        int coreqCutoff = AppSettings.shared().getBoolean(AppSettings.ALLOW_COREQUISITES_TOGETHER, true) ? semester : semester - 1;
        List<String> unsatisfiedCoreqs = unsatisfiedRequirements(course, course.getCorequisitesList(), coreqCutoff, false);

        List<Warning> result = new ArrayList<>();
        if (semester % 3 == 1 && !course.isOfferedFall) {
            result.add(Warning.notOffered("fall"));
        } else if (semester % 3 == 2 && !course.isOfferedIAP) {
            result.add(Warning.notOffered("IAP"));
        } else if (semester % 3 == 0 && !course.isOfferedSpring) {
            result.add(Warning.notOffered("spring"));
        }
        if (!course.getEitherPrereqOrCoreq() || (unsatisfiedPrereqs.size() != 0 && unsatisfiedCoreqs.size() != 0)) {
            if (unsatisfiedPrereqs.size() > 0)
                result.add(Warning.unsatisfiedPrereq(unsatisfiedPrereqs));
            if (unsatisfiedCoreqs.size() > 0)
                result.add(Warning.unsatisfiedCoreq(unsatisfiedCoreqs));
        }

        if (warningsCache == null)
            warningsCache = new HashMap<>();
        if (!warningsCache.containsKey(course))
            warningsCache.put(course, new HashMap<Integer, List<Warning>>());
        warningsCache.get(course).put(semester, result);

        return result;
    }
}
