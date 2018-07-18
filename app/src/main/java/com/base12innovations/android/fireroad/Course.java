package com.base12innovations.android.fireroad;

public class Course {
    public String subjectID;
    public String subjectTitle = "";
    public String subjectDescription = "";
    public int totalUnits = 0;

    public Course(String subjectID) {
        this.subjectID = subjectID;
    }

    public Course(String subjectID, String subjectTitle) {
        this.subjectID = subjectID;
        this.subjectTitle = subjectTitle;
    }
}
