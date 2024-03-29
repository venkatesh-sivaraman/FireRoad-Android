package com.base12innovations.android.fireroad.models.course;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.Semester;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;
import com.base12innovations.android.fireroad.models.schedule.ScheduleSlots;
import com.base12innovations.android.fireroad.utils.ListHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Entity(indices = {@Index(value = {"subjectID"},
        unique = true)})
public class Course implements Parcelable {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int uid;

    private String subjectID;
    public String subjectTitle = "";
    public String subjectDescription = "";
    public int totalUnits = 0;
    public String rawVirtualStatus = "";
    public String oldSubjectID = null;

    public String getSubjectID() { return subjectID; }
    public void setSubjectID(String subjectID) { this.subjectID = subjectID; }

    public String subjectLevel = "";

    public String subjectCode() {
        if (subjectID.contains(".")) {
            return subjectID.substring(0, subjectID.indexOf("."));
        }
        return subjectID;
    }

    private String equivalentSubjects = "";
    public String getEquivalentSubjects() { return equivalentSubjects; }
    public List<String> getEquivalentSubjectsList() { return nonemptyComponents(equivalentSubjects, ","); }
    public void setEquivalentSubjects(String newValue) { this.equivalentSubjects = newValue; }

    private String jointSubjects = "";
    public String getJointSubjects() { return jointSubjects; }
    public List<String> getJointSubjectsList() { return nonemptyComponents(jointSubjects, ","); }
    public void setJointSubjects(String newValue) { this.jointSubjects = newValue; }

    private String meetsWithSubjects = "";
    public String getMeetsWithSubjects() { return meetsWithSubjects; }
    public List<String> getMeetsWithSubjectsList() { return nonemptyComponents(meetsWithSubjects, ","); }
    public void setMeetsWithSubjects(String newValue) { this.meetsWithSubjects = newValue; }

    private List<String> nonemptyComponents(String contents, String splitter) {
        List<String> result = new ArrayList<>();
        String[] comps = contents.split(splitter);
        for (int i = 0; i < comps.length; i++) {
            if (comps[i].trim().length() > 0) {
                result.add(comps[i].trim().replace("[J]", "").replace("#", ""));
            }
        }
        return result;
    }

    private boolean eitherPrereqOrCoreq = false;
    public boolean getEitherPrereqOrCoreq() { return eitherPrereqOrCoreq; }
    public void setEitherPrereqOrCoreq(boolean newValue) { eitherPrereqOrCoreq = newValue; }

    public String gradeRule = "";
    public String gradeType = "";

    private String instructors = "";
    public String getInstructors() { return instructors; }
    public List<String> getInstructorsList() {
        String[] comps = instructors.split(",");
        List<String> inst = new ArrayList<>();
        for (String comp : comps) {
            if (comp.length() == 0) continue;
            inst.add(comp);
        }
        return inst;
    }
    public void setInstructors(String newValue) { this.instructors = newValue; }

    public boolean isOfferedFall = false;
    public boolean isOfferedIAP = false;
    public boolean isOfferedSpring = false;
    public boolean isOfferedSummer = false;
    private boolean offeredThisYear = false;
    public boolean isOfferedThisYear() { return offeredThisYear; }
    public void setOfferedThisYear(boolean newValue) {
        offeredThisYear = newValue;
        updateOfferingPattern();
    }
    private String notOfferedYear = "";
    public String getNotOfferedYear() { return notOfferedYear; }
    public void setNotOfferedYear(String newValue) {
        this.notOfferedYear = newValue;
        updateOfferingPattern();
    }

    public enum OfferingPattern {
        EveryYear, AlternateYears, Never
    }
    @Ignore
    public OfferingPattern offeringPattern = OfferingPattern.EveryYear;

    private void updateOfferingPattern() {
        if (notOfferedYear != null && notOfferedYear.length() > 0) {
            offeringPattern = OfferingPattern.EveryYear;
        } else {
            offeringPattern = isOfferedThisYear() ? OfferingPattern.EveryYear : OfferingPattern.Never;
        }
    }

    public boolean variableUnits = false;
    public int labUnits = 0;
    public int lectureUnits = 0;
    public int designUnits = 0;
    public int preparationUnits = 0;

    public String sourceSemester = "";
    public boolean isHistorical = false;

