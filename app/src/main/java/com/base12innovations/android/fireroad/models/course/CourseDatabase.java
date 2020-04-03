package com.base12innovations.android.fireroad.models.course;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Course.class}, version = 13, exportSchema = false)
public abstract class CourseDatabase extends RoomDatabase {
    public abstract CourseDaoAccess daoAccess() ;
}
