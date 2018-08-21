package com.base12innovations.android.fireroad;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.base12innovations.android.fireroad.models.ColorManager;
import com.base12innovations.android.fireroad.models.RequirementsList;
import com.base12innovations.android.fireroad.models.User;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RequirementsBrowserAdapter extends BaseAdapter implements SpinnerAdapter {

    private static String[] headers = new String[] {
            "MY COURSES", "MAJORS", "MINORS", "MASTERS", "OTHER"
    };
    private List<List<RequirementsList>> requirementsLists;
    private LayoutInflater inflater;

    private static int HEADER_VIEW = 0;
    private static int REQ_LIST_VIEW = 1;

    public int setRequirementsLists(List<RequirementsList> reqLists, int currentSelection) {
        RequirementsList currentReq = (RequirementsList)getItem(currentSelection);
        requirementsLists = sortRequirementsLists(reqLists);
        return indexOf(currentReq);
    }

    public RequirementsBrowserAdapter(Activity context, List<RequirementsList> requirementsLists) {
        this.requirementsLists = sortRequirementsLists(requirementsLists);
        inflater = context.getLayoutInflater();
    }

    private List<List<RequirementsList>> sortRequirementsLists(List<RequirementsList> input) {
        List<List<RequirementsList>> result = new ArrayList<>();
        for (String header : headers) {
            result.add(new ArrayList<RequirementsList>());
        }

        for (RequirementsList list : input) {
            int listToAdd = 4; // Other
            if (User.currentUser().getCurrentDocument() != null &&
                    User.currentUser().getCurrentDocument().coursesOfStudy.contains(list.listID))
                listToAdd = 0;
            else if (list.listID.contains("major"))
                listToAdd = 1;
            else if (list.listID.contains("minor"))
                listToAdd = 2;
            else if (list.listID.contains("master"))
                listToAdd = 3;
            result.get(listToAdd).add(list);
        }

        return result;
    }

    @Override
    public Object getItem(int i) {
        int cursor = 0;
        for (int k = 0; k < headers.length; k++) {
            List<RequirementsList> sublist = requirementsLists.get(k);
            if (sublist.size() == 0)
                continue;
            if (i == cursor)
                return headers[k];
            cursor += 1;
            if (i < cursor + sublist.size())
                return sublist.get(i - cursor);
            cursor += sublist.size();
        }
        return null;
    }

    public int indexOf(RequirementsList item) {
        int cursor = 0;
        for (int k = 0; k < headers.length; k++) {
            List<RequirementsList> sublist = requirementsLists.get(k);
            if (sublist.size() == 0)
                continue;
            cursor += 1;
            if (sublist.contains(item))
                return cursor + sublist.indexOf(item);
            cursor += sublist.size();
        }
        return -1;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getCount() {
        int cursor = 0;
        for (int k = 0; k < headers.length; k++) {
            List<RequirementsList> sublist = requirementsLists.get(k);
            if (sublist.size() == 0)
                continue;
            cursor += 1 + sublist.size();
        }
        return cursor;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position) instanceof String)
            return HEADER_VIEW;
        return REQ_LIST_VIEW;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View myView = view;
        if (myView == null) {
            myView = inflater.inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        }
        RequirementsList rList = (RequirementsList)getItem(i);
        ((TextView)myView.findViewById(android.R.id.text1)).setText(rList.mediumTitle != null ? rList.mediumTitle : rList.title);
        return myView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View myView = convertView;
        int viewType = getItemViewType(position);
        if (viewType == HEADER_VIEW && (myView == null || myView.findViewById(R.id.headerLabel) == null))
            myView = inflater.inflate(R.layout.cell_requirements_browse_header, parent, false);
        else if (viewType == REQ_LIST_VIEW && (myView == null || myView.findViewById(R.id.progressLabel) == null))
            myView = inflater.inflate(R.layout.cell_requirements_browser, parent, false);
        myView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, myView.getLayoutParams().height));
        if (viewType == HEADER_VIEW)
            ((TextView)myView.findViewById(R.id.headerLabel)).setText((String)getItem(position));
        else
            formatView(myView, position, false);
        return myView;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != HEADER_VIEW;
    }

    private Set<String> computedListIDs;

    public void resetRequirementsCache() {
        computedListIDs = null;
    }

    private void formatView(final View myView, int position, boolean hideColorView) {
        final RequirementsList rList = (RequirementsList)getItem(position);
        if (rList == null) {
            return;
        }
        ((TextView)myView.findViewById(R.id.subjectIDLabel)).setText(shortTitleForRequirementsList(rList));
        ((TextView)myView.findViewById(R.id.subjectTitleLabel)).setText(rList.titleNoDegree != null ? rList.titleNoDegree : rList.title);
        String dept = rList.shortTitle;
        String[] comps = dept.split("[^A-z0-9]");
        View colorView = myView.findViewById(R.id.colorCodingView);
        if (hideColorView)
            colorView.setVisibility(View.GONE);
        else
            colorView.setBackgroundColor(ColorManager.colorForDepartment(comps[0], 0xFF));

        updateProgressLabel(myView, rList);
    }

    private static String shortTitleForRequirementsList(RequirementsList rList) {
        return rList.mediumTitle != null ? rList.mediumTitle : rList.shortTitle;
    }

    private void updateProgressLabel(final View metadataView, final RequirementsList rList) {
        if (computedListIDs == null)
            computedListIDs = new HashSet<>();
        if (computedListIDs.contains(rList.listID)) {
            updateProgressLabelPrecomputed(metadataView, rList.percentageFulfilled());
            return;
        }
        computedListIDs.add(rList.listID);
        metadataView.findViewById(R.id.progressLabel).setVisibility(View.GONE);
        TaskDispatcher.perform(new TaskDispatcher.Task<Float>() {
            @Override
            public Float perform() {
                if (User.currentUser().getCurrentDocument() != null) {
                    rList.loadIfNeeded();
                    rList.computeRequirementStatus(User.currentUser().getCurrentDocument().getAllCourses());
                    return rList.percentageFulfilled();
                }
                return 0.0f;
            }
        }, new TaskDispatcher.CompletionBlock<Float>() {
            @Override
            public void completed(Float progress) {
                // If the view has moved on, don't update
                if (!((TextView)metadataView.findViewById(R.id.subjectIDLabel)).getText().equals(shortTitleForRequirementsList(rList)))
                    return;
                updateProgressLabelPrecomputed(metadataView, progress);
            }
        });
    }

    private void updateProgressLabelPrecomputed(View metadataView, float progress) {
        if (progress <= 0.5f) {
            metadataView.findViewById(R.id.progressLabel).setVisibility(View.GONE);
        } else {
            TextView progressLabel = metadataView.findViewById(R.id.progressLabel);
            progressLabel.setVisibility(View.VISIBLE);
            progressLabel.setText(String.format(Locale.US, "%d%%", (int)Math.round(progress)));
            int color = Color.HSVToColor(new float[] { 1.8f * progress, 0.5f, 0.8f });
            ((GradientDrawable)progressLabel.getBackground()).setColor(color);
        }
    }
}
