package com.base12innovations.android.fireroad.models;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequirementsListManager {
    private static RequirementsListManager _shared;
    public static String FILE_EXTENSION = ".reql";

    private RequirementsListManager() { }
    private Context context;

    private Map<String, RequirementsList> requirementsLists;

    public static RequirementsListManager sharedInstance() {
        if (_shared == null) {
            _shared = new RequirementsListManager();
        }
        return _shared;
    }

    public void initialize(Context context) {
        this.context = context;
        loadRequirementsFiles();
    }

    private void loadRequirementsFiles() {
        requirementsLists = new HashMap<>();
        for (File reqFile : getRequirementsDirectory().listFiles()) {
            RequirementsList newReqList = new RequirementsList(reqFile);
            requirementsLists.put(newReqList.listID, newReqList);
        }
    }

    public List<RequirementsList> getAllRequirementsLists() {
        List<String> sortedKeys = new ArrayList<>(requirementsLists.keySet());
        Collections.sort(sortedKeys);
        List<RequirementsList> result = new ArrayList<>();
        for (String listID : sortedKeys) {
            result.add(requirementsLists.get(listID));
        }
        return result;
    }

    private File getRequirementsDirectory() {
        File path = new File(context.getFilesDir(), "requirements");
        if (!path.exists()) {
            boolean success = path.mkdir();
            if (!success) {
                Log.d("RequirementsList", "Failed to create requirements directory");
            }
        }
        return path;
    }

    public File getPathForRequirementsFile(String fileName) {
        return new File(getRequirementsDirectory(), fileName);
    }

    public RequirementsList getRequirementsList(String listID) {
        if (requirementsLists != null && requirementsLists.containsKey(listID)) {
            return requirementsLists.get(listID);
        }
        return null;
    }
}
