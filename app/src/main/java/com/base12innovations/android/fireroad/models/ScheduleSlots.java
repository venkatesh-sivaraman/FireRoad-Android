package com.base12innovations.android.fireroad.models;

import java.util.ArrayList;
import java.util.List;

public class ScheduleSlots {
    public static List<Course.ScheduleTime> slots;
    static {
        slots = new ArrayList<>();
        for (int i = 8; i <= 11; i++) {
            slots.add(new Course.ScheduleTime(i, 0, false));
            slots.add(new Course.ScheduleTime(i, 30, false));
        }
        slots.add(new Course.ScheduleTime(12, 0, true));
        slots.add(new Course.ScheduleTime(12, 30, true));
        for (int i = 1; i <= 10; i++) {
            slots.add(new Course.ScheduleTime(i, 0, true));
            slots.add(new Course.ScheduleTime(i, 30, true));
        }
    }

    public static int slotIndex(Course.ScheduleTime time) {
        int base = 0;
        if (!time.PM || time.hour == 12) {
            base = (time.hour - 8) * 2;
        } else {
            base = (time.hour + 4) * 2;
        }
        if (time.minute >= 30) {
            return base + 1;
        }
        return base;
    }

}
