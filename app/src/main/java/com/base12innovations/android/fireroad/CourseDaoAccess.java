package com.base12innovations.android.fireroad;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface CourseDaoAccess {
    @Insert
    void insertCourse(Course course);

    @Insert
    void insertCourses(List<Course> courseList);

    @Query("SELECT * FROM Course WHERE subjectID = :subjectID")
    Course findCourseWithSubjectID(String subjectID);

    @Query("SELECT COUNT(*) FROM Course")
    int getNumberOfCourses();

    @Update
    void updateCourse(Course course);

    @Delete
    void deleteCourse(Course course);
}