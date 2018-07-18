package com.base12innovations.android.fireroad;

import android.os.Parcel;
import android.os.Parcelable;

public class Course implements Parcelable {
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
}
