package com.base12innovations.android.fireroad;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.RoadDocument;
import com.base12innovations.android.fireroad.models.User;

import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddCourseDialog extends DialogFragment {

    public Course course;

    public interface AddCourseDialogDelegate {
        void addCourseDialogDismissed();
        void addCourseDialogAddedToSemester(Course course, int semester);
    }

    private Map<Integer, Integer> buttonSemesters;

    public AddCourseDialogDelegate delegate;

    public AddCourseDialog() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_course_dialog, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_add_course_dialog, null);
        builder.setView(view);

        buttonSemesters = new HashMap<>();
        buttonSemesters.put(R.id.button0, 0);
        buttonSemesters.put(R.id.button1, 1);
        buttonSemesters.put(R.id.button2, 2);
        buttonSemesters.put(R.id.button3, 3);
        buttonSemesters.put(R.id.button4, 4);
        buttonSemesters.put(R.id.button5, 5);
        buttonSemesters.put(R.id.button6, 6);
        buttonSemesters.put(R.id.button7, 7);
        buttonSemesters.put(R.id.button8, 8);
        buttonSemesters.put(R.id.button9, 9);
        buttonSemesters.put(R.id.button10, 10);
        buttonSemesters.put(R.id.button11, 11);
        buttonSemesters.put(R.id.button12, 12);
        buttonSemesters.put(R.id.button13, 13);
        buttonSemesters.put(R.id.button14, 14);
        buttonSemesters.put(R.id.button15, 15);

        RoadDocument doc = User.currentUser().getCurrentDocument();

        for (final Integer id : buttonSemesters.keySet()) {
            Button button = view.findViewById(id);
            Log.d("AddCourseDialog", doc.coursesForSemester(buttonSemesters.get(id)).toString());
            if (doc != null && doc.coursesForSemester(buttonSemesters.get(id)).contains(course)) {
                button.setEnabled(false);
                button.setAlpha(0.5f);
                button.setText("Added");
            } else {
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (delegate != null) {
                            delegate.addCourseDialogAddedToSemester(course, buttonSemesters.get(id));
                        }
                    }
                });
            }
        }

        ((TextView)view.findViewById(R.id.titleLabel)).setText("Add " + course.getSubjectID() + " to:");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (delegate != null) {
                    delegate.addCourseDialogDismissed();
                }
            }
        });
        return builder.create();
    }
}
