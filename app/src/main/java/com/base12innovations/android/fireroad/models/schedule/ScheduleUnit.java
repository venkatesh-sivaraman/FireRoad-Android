package com.base12innovations.android.fireroad.models.schedule;

import com.base12innovations.android.fireroad.models.course.Course;

import java.util.List;

public class ScheduleUnit {

    public Course course;
    public String sectionType;
    public List<Course.ScheduleItem> scheduleItems;

    public ScheduleUnit(Course course, String sectionType, List<Course.ScheduleItem> items) {
        this.course = course;
        this.sectionType = sectionType;
        this.scheduleItems = items;
    }

    @Override
    public String toString() {
        return course.getSubjectID() + " " + sectionType + ": " + scheduleItems.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ScheduleUnit)) {
            return false;
        }
        ScheduleUnit other = (ScheduleUnit)obj;
        return course.equals(other.course) &&
                sectionType.equals(other.sectionType) &&
                scheduleItems.equals(other.scheduleItems);
    }
}
