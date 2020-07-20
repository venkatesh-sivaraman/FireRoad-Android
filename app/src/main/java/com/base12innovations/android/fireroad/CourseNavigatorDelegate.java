package com.base12innovations.android.fireroad;

import android.support.v4.app.Fragment;

import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseSearchEngine;

import java.util.EnumSet;

public interface CourseNavigatorDelegate {
    String ADD_TO_SCHEDULE = "ADD_TO_SCHEDULE";

    void courseNavigatorWantsCourseDetails(Fragment source, Course course);
    void courseNavigatorWantsSearchCourses(Fragment source, String searchTerm, EnumSet<CourseSearchEngine.Filter> filters);
    void courseNavigatorAddedCourse(Fragment source, Course course, String semesterID);
}
