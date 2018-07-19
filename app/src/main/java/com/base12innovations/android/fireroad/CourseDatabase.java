package com.base12innovations.android.fireroad;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Course.class}, version = 2, exportSchema = false)
public abstract class CourseDatabase extends RoomDatabase {
    public abstract CourseDaoAccess daoAccess() ;
}
