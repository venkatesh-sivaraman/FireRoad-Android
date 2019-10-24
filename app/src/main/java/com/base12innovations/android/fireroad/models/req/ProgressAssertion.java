package com.base12innovations.android.fireroad.models.req;

import com.base12innovations.android.fireroad.models.course.Course;

import java.util.List;

public class ProgressAssertion {
    private String requirementKey;
    private String[] subtitutions;
    private boolean ignore;
    public ProgressAssertion(){

    }
    public ProgressAssertion(String s){

    }
    public ProgressAssertion(List<Course>courses){

    }
    @Override
    public String toString(){
        return "";
    }
}
