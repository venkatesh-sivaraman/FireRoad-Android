package com.base12innovations.android.fireroad;

import android.support.v4.app.Fragment;

import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.CourseSearchEngine;

import java.util.EnumSet;

public interface CourseNavigatorDelegate {
    int ADD_TO_SCHEDULE = 1425;

    void courseNavigatorWantsCourseDetails(Fragment source, Course course);
    void courseNavigatorWantsSearchCourses(Fragment source, String searchTerm, EnumSet<CourseSearchEngine.Filter> filters);
    void courseNavigatorAddedCourse(Fragment source, Course course, int semester);
}