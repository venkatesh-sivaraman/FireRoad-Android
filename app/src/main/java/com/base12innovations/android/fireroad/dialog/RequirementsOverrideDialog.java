package com.base12innovations.android.fireroad.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;

import java.util.List;

public class RequirementsOverrideDialog extends DialogFragment {
    public interface RequirementsOverrideDialogDelegate{
        void requirementsOverrideDialogDismissed();
        void requirementsOverrideDialogEditOverride(boolean overridden, List<Course> courses);

    }

    public RequirementsListStatement req;
    public RequirementsOverrideDialogDelegate delegate;

    private boolean overriding;

    public RequirementsOverrideDialog(){
        //required empty constructor
    }



    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_requirements_override_dialog, null);
        builder.setView(view);

        final Switch switchOverride = view.findViewById(R.id.switchOverride);
        switchOverride.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                overriding = isChecked;
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                delegate.requirementsOverrideDialogDismissed();
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                delegate.requirementsOverrideDialogEditOverride(overriding,getCourses());
            }
        });
        return builder.create();
    }

    public void setOverrideStatus(boolean overrideStatus){

    }


    public List<Course> getCourses(){
        return null;
    }


}
