package com.base12innovations.android.fireroad.models.doc;

import android.util.Log;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;
import com.base12innovations.android.fireroad.utils.ListHelper;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.security.auth.Subject;

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
        static String subjectID = "subject_id";
        static String subjectIDAlt = "id";
        static String units = "units";
        static String marker = "marker";
        static String creator = "creator";
    }

    public enum SubjectMarker {
        PNR("pnr"), ABCNR("abcnr"), EXPLORATORY("exp"), PDF("pdf"),
        LISTENER("listener"), EASY("easy"), DIFFICULT("difficult"), MAYBE("maybe");

        private final String rawValue;
        SubjectMarker(String rv) {
            rawValue = rv;
        }

        private static List<SubjectMarker> allValues = new ArrayList<>();
        static {
            allValues.add(PNR);
            allValues.add(ABCNR);
            allValues.add(EXPLORATORY);
            allValues.add(PDF);
            allValues.add(LISTENER);
            allValues.add(EASY);
            allValues.add(DIFFICULT);
            allValues.add(MAYBE);
        }

        public static SubjectMarker fromRaw(final String raw) {
            int index = ListHelper.indexOfElement(allValues, new ListHelper.Predicate<SubjectMarker>() {
                @Override
                public boolean test(SubjectMarker element) {
                    return element.rawValue.equals(raw);
                }
            });
            if (index != ListHelper.NOT_FOUND)
                return allValues.get(index);
            return null;
        }

        public String readableName() {
            switch (this) {
                case PNR:
                    return "P/NR";
                case ABCNR:
                    return "A/B/C/NR";
                case EXPLORATORY:
                    return "Exploratory";
                case PDF:
                    return "P/D/F";
                case LISTENER:
                    return "Listener";
                case EASY:
                    return "Easy";
                case DIFFICULT:
                    return "Difficult";
                case MAYBE:
                    return "Maybe";
            }
            return "None";
        }

        public int getImageResource() {
            switch (this) {
                case PNR:
                    return R.drawable.marker_pnr;
                case ABCNR:
                    return R.drawable.marker_abcnr;
                case EXPLORATORY:
                    return R.drawable.marker_exp;
                case PDF:
                    return R.drawable.marker_pdf;
                case LISTENER:
                    return R.drawable.marker_listener;
                case EASY:
                    return R.drawable.marker_easy;
                case DIFFICULT:
                    return R.drawable.marker_hard;
                case MAYBE:
                    return R.drawable.marker_maybe;
            }
            return R.drawable.marker_exp;
        }
    }

    Map<Integer, List<Course>> courses = new HashMap<>();
    public List<String> coursesOfStudy = new ArrayList<>();
    private Map<Course, Boolean> overrides = new HashMap<>();
    private Map<Integer, Map<Course, SubjectMarker>> markers = new HashMap<>();

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
                String subjectID = null;
                if (subjectInfo.has(RoadJSON.subjectID)) {
                    subjectID = subjectInfo.getString(RoadJSON.subjectID);
                } else if (subjectInfo.has(RoadJSON.subjectIDAlt)) {
                    subjectID = subjectInfo.getString(RoadJSON.subjectIDAlt);
                }
                if (subjectID == null) continue;
                String subjectTitle = subjectInfo.getString(RoadJSON.subjectTitle);
