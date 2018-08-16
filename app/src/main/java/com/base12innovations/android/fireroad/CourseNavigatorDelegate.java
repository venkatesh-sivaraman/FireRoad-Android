package com.base12innovations.android.fireroad;

import android.support.v4.app.Fragment;

import com.base12innovations.android.fireroad.models.Course;

public interface CourseNavigatorDelegate {
    void courseNavigatorWantsCourseDetails(Fragment source, Course course);
    void courseNavigatorWantsSearchCourses(Fragment source, String searchTerm);
    void courseNavigatorAddedCourse(Fragment source, Course course, int semester);
}