    public boolean hasFinal = false;
    public boolean pdfOption = false;
    public boolean isHalfClass = false;

    public String url = "";
    private String quarterInformation = "";

    public enum QuarterOffered {
        WholeSemester, EndOnly, BeginningOnly
    }

    @Ignore
    private QuarterOffered quarterOffered;
    public QuarterOffered getQuarterOffered() {
        if (quarterOffered == null) {
            updateQuarterInformation();
        }
        return quarterOffered;
    }
    @Ignore
    private String quarterBoundaryDate;
    public String getQuarterBoundaryDate() {
        if (quarterBoundaryDate == null) {
            updateQuarterInformation();
        }
        return quarterBoundaryDate;
    };

    public String getQuarterInformation() { return quarterInformation; }
    public void setQuarterInformation(String newValue) {
        quarterInformation = newValue;
        updateQuarterInformation();
    }
    private void updateQuarterInformation() {
        String[] comps = quarterInformation.split(",");
        if (comps.length == 2) {
            if (comps[0].equals("0")) {
                quarterOffered = QuarterOffered.BeginningOnly;
            } else if (comps[0].equals("1")) {
                quarterOffered = QuarterOffered.EndOnly;
            } else {
                quarterOffered = QuarterOffered.WholeSemester;
            }
            quarterBoundaryDate = comps[1];
        } else {
            quarterOffered = QuarterOffered.WholeSemester;
            quarterBoundaryDate = null;
        }
    }

    public int enrollmentNumber = 0;
    public double rating = 0.0;
    public double inClassHours = 0.0;
    public double outOfClassHours = 0.0;

    public enum VirtualStatus {
        VIRTUAL("Virtual"), HYBRID("Virtual/In-Person"), INPERSON("In-Person");

        private static Map<String, VirtualStatus> names;
        static {
            names = new HashMap<>();
            names.put("Virtual", VIRTUAL);
            names.put("Virtual/In-Person", HYBRID);
            names.put("In-Person", INPERSON);
        }

        public final String rawValue;

        VirtualStatus(final String raw) {
            this.rawValue = raw;
        }

        @Override
        public String toString() { return rawValue; }

        public static VirtualStatus fromRaw(String raw) {
            if (names.containsKey(raw)) {
                return names.get(raw);
            }
            return null;
        }
    }

    public VirtualStatus getVirtualStatus() { return rawVirtualStatus != null ? VirtualStatus.fromRaw(rawVirtualStatus) : null; }

    public enum GIRAttribute {
        PHYSICS_1("PHY1"), PHYSICS_2("PHY2"),
        CHEMISTRY("CHEM"), BIOLOGY("BIOL"),
        CALCULUS_1("CAL1"), CALCULUS_2("CAL2"),
        LAB("LAB"), REST("REST");

        private static Map<String, GIRAttribute> lowercasedNames;
        private static Map<GIRAttribute, String> descriptions;
        private static Map<String, GIRAttribute> lowercasedDescriptions;
        static {
            lowercasedNames = new HashMap<>();
            lowercasedNames.put("phy1", PHYSICS_1);
            lowercasedNames.put("phy2", PHYSICS_2);
            lowercasedNames.put("chem", CHEMISTRY);
            lowercasedNames.put("biol", BIOLOGY);
            lowercasedNames.put("cal1", CALCULUS_1);
            lowercasedNames.put("cal2", CALCULUS_2);
            lowercasedNames.put("lab", LAB);
            lowercasedNames.put("rest", REST);
            descriptions = new HashMap<>();
            descriptions.put(GIRAttribute.PHYSICS_1, "Physics I GIR");
            descriptions.put(GIRAttribute.PHYSICS_2, "Physics II GIR");
            descriptions.put(GIRAttribute.CHEMISTRY, "Chemistry GIR");
            descriptions.put(GIRAttribute.BIOLOGY, "Biology GIR");
            descriptions.put(GIRAttribute.CALCULUS_1, "Calculus I GIR");
            descriptions.put(GIRAttribute.CALCULUS_2, "Calculus II GIR");
            descriptions.put(GIRAttribute.LAB, "Lab GIR");
            descriptions.put(GIRAttribute.REST, "REST GIR");
            lowercasedDescriptions = new HashMap<>();
            for (GIRAttribute attr : descriptions.keySet()) {
                lowercasedDescriptions.put(descriptions.get(attr).toLowerCase(), attr);
            }
        }