//                int units = subjectInfo.getInt(RoadJSON.units);
                int semester = subjectInfo.getInt(RoadJSON.semester);
                boolean ignoreWarnings = subjectInfo.getBoolean(RoadJSON.overrideWarnings);

                if (semester < 0 || semester >= semesterNames.length) {
                    continue;
                }
                if (!courses.containsKey(semester)) {
                    courses.put(semester, new ArrayList<Course>());
                }
                Course course;
                if (subjectInfo.has(RoadJSON.creator) && subjectInfo.getString(RoadJSON.creator).length() > 0) {
                    course = CourseManager.sharedInstance().getCustomCourse(subjectID, subjectTitle);
                    if (course == null) {
                        course = new Course();
                        course.readJSON(subjectInfo);
                        CourseManager.sharedInstance().setCustomCourse(course);
                    }
                } else {
                    course = CourseManager.sharedInstance().getSubjectByID(subjectID);
                    if (course == null) {
                        Log.d("RoadDocument", "Couldn't find course with ID " + subjectID);
                        continue;
                    }
                }
                courses.get(semester).add(course);
                overrides.put(course, ignoreWarnings);
                if (subjectInfo.has(RoadJSON.marker)) {
                    SubjectMarker marker = SubjectMarker.fromRaw(subjectInfo.getString(RoadJSON.marker));
                    setSubjectMarker(marker, course, semester, false);
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
                    JSONObject courseObj = course.toJSON();
                    courseObj.put(RoadJSON.semester, semesterIndex);
                    if (overrides.containsKey(course)) {
                        courseObj.put(RoadJSON.overrideWarnings, overrides.get(course));
                    } else {
                        courseObj.put(RoadJSON.overrideWarnings, false);
                    }
                    SubjectMarker marker = subjectMarkerForCourse(course, semesterIndex);
                    if (marker != null)
                        courseObj.put(RoadJSON.marker, marker.rawValue);
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

        /*TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                NetworkManager.sharedInstance().getRoadManager().syncDocument(RoadDocument.this, true, false, true, null);
            }
        });*/
        NetworkManager.sharedInstance().getRoadManager().setJustModifiedFile(getFileName());
    }

    @Override
    public List<Course> getAllCourses() {
        List<Course> allCourses = new ArrayList<>();
        for (int semester : courses.keySet()) {
            allCourses.addAll(courses.get(semester));
        }
        return allCourses;
    }

    // Lists just courses that aren't marked as listener
    public List<Course> getCreditCourses() {
        List<Course> creditCourses = new ArrayList<>();
        for (int semester : courses.keySet()) {
            for (Course course: courses.get(semester)) {
                SubjectMarker marker = subjectMarkerForCourse(course, semester);
                if (marker != null && marker == SubjectMarker.LISTENER) continue;
                creditCourses.add(course);
            }
        }
        return creditCourses;
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
        setSubjectMarker(null, course, semester, false);
        save();
        return ret;
    }

    public void removeAllCoursesFromSemester(int semester) {
        if (semester < 0 || semester >= semesterNames.length) {
            return;
        }
        courses.get(semester).clear();
        if (markers.containsKey(semester))
            markers.remove(semester);
        save();
    }

    public void moveCourse(int startSemester, int startPos, int endSemester, int endPos) {
        if (!courses.containsKey(startSemester)) {
            return;
        }

        List<Course> semCourses = courses.get(startSemester);
        Course course = semCourses.get(startPos);

        // Update subject markers before moving
        SubjectMarker marker = subjectMarkerForCourse(course, startSemester);
        setSubjectMarker(null, course, startSemester, false);
        setSubjectMarker(marker, course, endSemester, false);

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

    // Markers

    public void setSubjectMarker(SubjectMarker marker, Course course, int semester, boolean shouldSave) {
        if (semester < 0 || semester >= semesterNames.length)
            return;

        if (!markers.containsKey(semester))
            markers.put(semester, new HashMap<Course, SubjectMarker>());
        if (marker != null)
            markers.get(semester).put(course, marker);
        else
            markers.get(semester).remove(course);
        if (shouldSave)
            save();
    }

    public SubjectMarker subjectMarkerForCourse(Course course, int semester) {
        if (!markers.containsKey(semester))
            return null;
        if (!markers.get(semester).containsKey(course))
            return null;
        return markers.get(semester).get(course);
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
        public String semester;

        private Warning(WarningType type, String semester) {
            this.type = type;
            this.semester = semester;
        }

        public static Warning notOffered(String semester) {
            return new Warning(WarningType.NOT_OFFERED, semester);
        }

        public static Warning unsatisfiedPrereq() {
            return new Warning(WarningType.UNSATISFIED_PREREQ, null);
        }

        public static Warning unsatisfiedCoreq() {
            return new Warning(WarningType.UNSATISFIED_COREQ, null);
        }
    }

    private boolean hasUnsatisfiedRequirements(Course course, RequirementsListStatement statement, int maxSemester, boolean useQuarter) {
        if (statement == null)
            return false;

        List<Course> takenCourses = coursesTakenBeforeCourse(course, maxSemester, useQuarter);
        statement.computeRequirementStatus(takenCourses);
        return !statement.isFulfilled();
    }

    /**
     * Compiles the list of courses taken before the given course, with maxSemester as the maximum
     * inclusive semester for comparison.
     */
    public List<Course> coursesTakenBeforeCourse(Course course, int maxSemester, boolean useQuarter) {
        List<Course> takenCourses = new ArrayList<>();
        for (int i = 0; i <= maxSemester + 1; i++) {
            for (Course otherCourse : coursesForSemester(i)) {
                if ((i <= maxSemester ||
                        (useQuarter && course.getQuarterOffered() != Course.QuarterOffered.BeginningOnly &&
                                otherCourse.getQuarterOffered() == Course.QuarterOffered.BeginningOnly))) {
                    takenCourses.add(otherCourse);
                }
            }
        }
        return takenCourses;
    }

    /**
     * Returns the maximum semester index if the course isn't found. Excludes prior credit from the
     * search.
     */
    public int firstSemesterForCourse(Course course) {
        // Exclude prior credit
        for (int i = 1; i < semesterNames.length; i++) {
            if (coursesForSemester(i).contains(course)) {
                return i;
            }
        }
        return semesterNames.length;
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

        boolean unsatisfiedPrereqs = hasUnsatisfiedRequirements(course, course.getPrerequisites(), semester - 1, true);
        int coreqCutoff = AppSettings.shared().getBoolean(AppSettings.ALLOW_COREQUISITES_TOGETHER, true) ? semester : semester - 1;
        boolean unsatisfiedCoreqs = hasUnsatisfiedRequirements(course, course.getCorequisites(), coreqCutoff, false);

        List<Warning> result = new ArrayList<>();
        if (semester % 3 == 1 && !course.isOfferedFall) {
            result.add(Warning.notOffered("fall"));
        } else if (semester % 3 == 2 && !course.isOfferedIAP) {
            result.add(Warning.notOffered("IAP"));
        } else if (semester % 3 == 0 && !course.isOfferedSpring) {
            result.add(Warning.notOffered("spring"));
        }
        if (!course.getEitherPrereqOrCoreq() || (unsatisfiedPrereqs && unsatisfiedCoreqs)) {
            if (unsatisfiedPrereqs)
                result.add(Warning.unsatisfiedPrereq());
            if (unsatisfiedCoreqs)
                result.add(Warning.unsatisfiedCoreq());
        }

        if (warningsCache == null)
            warningsCache = new HashMap<>();
        if (!warningsCache.containsKey(course))
            warningsCache.put(course, new HashMap<Integer, List<Warning>>());
        warningsCache.get(course).put(semester, result);

        return result;
    }
}
