package com.base12innovations.android.fireroad.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.RoadDocument;

import java.util.ArrayList;
import java.util.List;


public class CourseWarningsDialogFragment extends DialogFragment {
    public Course course;
    public List<RoadDocument.Warning> warnings;

    public interface Delegate {
        void warningsDialogDismissed(CourseWarningsDialogFragment dialog);
        void warningsDialogSetOverride(CourseWarningsDialogFragment dialog, Course course, boolean override);
    }

    public boolean override;

    public Delegate delegate;

    public CourseWarningsDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_warnings_dialog, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_warnings_dialog, null);
        if (course != null) {
            ((TextView)view.findViewById(R.id.titleLabel)).setText("Warnings for " + course.getSubjectID());

            List<String> comps = new ArrayList<>();
            for (RoadDocument.Warning warning : warnings) {
                String base = "<b>" + warning.type.toString() + "</b> - ";
                switch (warning.type) {
                    case NOT_OFFERED:
                        base += "According to the course catalog, " + course.getSubjectID() + " is not offered in " + warning.semester + ".";
                        break;
                    case UNSATISFIED_COREQ:
                    case UNSATISFIED_PREREQ:
                        base += "The following requisites are not yet fulfilled: " + TextUtils.join(", ", warning.courses) + ".";
                        break;
                    default:
                        break;
                }
                comps.add(base);
            }
            ((TextView)view.findViewById(R.id.detailLabel)).setText(Html.fromHtml(TextUtils.join("<br/><br/>", comps), Html.FROM_HTML_MODE_LEGACY));
            Switch mSwitch = view.findViewById(R.id.overrideSwitch);
            mSwitch.setChecked(override);
            mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (delegate != null) {
                        delegate.warningsDialogSetOverride(CourseWarningsDialogFragment.this, course, b);
                    }
                }
            });
        }
        builder.setView(view);

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (delegate != null) {
                    delegate.warningsDialogDismissed(CourseWarningsDialogFragment.this);
                }
            }
        });
        return builder.create();
    }
}
