package com.base12innovations.android.fireroad.models.req;

import android.util.Log;

import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;

import java.util.ArrayList;
import java.util.List;

public class ProgressAssertion {
    private String requirementKey;
    private List<String> subtitutions = new ArrayList<>();
    private boolean ignore;
    public ProgressAssertion(){

    }
    public ProgressAssertion(String s){

    }
    public ProgressAssertion(List<Course>courses){
        ignore = courses == null || courses.isEmpty();
        subtitutions = new ArrayList<>();
        if(!ignore){
            for(Course course : courses){
                Log.d("ProgAssert",course.getSubjectID());
                subtitutions.add(course.getSubjectID());
            }
        }
    }
    public boolean getIgnore(){
        return ignore;
    }
    public List<String> getSubstitutions(){
        return subtitutions;
    }
    @Override
    public String toString(){
        return "";
    }
}
