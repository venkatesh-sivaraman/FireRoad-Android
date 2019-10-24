package com.base12innovations.android.fireroad.dialog;

import android.support.v4.app.DialogFragment;

import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;

public class RequirementsOverrideDialog extends DialogFragment {
    public interface RequirementsOverrideDialogDelegate{
        void requirementsOverrideDialogDismissed(Course course);
    }

    private RequirementsListStatement req;

    public RequirementsOverrideDialog(){

    }

}
