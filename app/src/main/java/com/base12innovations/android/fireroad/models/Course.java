package com.base12innovations.android.fireroad.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    public String[] getInstructorsList() { return instructors.split(","); }
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

    enum QuarterOffered {
        WholeSemester, EndOnly, BeginningOnly
    }
    @Ignore
    public QuarterOffered quarterOffered;
    @Ignore
    public String quarterBoundaryDate;

    public String getQuarterInformation() { return quarterInformation; }
    public void setQuarterInformation(String newValue) {
        quarterInformation = newValue;
        String[] comps = quarterInformation.split(",");
        if (comps.length == 2) {
            if (comps[0] == "0") {
                quarterOffered = QuarterOffered.BeginningOnly;
            } else if (comps[0] == "1") {
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

    //var girAttribute: GIRAttribute?
    //var communicationRequirement: CommunicationAttribute?
    //var hassAttribute: HASSAttribute?

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
        return other.getSubjectID().equals(getSubjectID());
    }

    public boolean parseBoolean(String value) {
        if (value.contentEquals("Y") || value.toLowerCase().contentEquals("true")) {
            return true;
        }
        return false;
    }
}