        public final String rawValue;

        GIRAttribute(final String raw) {
            this.rawValue = raw;
        }

        @Override
        public String toString() {
            return descriptions.get(this);
        }

        public static GIRAttribute fromRaw(String raw) {
            String trimmed = raw.toLowerCase().replaceAll("gir:", "").trim();
            if (lowercasedNames.containsKey(trimmed)) {
                return lowercasedNames.get(trimmed);
            } else if (lowercasedDescriptions.containsKey(trimmed)) {
                return lowercasedDescriptions.get(trimmed);
            }
            return null;
        }

        public boolean satisfies(GIRAttribute otherAttr) {
            return otherAttr != null && otherAttr == this;
        }
    }

    public enum CommunicationAttribute {
        CI_H("CI-H"), CI_HW("CI-HW");

        public final String rawValue;
        CommunicationAttribute(final String raw) {
            this.rawValue = raw;
        }

        private static Map<CommunicationAttribute, String> descriptions;
        private static Map<String, CommunicationAttribute> lowercasedNames;
        static {
            descriptions = new HashMap<>();
            descriptions.put(CommunicationAttribute.CI_H, "Communication Intensive");
            descriptions.put(CommunicationAttribute.CI_HW, "Communication Intensive with Writing");
            lowercasedNames = new HashMap<>();
            lowercasedNames.put("ci-h", CI_H);
            lowercasedNames.put("ci-hw", CI_HW);
        }

        @Override
        public String toString() {
            return rawValue;
        }

        public String descriptionText() {
            return descriptions.get(this);
        }

        public static CommunicationAttribute fromRaw(String raw) {
            String trimmed = raw.trim().toLowerCase();
            if (lowercasedNames.containsKey(trimmed)) {
                return lowercasedNames.get(trimmed);
            }
            return null;
        }

        public boolean satisfies(CommunicationAttribute otherAttr) {
            return otherAttr != null && (this == CI_HW || otherAttr == this);
        }
    }

    public enum HASSAttribute {
        ANY("HASS"), ARTS("HASS-A"), SOCIAL_SCIENCES("HASS-S"), HUMANITIES("HASS-H"), ELECTIVE("HASS-E");

        public final String rawValue;
        HASSAttribute(final String raw) {
            this.rawValue = raw;
        }

        private static Map<HASSAttribute, String> descriptions;
        private static Map<String, HASSAttribute> lowercasedNames;
        static {
            descriptions = new HashMap<>();
            descriptions.put(HASSAttribute.ANY, "HASS");
            descriptions.put(HASSAttribute.ARTS, "HASS Arts");
            descriptions.put(HASSAttribute.HUMANITIES, "HASS Humanities");
            descriptions.put(HASSAttribute.SOCIAL_SCIENCES, "HASS Social Sciences");
            descriptions.put(HASSAttribute.ELECTIVE, "HASS Elective");
            lowercasedNames = new HashMap<>();
            lowercasedNames.put("hass", ANY);
            lowercasedNames.put("hass-a", ARTS);
            lowercasedNames.put("hass-s", SOCIAL_SCIENCES);
            lowercasedNames.put("hass-h", HUMANITIES);
            lowercasedNames.put("hass-e", ELECTIVE);
        }

        @Override
        public String toString() {
            return rawValue;
        }

        public String descriptionText() {
            return descriptions.get(this);
        }

        public static HASSAttribute fromRaw(String raw) {
            String trimmed = raw.trim().toLowerCase();
            if (lowercasedNames.containsKey(trimmed)) {
                return lowercasedNames.get(trimmed);
            }
            return null;
        }

        public boolean satisfies(HASSAttribute otherAttr) {
            return otherAttr != null && (otherAttr == ANY || otherAttr == this);
        }
    }

    public String girAttribute;
    public GIRAttribute getGIRAttribute() { return girAttribute != null ? GIRAttribute.fromRaw(girAttribute) : null; }

    public String communicationRequirement;
    public CommunicationAttribute getCommunicationRequirement() {
        if (communicationRequirement != null) {
            return CommunicationAttribute.fromRaw(communicationRequirement);
        }
        return null;
    }

