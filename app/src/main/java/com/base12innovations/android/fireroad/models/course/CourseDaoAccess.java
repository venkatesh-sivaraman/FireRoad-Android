package com.base12innovations.android.fireroad.models.course;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CourseDaoAccess {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCourse(Course course);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCourses(List<Course> courseList);

    @Query("SELECT * FROM Course WHERE subjectID = :subjectID")
    Course findCourseWithSubjectID(String subjectID);

    @Query("SELECT * FROM Course WHERE isPublic = 1")
    List<Course> publicCourses();

    @Query("SELECT * FROM Course WHERE subjectID LIKE '%' || :query || '%' OR " +
            "subjectTitle LIKE '%' || :query || '%'")
    List<Course> searchCoursesByIDOrTitle(String query);

    @Query("SELECT COUNT(*) FROM Course")
    int getNumberOfCourses();

    @Update
    void updateCourse(Course course);

    @Delete
    void deleteCourse(Course course);

    @Query("DELETE FROM Course")
    void clearCourses();
}