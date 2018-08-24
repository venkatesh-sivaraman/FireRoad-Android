package com.base12innovations.android.fireroad.models.req;

import android.content.Context;
import android.util.Log;

import com.base12innovations.android.fireroad.utils.AlphanumComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    public void loadRequirementsFiles() {
        requirementsLists = new HashMap<>();
        for (File reqFile : getRequirementsDirectory().listFiles()) {
            RequirementsList newReqList = new RequirementsList(reqFile);
            requirementsLists.put(newReqList.listID, newReqList);
        }
    }

    public List<RequirementsList> getAllRequirementsLists() {
        List<RequirementsList> result = new ArrayList<>(requirementsLists.values());
        final Comparator<String> comp = new AlphanumComparator();
        Collections.sort(result, new Comparator<RequirementsList>() {
            @Override
            public int compare(RequirementsList item1, RequirementsList item2) {
                return comp.compare(item1.mediumTitle != null ? item1.mediumTitle : item1.shortTitle,
                        item2.mediumTitle != null ? item2.mediumTitle : item2.shortTitle);
            }
        });
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
