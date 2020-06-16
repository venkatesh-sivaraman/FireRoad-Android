package com.base12innovations.android.fireroad.models.doc;

import android.util.Log;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.req.ProgressAssertion;
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

public class RoadDocument extends Document{

    protected static class RoadJSON {
        static final String coursesOfStudy = "coursesOfStudy";
        static final String selectedSubjects = "selectedSubjects";
        static final String overrideWarnings = "overrideWarnings";
        static final String progressOverrides = "progressOverrides";
        static final String semester = "semester";
        static final String semesterID = "semesterID";
        static final String numYears = "numYears";
        static final String subjectTitle = "title";
        static final String subjectID = "subject_id";
        static final String subjectIDAlt = "id";
        static final String units = "units";
        static final String marker = "marker";
        static final String creator = "creator";
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

    Map<Semester, List<Course>> courses = new HashMap<>();
    public List<String> coursesOfStudy = new ArrayList<>();
    private Map<Course, Boolean> overrides = new HashMap<>();
    private Map<Semester, Map<Course, SubjectMarker>> markers = new HashMap<>();
    private Map<String, ProgressAssertion> progressOverrides = new HashMap<>();

    public RoadDocument(File location) {
        super(location);
        updateNumYears(4);
    }
    public RoadDocument(File location, boolean readOnly) {
        super(location, readOnly);
        updateNumYears(4);
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
            if(json.has(RoadJSON.numYears)){
                updateNumYears(json.getInt(RoadJSON.numYears));
            }else {
                updateNumYears(5);
            }
            // load selected subjects
            JSONArray selectedSubjects = json.getJSONArray(RoadJSON.selectedSubjects);
            courses = new HashMap<>();
            overrides = new HashMap<>();
            progressOverrides = new HashMap<>();
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
                boolean ignoreWarnings = subjectInfo.getBoolean(RoadJSON.overrideWarnings);
                Semester semester;
                //ensure backwards compatibility, RoadJSON.semester is the old semester, RoadJSON.semesterID is the new string
                if (subjectInfo.has(RoadJSON.semesterID)){
                    semester = new Semester(subjectInfo.getString(RoadJSON.semesterID));
                }else{
                    // this is an old version of the JSON file, we now need to ensure that it follows the summer system as well
                    // the old semester system is as follows: 0 is Prior Credit, 1-3 is 1st year, 4-6 is 2nd year, etc.
                    semester = new Semester(subjectInfo.getInt(RoadJSON.semester));
                }
                if(!isSemesterValid(semester))
                    continue;
                else if(semester.getYear() > numYears){
                    updateNumYears(semester.getYear());
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
                        course = new Course();
                        course.readJSON(subjectInfo);
                    }
                }
                courses.get(semester).add(course);
                overrides.put(course, ignoreWarnings);
                if (subjectInfo.has(RoadJSON.marker)) {
                    SubjectMarker marker = SubjectMarker.fromRaw(subjectInfo.getString(RoadJSON.marker));
                    setSubjectMarker(marker, course, semester, false);
                }
            }

