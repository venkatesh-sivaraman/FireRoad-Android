package com.base12innovations.android.fireroad.models.course;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

public class ColorManager {

    private static String[] departmentNumbers = new String[] {
            "1", "2", "3", "4",
            "5", "6", "7", "8",
            "9", "10", "11", "12",
            "14", "15", "16", "17",
            "18", "20", "21", "21A",
            "21W", "CMS", "21G", "21H",
            "21L", "21M", "WGS", "22",
            "24", "CC", "CSB", "EC",
            "EM", "ES", "HST", "IDS",
            "MAS", "SCM", "STS", "SWE", "SP"
    };

    private static Map<String, Float> hueMap;
    static {
        hueMap = new HashMap<String, Float>();
        hueMap.put("1", 0.0f);
        hueMap.put("2", 20.0f);
        hueMap.put("3", 225.0f);
        hueMap.put("4", 128.0f);
        hueMap.put("5", 162.0f);
        hueMap.put("6", 210.0f);
        hueMap.put("7", 218.0f);
        hueMap.put("8", 267.0f);
        hueMap.put("9", 264.0f);
        hueMap.put("10", 0.0f);
        hueMap.put("11", 342.0f);
        hueMap.put("12", 125.0f);
        hueMap.put("14", 30.0f);
        hueMap.put("15", 3.0f);
        hueMap.put("16", 197.0f);
        hueMap.put("17", 315.0f);
        hueMap.put("18", 236.0f);
        hueMap.put("20", 135.0f);
        hueMap.put("21", 130.0f);
        hueMap.put("21A", 138.0f);
        hueMap.put("21W", 146.0f);
        hueMap.put("CMS", 154.0f);
        hueMap.put("21G", 162.0f);
        hueMap.put("21H", 170.0f);
        hueMap.put("21L", 178.0f);
        hueMap.put("21M", 186.0f);
        hueMap.put("WGS", 194.0f);
        hueMap.put("22", 0.0f);
        hueMap.put("24", 260.0f);
        hueMap.put("CC", 115.0f);
        hueMap.put("CSB", 197.0f);
        hueMap.put("EC", 100.0f);
        hueMap.put("EM", 225.0f);
        hueMap.put("ES", 242.0f);
        hueMap.put("HST", 218.0f);
        hueMap.put("IDS", 150.0f);
        hueMap.put("MAS", 122.0f);
        hueMap.put("SCM", 138.0f);
        hueMap.put("STS", 276.0f);
        hueMap.put("SWE", 13.0f);
        hueMap.put("SP", 240.0f);
    }

    private static Map<String, Integer> svMap;
    static {
        svMap = new HashMap<String, Integer>();
        svMap.put("1", 0);
        svMap.put("2", 0);
        svMap.put("3", 0);
        svMap.put("4", 1);
        svMap.put("5", 0);
        svMap.put("6", 0);
        svMap.put("7", 2);
        svMap.put("8", 2);
        svMap.put("9", 0);
        svMap.put("10", 2);
        svMap.put("11", 1);
        svMap.put("12", 0);
        svMap.put("14", 0);
        svMap.put("15", 1);
        svMap.put("16", 0);
        svMap.put("17", 0);
        svMap.put("18", 1);
        svMap.put("20", 2);
        svMap.put("21", 2);
        svMap.put("21A", 2);
        svMap.put("21W", 2);
        svMap.put("CMS", 2);
        svMap.put("21G", 2);
        svMap.put("21H", 2);
        svMap.put("21L", 2);
        svMap.put("21M", 2);
        svMap.put("WGS", 2);
        svMap.put("22", 1);
        svMap.put("24", 1);
        svMap.put("CC", 0);
        svMap.put("CSB", 2);
        svMap.put("EC", 1);
        svMap.put("EM", 1);
        svMap.put("ES", 1);
        svMap.put("HST", 1);
        svMap.put("IDS", 1);
        svMap.put("MAS", 2);
        svMap.put("SCM", 1);
        svMap.put("STS", 2);
        svMap.put("SWE", 2);
        svMap.put("SP", 0);
    }

    // For GIRs, CIs, etc.
    private static Map<String, Integer> specialColors;
    static {
        float saturation = 0.7f;
        float brightness = 0.75f;
        specialColors = new HashMap<>();
        specialColors.put("GIR", Color.HSVToColor(new float[] { 18.0f, saturation, brightness }));
        specialColors.put("HASS", Color.HSVToColor(new float[] { 162.0f, saturation, brightness }));
        specialColors.put("HASS-A", Color.HSVToColor(new float[] { 198.0f, saturation, brightness }));
        specialColors.put("HASS-H", Color.HSVToColor(new float[] { 234.0f, saturation, brightness }));
        specialColors.put("HASS-S", Color.HSVToColor(new float[] { 270.0f, saturation, brightness }));
        specialColors.put("CI-H", Color.HSVToColor(new float[] { 306.0f, saturation, brightness }));
        specialColors.put("CI-HW", Color.HSVToColor(new float[] { 342.0f, saturation, brightness }));
    }

    private static float[] saturations = new float[] { 0.7f, 0.52f, 0.88f };
    private static float[] brightnesses = new float[] { 0.87f, 0.71f, 0.71f };

    public static int colorForCourse(Course course) {
        return colorForCourse(course, 0xFF);
    }

    public static int colorForCourse(Course course, int alpha) {
        if (course.getSubjectID() != null) {
            if (specialColors.containsKey(course.getSubjectID()))
                return specialColors.get(course.getSubjectID());
            else if (course.isGeneric)
                return specialColors.get("GIR");
        }
        if (course.getSubjectID() == null || !course.getSubjectID().contains("."))
            return Color.parseColor("#FFBBBBBB");
        String department = course.getSubjectID().substring(0, course.getSubjectID().indexOf('.'));
        return colorForDepartment(department, alpha);
    }

    public static int colorForDepartment(String department, int alpha) {
        if (!hueMap.containsKey(department)) {
            return Color.parseColor("#FFBBBBBB");
        }
        float hue = hueMap.get(department);
        float saturation = saturations[svMap.get(department)];
        float brightness = brightnesses[svMap.get(department)];
        return Color.HSVToColor(alpha, new float[] {hue, saturation, brightness});
    }

    public static int darkenColor(int color, int alpha) {
        float[] values = new float[] { 0.0f, 0.0f, 0.0f };
        Color.colorToHSV(color, values);
        return Color.HSVToColor(alpha, new float[] {values[0], values[1], values[2] * 0.8f});
    }

    public static int lightenColor(int color, int alpha) {
        float[] values = new float[] { 0.0f, 0.0f, 0.0f };
        Color.colorToHSV(color, values);
        return Color.HSVToColor(alpha, new float[] {values[0], values[1], Math.min(1.0f, values[2] * 1.3f)});
    }
}
