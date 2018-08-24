package com.base12innovations.android.fireroad.utils;

import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.Locale;

public class CourseSearchSuggestion implements SearchSuggestion {

    public String subjectID;
    public String subjectTitle;
    public boolean isRecent;

    public CourseSearchSuggestion(String subjectID, String subjectTitle, boolean isRecent) {
        this.subjectID = subjectID;
        this.subjectTitle = subjectTitle;
        this.isRecent = isRecent;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(subjectID);
        parcel.writeString(subjectTitle);
        parcel.writeInt(isRecent ? 1 : 0);
    }

    public static final Creator<CourseSearchSuggestion> CREATOR = new Creator<CourseSearchSuggestion>() {
        @Override
        public CourseSearchSuggestion createFromParcel(Parcel in) {
            String subjectID = in.readString();
            String subjectTitle = in.readString();
            boolean isRecent = in.readInt() == 1;
            return new CourseSearchSuggestion(subjectID, subjectTitle, isRecent);
        }

        @Override
        public CourseSearchSuggestion[] newArray(int size) {
            return new CourseSearchSuggestion[size];
        }
    };

    @Override
    public String getBody() {
        return String.format(Locale.US, "<b>%s</b> %s", subjectID, subjectTitle);
    }
}