    public String hassAttribute;
    public List<HASSAttribute> getHASSAttribute() {
        if (hassAttribute != null) {
            String[] comps = hassAttribute.split(",");
            List<HASSAttribute> ret = new ArrayList<>();
            for (String comp: comps) {
                HASSAttribute attr = HASSAttribute.fromRaw(comp);
                if (attr != null)
                    ret.add(attr);
            }
            return ret;
        }
        return null;
    }

    public String prerequisites = "";
    @Ignore
    private RequirementsListStatement _prereqs = null;
    public RequirementsListStatement getPrerequisites() {
        if (_prereqs == null && prerequisites != null && prerequisites.length() > 0) {
            _prereqs = RequirementsListStatement.fromStatement(prerequisites.replace("'", "\""), null);
        }
        return _prereqs;
    }
    public String corequisites = "";
    @Ignore
    private RequirementsListStatement _coreqs = null;
    public RequirementsListStatement getCorequisites() {
        if (_coreqs == null && corequisites != null && corequisites.length() > 0) {
            _coreqs = RequirementsListStatement.fromStatement(corequisites.replace("'", "\""), null);
        }
        return _coreqs;
    }

    public String relatedSubjects;
    public List<String> getRelatedSubjectsList() {
        if (relatedSubjects == null)
            return new ArrayList<>();
        return nonemptyComponents(relatedSubjects, ",");
    }

    @Ignore
    public boolean isGeneric = false;

    // Items used for custom subjects
    public boolean isPublic = true;
    public String creator = null;
    public String customColor = null;

    // Equivalences
    public String parent = null;
    public String children = null;

    public List<String> getChildren() {
        if (children == null)
            return new ArrayList<>();
        return nonemptyComponents(children, ",");
    }

    /**
     * Sets each child's siblings to this course's parents for faster requirements computation.
     * Should be called on the background thread, since this method performs database lookups.
     */
    public void updateChildren() {
        if (children == null || children.length() == 0)
            return;
        for (String child: getChildren()) {
            Course childCourse = CourseManager.sharedInstance().getSubjectByID(child);
            if (childCourse != null) {
                childCourse.siblings = children;
                CourseManager.sharedInstance().courseDatabase.daoAccess().updateCourse(childCourse);
            }
        }
    }

    public String siblings = null;
    public List<String> getSiblings() {
        if (siblings == null)
            return new ArrayList<>();
        return nonemptyComponents(siblings, ",");
    }

    // JSON read/write (currently limited)

    private static class JSONConstants {
        static final String subjectID = "subject_id";
        static final String title = "title";
        static final String totalUnits = "total_units";
        static final String units = "units";
        static final String level = "level";
        static final String offeredFall = "offered_fall";
        static final String offeredIAP = "offered_IAP";
        static final String offeredSpring = "offered_spring";
        static final String offeredSummer = "offered_summer";
        static final String schedule = "schedule";
        static final String inClassHours = "in_class_hours";
        static final String outOfClassHours = "out_of_class_hours";
        static final String isPublic = "public";
        static final String creator = "creator";
        static final String customColor = "custom_color";
    }

