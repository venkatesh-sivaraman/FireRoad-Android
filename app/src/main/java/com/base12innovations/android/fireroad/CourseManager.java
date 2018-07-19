package com.base12innovations.android.fireroad;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class CourseManager {

    private static CourseManager _shared;

    private static final String DATABASE_NAME = "course_db";
    private CourseDatabase courseDatabase;
    private CourseManager() { }

    public static CourseManager sharedInstance() {
        if (_shared == null) {
            _shared = new CourseManager();
        }
        return _shared;
    }

    public void initializeDatabase(Context context) {
        if (courseDatabase != null) {
            Log.d("CourseManager", "Already initialized this database!");
            return;
        }
        courseDatabase = Room.databaseBuilder(context,
                CourseDatabase.class, DATABASE_NAME).fallbackToDestructiveMigration().build();
    }

    public void loadCourses(final Callable<Void> completionHandler) {
        TaskDispatcher.perform(new TaskDispatcher.Task<Void>() {
            @Override
            public Void perform() {
                if (courseDatabase.daoAccess().getNumberOfCourses() == 0) {
                    String[] subjectIDs = new String[] {
                            "6.003", "18.03", "7.013", "6.046"
                    };
                    String[] subjectTitles = new String[] {
                            "Signals and Systems", "Differential Equations", "Introductory Biology", "Advanced Algorithms"
                    };
                    for (int i = 0; i < subjectIDs.length; i++) {
                        Course course = new Course();
                        course.setSubjectID(subjectIDs[i]);
                        course.setSubjectTitle(subjectTitles[i]);
                        courseDatabase.daoAccess().insertCourse(course);
                    }
                } else {
                    Log.d("CourseManager", "Didn't need to load courses");
                }

                Log.d("CourseManager", "Has " + Integer.toString(courseDatabase.daoAccess().getNumberOfCourses()) + " courses");
                return null;
            }
        }, new TaskDispatcher.CompletionBlock<Void>() {
            @Override
            public void completed(Void arg) {
                try {
                    completionHandler.call();
                } catch (Exception e) {
                    Log.e("CourseManager", "Exception calling completion handler");
                    e.printStackTrace();
                }
            }
        });
    }

    public Course getSubjectByID(final String id) {
        return courseDatabase.daoAccess().findCourseWithSubjectID(id);
    }


}
