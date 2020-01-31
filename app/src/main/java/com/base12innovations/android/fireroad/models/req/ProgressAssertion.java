package com.base12innovations.android.fireroad.models.req;

import android.util.Log;

import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;

import java.util.ArrayList;
import java.util.List;

public class ProgressAssertion {
    private String requirementKey;
    private List<String> subtitutions = new ArrayList<>();
    public ProgressAssertion(String s, List<String> subtitutions){
        this.requirementKey = s;
        this.subtitutions = subtitutions;
    }
    public ProgressAssertion(List<Course>courses){
        subtitutions = new ArrayList<>();
        if(courses.size()!=0){
            for(Course course : courses){
                Log.d("ProgAssert",course.getSubjectID());
                subtitutions.add(course.getSubjectID());
            }
        }
    }
    public boolean getIgnore(){
        return subtitutions==null || subtitutions.size()==0;
    }
    public List<String> getSubstitutions(){
        return subtitutions;
    }
    @Override
    public String toString(){
        return (getIgnore()? "Ignoring " + requirementKey:"Substituting " + requirementKey+ " with " + subtitutions.toString() );
    }
}