    // Read and overwrite properties of this Course object based on the given JSON
    public void readJSON(JSONObject json) {
        try {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                setAttribute(key, json.get(key));
            }
        } catch (JSONException e) {
            Log.e("Course", "Couldn't read JSON");
            e.printStackTrace();
        }
    }

    public void setAttribute(String key, Object value) {
        try {
            switch (key) {
                case JSONConstants.subjectID:
                    setSubjectID((String) value);
                    break;
                case JSONConstants.title:
                    subjectTitle = (String) value;
                    break;
                case JSONConstants.level:
                    subjectLevel = (String) value;
                    break;
                case JSONConstants.schedule:
                    rawSchedule = (String) value;
                    _schedule = null; // force reload
                    break;
                case JSONConstants.creator:
                    creator = (String) value;
                    break;
                case JSONConstants.customColor:
                    customColor = (String) value;
                    break;
                case JSONConstants.totalUnits:
                case JSONConstants.units:
                    if (value instanceof Double)
                        totalUnits = ((Double)value).intValue();
                    else if (value instanceof Integer)
                        totalUnits = (Integer) value;
                    break;
                case JSONConstants.offeredFall:
                    isOfferedFall = (Boolean) value;
                    break;
                case JSONConstants.offeredIAP:
                    isOfferedIAP = (Boolean) value;
                    break;
                case JSONConstants.offeredSpring:
                    isOfferedSpring = (Boolean) value;
                    break;
                case JSONConstants.offeredSummer:
                    isOfferedSummer = (Boolean) value;
                    break;
                case JSONConstants.isPublic:
                    isPublic = (Boolean) value;
                    break;
                case JSONConstants.inClassHours:
                    if (value instanceof Double)
                        inClassHours = ((Double)value).floatValue();
                    else if (value instanceof Float)
                        inClassHours = (Float) value;
                    break;
                case JSONConstants.outOfClassHours:
                    if (value instanceof Double)
                        outOfClassHours = ((Double)value).floatValue();
                    else if (value instanceof Float)
                        outOfClassHours = (Float) value;
                    break;
            }
        } catch (ClassCastException e) {
            Log.e("Course", "Can't set key value " + key + " with this value");
        }
    }

    public JSONObject toJSON() {
        JSONObject ret = new JSONObject();
        try {
            ret.put(JSONConstants.subjectID, getSubjectID());
            ret.put(JSONConstants.title, subjectTitle);
            if (subjectLevel != null && subjectLevel.length() > 0)
                ret.put(JSONConstants.level, subjectLevel);
            if (customColor != null && customColor.length() > 0)
                ret.put(JSONConstants.customColor, customColor);
            ret.put(JSONConstants.units, totalUnits);

            if (creator != null && creator.length() > 0) {
                ret.put(JSONConstants.creator, creator);

                if (rawSchedule != null && rawSchedule.length() > 0)
                    ret.put(JSONConstants.schedule, rawSchedule);
                ret.put(JSONConstants.offeredFall, isOfferedFall);
                ret.put(JSONConstants.offeredIAP, isOfferedIAP);
                ret.put(JSONConstants.offeredSpring, isOfferedSpring);
                ret.put(JSONConstants.offeredSummer, isOfferedSummer);
                ret.put(JSONConstants.isPublic, isPublic);
                ret.put(JSONConstants.inClassHours, inClassHours);
                ret.put(JSONConstants.outOfClassHours, outOfClassHours);
            }
            return ret;

        } catch (JSONException e) {
            Log.e("Course", "Couldn't write JSON");
            e.printStackTrace();
        }
        return ret;
    }

    public Course() {}

    // 99.9% of the time you can just ignore this
    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(subjectID);
        out.writeString(subjectTitle);
        out.writeString(subjectDescription);
        out.writeInt(totalUnits);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Course> CREATOR = new Parcelable.Creator<Course>() {
        public Course createFromParcel(Parcel in) {
            return new Course(in);
        }

        public Course[] newArray(int size) {
            return new Course[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private Course(Parcel in) {
        subjectID = in.readString();
        subjectTitle = in.readString();
        subjectDescription = in.readString();
        totalUnits = in.readInt();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "<Course %s: %s>", getSubjectID(), subjectTitle);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }
        Course other = (Course)obj;
        return other.getSubjectID().equals(getSubjectID()) && other.subjectTitle.equals(subjectTitle);
    }

    public boolean parseBoolean(String value) {
        if (value.contentEquals("Y") || value.toLowerCase().contentEquals("true")) {
            return true;
        }
        return false;
    }

    // Satisfying requirements

    /**
     If `allCourses` is not nil, it may be a list of courses that can potentially
     satisfy the requirement. If a combination of courses satisfies the requirement,
     this method will return true.
     */
    public boolean satisfiesRequirement(String requirement, final List<Course> allCourses) {
        final String req = requirement.replaceAll("GIR:", "");
        // Normal requirement-course relationship
        if (getSubjectID().equals(req) ||
                (oldSubjectID != null && oldSubjectID.equals(req)) ||
                getJointSubjectsList().contains(req) ||
                getEquivalentSubjectsList().contains(req) ||
                (getGIRAttribute() != null && getGIRAttribute().satisfies(GIRAttribute.fromRaw(req))) ||
                (getCommunicationRequirement() != null && getCommunicationRequirement().satisfies(CommunicationAttribute.fromRaw(req)))) {
            return true;
        }
        List<HASSAttribute> hasses = getHASSAttribute();
        if (hasses != null) {
            for (HASSAttribute attr: hasses) {
                if (attr.satisfies(HASSAttribute.fromRaw(req))) {
                    return true;
                }
            }
        }

        // The course satisfies more than the requirement
        List<String> children = getChildren();
        if (children.contains(req))
            return true;

        // Multiple courses from the allCourses list together satisfy a requirement
        if (allCourses != null && parent != null && req.equals(parent) && siblings != null) {
            List<String> allIds = ListHelper.map(allCourses, new ListHelper.Function<Course, String>() {
                @Override
                public String apply(Course elem) {
                    return elem.subjectID;
                }
            });

            boolean allSiblingsPresent = true;
            for (String sibling: getSiblings()) {
                if (!allIds.contains(sibling)) {
                    allSiblingsPresent = false;
                    break;
                }
            }
            if (allSiblingsPresent)
                return true;
        }

        return false;
    }

    // Scheduling

    public String rawSchedule;

    public static class ScheduleDay {
        public static final int NONE = 0;
        public static final int MON = 1 << 6;
        public static final int TUES = 1 << 5;
        public static final int WED = 1 << 4;
        public static final int THURS = 1 << 3;
        public static final int FRI = 1 << 2;
        public static final int SAT = 1 << 1;
        public static final int SUN = 1 << 0;

        public static final int[] ordering = new int[] {
                MON, TUES, WED, THURS, FRI, SAT, SUN
        };

        public static int indexOf(int day) {
            for (int i = 0; i < ordering.length; i++) {
                if (ordering[i] == day)
                    return i;
            }
            return 0;
        }

        static Map<Integer, String> stringMappings;
        static {
            stringMappings = new HashMap<>();
            stringMappings.put(MON, "M");
            stringMappings.put(TUES, "T");
            stringMappings.put(WED, "W");
            stringMappings.put(THURS, "R");
            stringMappings.put(FRI, "F");
            stringMappings.put(SAT, "S");
            stringMappings.put(SUN, "S");
        }

        public static String toString(int val) {
            String result = "";
            for (int day: ordering) {
                if ((val & day) != 0) {
                    result += stringMappings.get(day);
                }
            }
            return result;
        }

        public static int fromString(String str) {
            int result = NONE;
            for (int i = 0; i < str.length(); i++) {
                for (int dayIdx: ordering) {
                    if (stringMappings.get(dayIdx).equals(str.substring(i, i + 1))) {
                        result |= dayIdx;
                    }
                }
            }
            return result;
        }

        public static int minDay(int val) {
            for (int day : ordering) {
                if ((val & day) != 0)
                    return day;
            }
            return NONE;
        }

        public static int maxDay(int val) {
            for (int i = ordering.length - 1; i >= 0; i--) {
                if ((val & ordering[i]) != 0)
                    return ordering[i];
            }
            return NONE;
        }
    }

    public static class ScheduleTime implements Comparable {
        public int hour;
        public int minute;
        public boolean PM;

        public ScheduleTime(int hour, int minute, boolean PM) {
            this.hour = hour;
            this.minute = minute;
            this.PM = PM;
        }

        public static ScheduleTime fromString(String time, boolean evening) {
            List<Integer> comps = new ArrayList<>();
            for (String comp: time.split("[,.:;]")) {
                try {
                    comps.add(Integer.parseInt(comp));
                } catch (NumberFormatException e) { }
            }
            if (comps.size() == 0) {
                Log.e("Course", "Not enough components in time string " + time);
                return new ScheduleTime(12, 0, true);
            }

            boolean pm = ((comps.get(0) <= 7) || evening);
            if (comps.get(0) == 12) pm = !evening;
            if (comps.size() == 1)
                return new ScheduleTime(comps.get(0), 0, pm);
            return new ScheduleTime(comps.get(0), comps.get(1), pm);
        }

        @Override
        public String toString() {
            return toString((PM && hour > 7 && hour != 12));
        }

        public String toString(boolean timeOfDay) {
            return Integer.toString(hour) + (minute != 0 ? String.format(Locale.US, ":%02d", minute) : "") + (timeOfDay ? (PM ? " PM" : " AM") : "");
        }

        public int hour24() {
            return hour + (PM && hour != 12 ? 12 : 0);
        }

        @Override
        public int compareTo(@NonNull Object o) {
            if (!(o instanceof ScheduleTime))
                return 0;
            ScheduleTime other = (ScheduleTime)o;
            if (hour24() != other.hour24())
                return (hour24() < other.hour24()) ? -1 : 1;
            if (minute != other.minute)
                return (minute < other.minute ? -1 : 1);
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ScheduleTime))
                return false;
            ScheduleTime other = (ScheduleTime)obj;
            return hour == other.hour && minute == other.minute && PM == other.PM;
        }

        public ScheduleTime deltaTo(ScheduleTime other) {
            if (compareTo(other) > 0) {
                ScheduleTime res = other.deltaTo(this);
                return new ScheduleTime(-res.hour, -res.minute, false);
            }
            int myHour = hour24();
            int destinationHour = other.hour24();
            int minutes = 0;
            while (myHour < destinationHour) {
                minutes += 60;
                myHour += 1;
            }
            minutes += other.minute - minute;
            return new ScheduleTime(minutes / 60, minutes % 60, false);
        }
    }

    public class ScheduleItem implements Comparable {
        public int days;
        public ScheduleTime startTime;
        public ScheduleTime endTime;
        public boolean isEvening;
        public String location;

        public ScheduleItem(String dayStr, String startTimeStr, String endTimeStr, boolean isEvening, String location) {
            this.days = ScheduleDay.fromString(dayStr);
            this.startTime = ScheduleTime.fromString(startTimeStr, isEvening);
            this.endTime = ScheduleTime.fromString(endTimeStr, isEvening);
            if (this.startTime.PM && !isEvening) {
                this.endTime.PM = true;
            }
            this.isEvening = isEvening;
            this.location = location;
        }

        public String toString(boolean withLocation) {
            return ScheduleDay.toString(days) + " " + startTime.toString(false) + "-" +
                    endTime.toString(false) + (location != null && withLocation ? " (" + location + ")" : "");
        }

        @Override
        public String toString() {
            return toString(true);
        }

        @Override
        public int compareTo(@NonNull Object o) {
            if (!(o instanceof ScheduleItem))
                return 0;
            ScheduleItem other = (ScheduleItem)o;
            if (ScheduleDay.minDay(days) != ScheduleDay.minDay(other.days)) {
                return (ScheduleDay.minDay(days) < ScheduleDay.minDay(other.days)) ? -1 : 1;
            }
            return startTime.compareTo(other.startTime);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ScheduleItem))
                return false;
            ScheduleItem other = (ScheduleItem)obj;
            return days == other.days && startTime.equals(other.startTime) && endTime.equals(other.endTime) && isEvening == other.isEvening && location.equals(other.location);
        }
    }

    public static class ScheduleType {
        public static final String LECTURE = "Lecture";
        public static final String RECITATION = "Recitation";
        public static final String LAB = "Lab";
        public static final String DESIGN = "Design";
        public static final String CUSTOM = "Custom";

        public static final String[] ordering = new String[] {
                LECTURE, RECITATION, LAB, DESIGN, CUSTOM
        };

        private static Map<String, String> abbreviations;
        static {
            abbreviations = new HashMap<>();
            abbreviations.put(LECTURE, "Lec");
            abbreviations.put(RECITATION, "Rec");
            abbreviations.put(LAB, "Lab");
            abbreviations.put(DESIGN, "Des");
            abbreviations.put(CUSTOM, "");
        }

        public static String abbreviationFor(String type) {
            if (abbreviations.containsKey(type))
                return abbreviations.get(type);
            return "N/A";
        }
    }

    @Ignore
    private Map<String, List<List<ScheduleItem>>> _schedule;
    private void parseScheduleString() {
        if (rawSchedule == null) {
            _schedule = null;
            return;
        }
        _schedule = new HashMap<>();
        for (String comp : rawSchedule.split(";")) {
            if (comp.length() == 0) continue;
            List<String> commaComponents = new ArrayList<>(Arrays.asList(comp.split(",")));
            if (commaComponents.size() == 0) continue;
            String groupType = commaComponents.remove(0);

            List<List<ScheduleItem>> items = new ArrayList<>();
            for (String scheduleOption : commaComponents) {
                String[] slashComps = scheduleOption.split("[/]");
                if (slashComps.length <= 1) continue;
                String location = slashComps[0];
                items.add(new ArrayList<ScheduleItem>());
                for (int i = 1; i < slashComps.length; i += 3) {
                    if (i + 3 > slashComps.length) break;
                    int eveningInt = Integer.parseInt(slashComps[i + 1]);
                    String startTime, endTime;
                    String timeString = slashComps[i + 2].toLowerCase().replaceAll("am|pm", "").trim();
                    if (timeString.contains("-")) {
                        String[] comps = timeString.split("-");
                        startTime = comps[0];
                        endTime = comps[1];
                    } else {
                        startTime = timeString;
                        try {
                            int timeInt = Integer.parseInt(startTime);
                            endTime = Integer.toString((timeInt % 12) + 1);
                        } catch (NumberFormatException e) {
                            // It may be a time like 7.30
                            if (startTime.contains(".")) {
                                int dotIndex = startTime.indexOf(".");
                                int hour = Integer.parseInt(startTime.substring(0, dotIndex));
                                endTime = Integer.toString((hour % 12) + 1) + "." + startTime.substring(dotIndex + 1);
                            } else {
                                Log.e("Course", "Can't parse schedule string " + scheduleOption);
                                endTime = startTime;
                            }
                        }
                    }
                    items.get(items.size() - 1).add(new ScheduleItem(slashComps[i], startTime, endTime, (eveningInt != 0), location));
                }
            }

            if (items.size() > 0) {
                Collections.sort(items, new Comparator<List<ScheduleItem>>() {
                    @Override
                    public int compare(List<ScheduleItem> scheduleItems, List<ScheduleItem> t1) {
                        return scheduleItems.get(0).compareTo(t1.get(0));
                    }
                });
                _schedule.put(groupType, items);
            }
        }
    }

    public Map<String, List<List<ScheduleItem>>> getSchedule() {
        if (_schedule == null) {
            parseScheduleString();
        }
        return _schedule;
    }

    public void updateRawSchedule() {
        if (_schedule == null)
            return;
        List<String> types = new ArrayList<>();
        for (String type: _schedule.keySet()) {
            StringBuilder b = new StringBuilder();
            b.append(type).append(",");
            for (List<ScheduleItem> option: _schedule.get(type)) {
                if (option.size() > 0 && option.get(0).location != null)
                    b.append(option.get(0).location);
                b.append("/");
                for (ScheduleItem item: option) {
                    b.append(ScheduleDay.toString(item.days)).append("/");
                    b.append(item.isEvening ? "1" : "0").append("/");
                    b.append((item.startTime.toString() + "-" +
                                    item.endTime.toString()).replace(":", ".")).append("/");
                }
                b.deleteCharAt(b.length() - 1);
                b.append(",");
            }
            b.deleteCharAt(b.length() - 1);
            types.add(b.toString());
        }

        rawSchedule = TextUtils.join(";", types);
        Log.d("Course", "new schedule is " + rawSchedule);
    }

    // Adds a new item to the first schedule option of the type
    public Course.ScheduleItem addScheduleItem(String type) {
        if (_schedule == null)
            parseScheduleString();
        if (_schedule == null)
            _schedule = new HashMap<>();
        if (!_schedule.containsKey(type))
            _schedule.put(type, new ArrayList<List<ScheduleItem>>());
        if (_schedule.get(type).size() == 0)
            _schedule.get(type).add(new ArrayList<ScheduleItem>());
        Course.ScheduleItem newItem = new Course.ScheduleItem("", "12", "1", false, "");
        _schedule.get(type).get(0).add(newItem);

        return newItem;
    }

    public static boolean isRequirementAutomaticallySatisfied(String requirement) {
        String req = requirement.replaceAll("GIR:", "");
        if (CourseManager.sharedInstance().getSubjectByID(req) != null)
            return false;
        if (GIRAttribute.fromRaw(req) != null || HASSAttribute.fromRaw(req) != null || CommunicationAttribute.fromRaw(req) != null)
            return false;
        return true;
    }

    public boolean inSemester(RoadDocument doc, Semester semester){
        return doc != null && doc.coursesForSemester(semester).contains(this);
    }
}
