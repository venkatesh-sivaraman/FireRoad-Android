package com.base12innovations.android.fireroad.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    public boolean hasFinal = false;
    public boolean pdfOption = false;

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
            } else if (lowercasedDescriptions.containsValue(trimmed)) {
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
        ANY("HASS"), ARTS("HASS-A"), SOCIAL_SCIENCES("HASS-S"), HUMANITIES("HASS-H");

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
            lowercasedNames = new HashMap<>();
            lowercasedNames.put("hass", ANY);
            lowercasedNames.put("hass-a", ARTS);
            lowercasedNames.put("hass-s", SOCIAL_SCIENCES);
            lowercasedNames.put("hass-h", HUMANITIES);
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
    public HASSAttribute getHASSAttribute() {
        if (hassAttribute != null) {
            return HASSAttribute.fromRaw(hassAttribute);
        }
        return null;
    }

    public String prerequisites = "";
    public List<List<String>> getPrerequisitesList() {
        List<String> topLevel = nonemptyComponents(prerequisites, ";");
        List<List<String>> result = new ArrayList<>();
        for (String top : topLevel) {
            List<String> item = nonemptyComponents(top, ",");
            if (item.size() > 0)
                result.add(item);
        }
        return result;
    }
    public String corequisites = "";
    public List<List<String>> getCorequisitesList() {
        List<String> topLevel = nonemptyComponents(corequisites, ";");
        List<List<String>> result = new ArrayList<>();
        for (String top : topLevel) {
            List<String> item = nonemptyComponents(top, ",");
            if (item.size() > 0)
                result.add(item);
        }
        return result;
    }

    public String relatedSubjects = "";
    public List<String> getRelatedSubjectsList() {
        return nonemptyComponents(relatedSubjects, ",");
    }

    //var schedule: [String: [[CourseScheduleItem]]]?


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
        if (!obj.getClass().equals(this.getClass())) {
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
     * Specifies that a given requirement can be satisfied by a given subject ID.
     */
    private static class EquivalencePair {
        String requirement;
        String subjectID;
        EquivalencePair(String r, String s) {
            this.requirement = r;
            this.subjectID = s;
        }
    }

    /**
     * Specifies that having ALL of a given list of subject IDs is equivalent to a requirement.
     */
    private static class EquivalenceSet {
        List<String> subjectIDs;
        String requirement;
        EquivalenceSet(String r, List<String> s) {
            this.requirement = r;
            this.subjectIDs = s;
        }
    }
    private static List<EquivalencePair> equivalencePairs;
    private static List<EquivalenceSet> equivalenceSets;
    static {
        equivalencePairs = new ArrayList<>();
        equivalencePairs.add(new EquivalencePair("6.0001", "6.00"));
        equivalencePairs.add(new EquivalencePair("6.0002", "6.00"));
        equivalenceSets = new ArrayList<>();
        equivalenceSets.add(new EquivalenceSet("6.00", Arrays.asList("6.0001", "6.0002")));
    }

    /**
     If `allCourses` is not nil, it may be a list of courses that can potentially
     satisfy the requirement. If a combination of courses satisfies the requirement,
     this method will return true.
     */
    public boolean satisfiesRequirement(String requirement, final List<Course> allCourses) {
        final String req = requirement.replaceAll("GIR:", "");
        // Normal requirement-course relationship
        if (getSubjectID().equals(req) ||
                getJointSubjectsList().contains(req) ||
                getEquivalentSubjectsList().contains(req) ||
                (getGIRAttribute() != null && getGIRAttribute().satisfies(GIRAttribute.fromRaw(req))) ||
                (getCommunicationRequirement() != null && getCommunicationRequirement().satisfies(CommunicationAttribute.fromRaw(req))) ||
                (getHASSAttribute() != null && getHASSAttribute().satisfies(HASSAttribute.fromRaw(req)))) {
            return true;
        }
        // The course satisfies more than the requirement
        if (equivalencePairs.stream().filter(new Predicate<EquivalencePair>() {
            @Override
            public boolean test(EquivalencePair x) {
                return x.subjectID.equals(getSubjectID()) && x.requirement.equals(req);
            }
        }).collect(Collectors.toList()).size() > 0)
            return true;
        // Multiple courses from the allCourses list together satisfy a requirement
        if (allCourses != null) {
            List<Object> satisfyingSets = equivalenceSets.stream().filter(new Predicate<EquivalenceSet>() {
                @Override
                public boolean test(EquivalenceSet equivalenceSet) {
                    if (!equivalenceSet.requirement.equals(req)) return false;
                    for (String sID : equivalenceSet.subjectIDs) {
                        boolean found = false;
                        for (Course course : allCourses) {
                            if (course.getSubjectID() == sID) {
                                found = true;
                                break;
                            }
                        }
                        if (!found)
                            return false;
                    }
                    return true;
                }
            }).collect(Collectors.toList());
            if (satisfyingSets.size() > 0) {
                return true;
            }
        }

        return false;
    }

}
