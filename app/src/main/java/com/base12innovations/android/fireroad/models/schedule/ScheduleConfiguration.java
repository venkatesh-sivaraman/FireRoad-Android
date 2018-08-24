package com.base12innovations.android.fireroad.models.schedule;

import com.base12innovations.android.fireroad.models.course.Course;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScheduleConfiguration {

    public List<ScheduleUnit> scheduleItems;
    int conflictCount = 0;

    public ScheduleConfiguration(List<ScheduleUnit> items, int conflictCount) {
        this.scheduleItems = items;
        this.conflictCount = conflictCount;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("ScheduleConfiguration:");
        for (ScheduleUnit item : scheduleItems) {
            b.append("\n\t");
            b.append(item.toString());
        }
        return b.toString();
    }

    public class ChronologicalElement {
        public Course course;
        public String type;
        public Course.ScheduleItem item;
        public ScheduleUnit unit;

        ChronologicalElement(Course course, String type, Course.ScheduleItem item, ScheduleUnit unit) {
            this.course = course;
            this.type = type;
            this.item = item;
            this.unit = unit;
        }
    }

    public List<ChronologicalElement> chronologicalItemsForDay(int day) {
        List<ChronologicalElement> result = new ArrayList<>();
        for (ScheduleUnit unit : scheduleItems) {
            for (Course.ScheduleItem item : unit.scheduleItems) {
                if ((item.days & day) != 0)
                    result.add(new ChronologicalElement(unit.course, unit.sectionType, item, unit));
            }
        }

        Collections.sort(result, new Comparator<ChronologicalElement>() {
            @Override
            public int compare(ChronologicalElement t1, ChronologicalElement t2) {
                return t1.item.startTime.compareTo(t2.item.startTime);
            }
        });
        return result;
    }

}

