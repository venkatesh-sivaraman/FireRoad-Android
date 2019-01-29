package com.base12innovations.android.fireroad.models.course;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Course.class}, version = 9, exportSchema = false)
public abstract class CourseDatabase extends RoomDatabase {
    public abstract CourseDaoAccess daoAccess() ;
}