            if (json.has(RoadJSON.progressOverrides)) {
                JSONObject reqOverrides = json.getJSONObject(RoadJSON.progressOverrides);
                Iterator<String> it = reqOverrides.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    //progressOverrides.put(key, reqOverrides.getInt(key));
                    JSONArray reqOverride = (reqOverrides.getJSONArray(key));
                    List<String> substitutionCourseIDs = new ArrayList<>();
                    for(int i = 0; i < reqOverride.length(); i++){
                        substitutionCourseIDs.add(reqOverride.getString(i));
                    }
                    progressOverrides.put(key,new ProgressAssertion(key,substitutionCourseIDs));
                }
            } else if (progressOverrides.size() == 0) {
                progressOverrides.putAll(CourseManager.sharedInstance().getAllProgressOverrides());
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

            parentObject.put(RoadJSON.numYears,numYears);
            // Write majors and minors
            JSONArray majors = new JSONArray();
            for (String major : coursesOfStudy) {
                majors.put(major);
            }
            parentObject.put(RoadJSON.coursesOfStudy, majors);

            // Write courses
            JSONArray subjects = new JSONArray();
            for (Semester semester : courses.keySet()) {
                List<Course> semCourses = courses.get(semester);
                for (Course course : semCourses) {
                    JSONObject courseObj = course.toJSON();
                    // we need to cast it back to the previous version. By default, will move to
                    // the prior credit section so it doesn't get deleted when viewing on an old version
                    courseObj.put(RoadJSON.semester, semester.oldSemesterIndex());
                    courseObj.put(RoadJSON.semesterID,semester.semesterID());
                    if (overrides.containsKey(course)) {
                        courseObj.put(RoadJSON.overrideWarnings, overrides.get(course));
                    } else {
                        courseObj.put(RoadJSON.overrideWarnings, false);
                    }
                    SubjectMarker marker = subjectMarkerForCourse(course, semester);
                    if (marker != null)
                        courseObj.put(RoadJSON.marker, marker.rawValue);
                    subjects.put(courseObj);
                }
            }
            parentObject.put(RoadJSON.selectedSubjects, subjects);

            JSONObject reqOverrides = new JSONObject();
            for (String keyPath: progressOverrides.keySet()) {
                JSONArray reqOverride = new JSONArray();
                List<String> substitutions = progressOverrides.get(keyPath).getSubstitutions();
                if(substitutions!=null) {
                    for (String courseID : substitutions) {
                        reqOverride.put(courseID);
                    }
                }
                reqOverrides.put(keyPath,reqOverride);
            }
            parentObject.put(RoadJSON.progressOverrides, reqOverrides);
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
        for(Semester nextSemester : semesters){
            builder.append(nextSemester.toString());
            List<Course> semCourses = coursesForSemester(nextSemester);
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
        for (Semester semester : courses.keySet()) {
            allCourses.addAll(courses.get(semester));
        }
        return allCourses;
    }

    // Lists just courses that aren't marked as listener
    public List<Course> getCreditCourses() {
        List<Course> creditCourses = new ArrayList<>();
        for (Semester semester : courses.keySet()) {
            for (Course course: courses.get(semester)) {
                SubjectMarker marker = subjectMarkerForCourse(course, semester);
                if (marker != null && marker == SubjectMarker.LISTENER) continue;
                creditCourses.add(course);
            }
        }
        return creditCourses;
    }

    public List<Course> coursesForSemester(Semester semester) {
        if (courses.containsKey(semester)) {
            return new ArrayList<>(courses.get(semester));
        }
        return new ArrayList<>();
    }

    public boolean addCourse(Course course, Semester semester) {
        if (!isSemesterValid(semester))
            return false;
        if (!courses.containsKey(semester))
            courses.put(semester, new ArrayList<Course>());
        if (courses.get(semester).contains(course))
            return false;
        courses.get(semester).add(course);
        save();
        return true;
    }

    public boolean removeCourse(Course course, Semester semester) {
        if (!isSemesterValid(semester))
            return false;
        if (!courses.containsKey(semester)) {
            courses.put(semester, new ArrayList<Course>());
        }
        boolean ret = courses.get(semester).remove(course);
        setSubjectMarker(null, course, semester, false);
        save();
        return ret;
    }

    public void removeAllCoursesFromSemester(Semester semester) {
        if (!isSemesterValid(semester))
            return;
        courses.get(semester).clear();
        if (markers.containsKey(semester))
            markers.remove(semester);
        save();
    }

    public void moveCourse(Semester startSemester, int startPos, Semester endSemester, int endPos) {
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
        if (startSemester.equals(endSemester)) {
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

    public void setSubjectMarker(SubjectMarker marker, Course course, Semester semester, boolean shouldSave) {
        if (!isSemesterValid(semester))
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

    public SubjectMarker subjectMarkerForCourse(Course course, Semester semester) {
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

    private boolean hasUnsatisfiedRequirements(Course course, RequirementsListStatement statement, Semester maxSemester, boolean useQuarter) {
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
    public List<Course> coursesTakenBeforeCourse(Course course, Semester maxSemester, boolean useQuarter) {
        List<Course> takenCourses = new ArrayList<>();
        for(Semester semester : semesters){
            if(!semester.isBeforeOrEqual(maxSemester))
                break;
            takenCourses.addAll(coursesForSemester(semester));
        }
        if(useQuarter) {
            Semester nextMaxSemester = maxSemester.nextSemester();
            if (isSemesterValid(nextMaxSemester)) {
                for (Course otherCourse : coursesForSemester(nextMaxSemester)) {
                    if (course.getQuarterOffered() != Course.QuarterOffered.BeginningOnly &&
                            otherCourse.getQuarterOffered() == Course.QuarterOffered.BeginningOnly) {
                        takenCourses.add(otherCourse);
                    }
                }
            }
        }
        return takenCourses;
    }

    /**
     * Returns the maximum semester index if the course isn't found. Excludes prior credit from the
     * search.
     */
    public Semester firstSemesterForCourse(Course course) {
        // Exclude prior credit
        Semester firstSemester = getLastSemester();
        for(Semester semester : semesters){
            if(semester.isBefore(firstSemester)){
                if(coursesForSemester(semester).contains(course)){
                    firstSemester = semester;
                }
            }
        }
        return firstSemester;
    }

    private Map<Course, Map<Semester, List<Warning>>> warningsCache;

    public List<Warning> warningsForCourseCached(Course course, Semester semester) {
        if (warningsCache != null && warningsCache.containsKey(course) && warningsCache.get(course).containsKey(semester))
            return warningsCache.get(course).get(semester);
        return null;
    }

    public List<Warning> warningsForCourse(Course course, Semester semester) {
        if (semester.isPriorCredit()) return new ArrayList<>();
        if (warningsCache != null && warningsCache.containsKey(course) && warningsCache.get(course).containsKey(semester))
            return warningsCache.get(course).get(semester);

        boolean unsatisfiedPrereqs = hasUnsatisfiedRequirements(course, course.getPrerequisites(), semester.prevSemester(), true);
        Semester coreqCutoff = AppSettings.shared().getBoolean(AppSettings.ALLOW_COREQUISITES_TOGETHER, true) ? semester : semester.prevSemester();
        boolean unsatisfiedCoreqs = hasUnsatisfiedRequirements(course, course.getCorequisites(), coreqCutoff, false);

        List<Warning> result = new ArrayList<>();
        if (semester.getSeason() == Semester.Season.Fall && !course.isOfferedFall) {
            result.add(Warning.notOffered("fall"));
        } else if (semester.getSeason() == Semester.Season.IAP && !course.isOfferedIAP) {
            result.add(Warning.notOffered("IAP"));
        } else if (semester.getSeason() == Semester.Season.Spring && !course.isOfferedSpring) {
            result.add(Warning.notOffered("spring"));
        } else if (semester.getSeason() == Semester.Season.Summer && !course.isOfferedSummer) {
            result.add(Warning.notOffered("summer"));
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
            warningsCache.put(course, new HashMap<Semester, List<Warning>>());
        warningsCache.get(course).put(semester, result);

        return result;
    }

    // Requirement Overrides

    public ProgressAssertion getProgressOverride(String keyPath) {
        if (progressOverrides.containsKey(keyPath))
            return progressOverrides.get(keyPath);
        return null;
    }

    public void setProgressOverride(String keyPath, ProgressAssertion progressAssertion) {
        progressOverrides.put(keyPath, progressAssertion);
        save();
    }

    public void removeProgressOverride(String keyPath){
        progressOverrides.remove(keyPath);
        save();
    }

    private int numYears;
    private ArrayList<Semester> semesters;
    public ArrayList<Semester> getSemesters(){
        return semesters;
    }
    public int getNumYears(){
        return numYears;
    }
    private void updateNumYears(int newNumYears){
        if(numYears != newNumYears) {
            numYears = newNumYears;
            semesters = new ArrayList<>(newNumYears * 4 + 1);
            semesters.add(new Semester(true));
            for (int y = 1; y <= newNumYears; y++) {
                for (Semester.Season season : Semester.Season.values()) {
                    semesters.add(new Semester(y, season));
                }
            }
        }
    }
    public Semester getLastSemester(){
        return new Semester(numYears, Semester.Season.Summer);
    }
    public boolean removeYearIsValid(){
        if(numYears <= 4)
            return false;
        return (coursesForSemester(new Semester(numYears, Semester.Season.Fall)).size() == 0 &&
                coursesForSemester(new Semester(numYears, Semester.Season.IAP)).size() == 0 &&
                coursesForSemester(new Semester(numYears, Semester.Season.Spring)).size() == 0 &&
                coursesForSemester(new Semester(numYears, Semester.Season.Summer)).size() == 0);
    }
    public void removeLastYear(){
        for(Semester.Season season : Semester.Season.values()) {
            courses.remove(new Semester(numYears,season));
            markers.remove(new Semester(numYears,season));
        }
        updateNumYears(numYears-1);
        save();
    }

    public void addAnotherYear(){
        updateNumYears(numYears+1);
        save();
    }

    public boolean isSemesterValid(Semester semester){
        return semester.isPriorCredit() || (semester.getYear() > 0 && semester.getYear() <= numYears && semester.getSeason() != null);
    }
}
