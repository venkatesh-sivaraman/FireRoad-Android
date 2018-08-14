package com.base12innovations.android.fireroad.models;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

public class ColorManager {
    /*
        static let departmentNumbers = [
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
                ]
    static let colorMapping: [String: UIColor] = {
        let baseValues: [(s: CGFloat, v: CGFloat)] = [
        (0.7, 0.87),
        (0.52, 0.71),
        (0.88, 0.71)
        ]
        let directive: [String: (h: CGFloat, base: Int)] = [
        "1": (0.0, 0), "2": (20.0, 0),
        "3": (225.0, 0), "4": (128.0, 1),
        "5": (162.0, 0), "6": (210.0, 0),
        "7": (218.0, 2), "8": (267.0, 2),
        "9": (264.0, 0), "10": (0.0, 2),
        "11": (342.0, 1), "12": (125.0, 0),
        "14": (30.0, 0), "15": (3.0, 1),
        "16": (197.0, 0), "17": (315.0, 0),
        "18": (236.0, 1), "20": (135.0, 2),
        "21": (130.0, 2), "21A": (138.0, 2),
        "21W": (146.0, 2), "CMS": (154.0, 2),
        "21G": (162.0, 2), "21H": (170.0, 2),
        "21L": (178.0, 2), "21M": (186.0, 2),
        "WGS": (194.0, 2), "22": (0.0, 1),
        "24": (260.0, 1), "CC": (115.0, 0),
        "CSB": (197.0, 2), "EC": (100.0, 1),
        "EM": (225.0, 1), "ES": (242.0, 1),
        "HST": (218.0, 1), "IDS": (150.0, 1),
        "MAS": (122.0, 2), "SCM": (138.0, 1),
        "STS": (276.0, 2), "SWE": (13.0, 2),
        "SP": (240.0, 0)
        ]
        var ret = directive.mapValues({ UIColor(hue: $0.h / 360.0, saturation: baseValues[$0.base].s, brightness: baseValues[$0.base].v, alpha: 1.0) })
        let saturation = CGFloat(0.7)
        let brightness = CGFloat(0.87)
        ret["GIR"] = UIColor(hue: 0.05, saturation: saturation * 0.75, brightness: brightness, alpha: 1.0)
        ret["HASS"] = UIColor(hue: 0.45, saturation: saturation * 0.75, brightness: brightness, alpha: 1.0)
        ret["HASS-A"] = UIColor(hue: 0.55, saturation: saturation * 0.75, brightness: brightness, alpha: 1.0)
        ret["HASS-H"] = UIColor(hue: 0.65, saturation: saturation * 0.75, brightness: brightness, alpha: 1.0)
        ret["HASS-S"] = UIColor(hue: 0.75, saturation: saturation * 0.75, brightness: brightness, alpha: 1.0)
        ret["CI-H"] = UIColor(hue: 0.85, saturation: saturation * 0.75, brightness: brightness, alpha: 1.0)
        ret["CI-HW"] = UIColor(hue: 0.95, saturation: saturation * 0.75, brightness: brightness, alpha: 1.0)
        return ret
    }()

            */
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

    private static float[] saturations = new float[] { 0.7f, 0.52f, 0.88f };
    private static float[] brightnesses = new float[] { 0.87f, 0.71f, 0.71f };

    public static int colorForCourse(Course course) {
        return colorForCourse(course, 0xFF);
    }

    public static int colorForCourse(Course course, int alpha) {
        String department = course.getSubjectID().substring(0, course.getSubjectID().indexOf('.'));
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
}
