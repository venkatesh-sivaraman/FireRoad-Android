package com.base12innovations.android.fireroad.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.utils.ListHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.text.TextUtils.join;


public class ConstrainDialogFragment extends DialogFragment {
    public Course course;

    public interface Delegate {
        void constrainDialogDismissed(ConstrainDialogFragment dialog);
        void constrainDialogFinished(ConstrainDialogFragment dialog, Map<String, List<Integer>> sections);
    }

    public Map<String, List<Integer>> sections;

    public Delegate delegate;

    public ConstrainDialogFragment() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_constrain_dialog, null);
        if (course != null)
            buildLayout((LinearLayout)view.findViewById(R.id.constrainLinearLayout));
        builder.setView(view);

        ((TextView)view.findViewById(R.id.titleLabel)).setText("Set sections for " + course.getSubjectID());
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (delegate != null) {
                    delegate.constrainDialogDismissed(ConstrainDialogFragment.this);
                }
            }
        });
        builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (delegate != null) {
                    delegate.constrainDialogFinished(ConstrainDialogFragment.this, sections);
                }
            }
        });
        return builder.create();
    }

    private void buildLayout(LinearLayout layout) {
        fillSections();

        for (final String sectionType : Course.ScheduleType.ordering) {
            if (!course.getSchedule().containsKey(sectionType)) continue;
            List<List<Course.ScheduleItem>> items = course.getSchedule().get(sectionType);

            TextView heading = new TextView(getContext());
            heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
            heading.setTypeface(heading.getTypeface(), Typeface.BOLD);
            heading.setText(sectionType);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layout.addView(heading, params);

            for (int i = 0; i < items.size(); i++) {
                List<Course.ScheduleItem> itemSet = items.get(i);

                String desc = TextUtils.join(", ", ListHelper.map(itemSet, new ListHelper.Function<Course.ScheduleItem, String>() {
                    @Override
                    public String apply(Course.ScheduleItem scheduleItem) {
                        return scheduleItem.toString(false);
                    }
                }));
                String location = null;
                for (Course.ScheduleItem item : itemSet) {
                    if (item.location != null) {
                        location = item.location;
                        break;
                    }
                }
                if (location != null)
                    desc += " (" + location + ")";

                CheckBox cb = new CheckBox(getContext());
                cb.setChecked(isEnabled(sectionType, i));
                cb.setText(desc);
                cb.setId(i);
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        setEnabled(sectionType, compoundButton.getId(), checked);
                        if (!hasAllowedSection(sectionType)) {
                            setEnabled(sectionType, compoundButton.getId(), true);
                            compoundButton.setChecked(true);
                        }
                    }
                });
                layout.addView(cb, params);
            }
        }
    }

    private void fillSections() {
        if (sections == null)
            sections = new HashMap<>();

        for (String sectionType : Course.ScheduleType.ordering) {
            if (!course.getSchedule().containsKey(sectionType)) continue;

            if (!sections.containsKey(sectionType))
                sections.put(sectionType, new ArrayList<Integer>());

            if (sections.get(sectionType).size() == 0) {
                for (int i = 0; i < course.getSchedule().get(sectionType).size(); i++) {
                    sections.get(sectionType).add(i);
                }
            }
        }
    }

    private boolean isEnabled(String sectionType, int index) {
        if (sections == null ||
                !sections.containsKey(sectionType) ||
                sections.get(sectionType).size() == 0) {
            return true;
        }
        return sections.get(sectionType).contains(index);
    }

    private boolean hasAllowedSection(String sectionType) {
        if (sections == null || !sections.containsKey(sectionType))
            return true;
        return sections.get(sectionType).size() > 0;
    }

    private void setEnabled(String sectionType, int index, boolean flag) {
        if (flag) {
            if (!sections.get(sectionType).contains(index))
                sections.get(sectionType).add(index);
        } else {
            if (sections.get(sectionType).contains(index))
                sections.get(sectionType).remove(Integer.valueOf(index));
        }
    }
}
