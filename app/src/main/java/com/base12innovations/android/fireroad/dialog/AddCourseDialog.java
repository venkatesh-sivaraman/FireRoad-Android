package com.base12innovations.android.fireroad.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.adapter.SelectCoursesAdapter;
import com.base12innovations.android.fireroad.adapter.SelectSemesterAdapter;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.Semester;
import com.base12innovations.android.fireroad.models.doc.User;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import static com.base12innovations.android.fireroad.CourseNavigatorDelegate.ADD_TO_SCHEDULE;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddCourseDialog extends DialogFragment implements SelectSemesterAdapter.Delegate{

    public Course course;

    public interface AddCourseDialogDelegate {
        void addCourseDialogDismissed();
        void addCourseDialogAddedToSemester(Course course, String semesterID);
    }
    private RoadDocument doc = User.currentUser().getCurrentDocument();

    public AddCourseDialogDelegate delegate;

    public AddCourseDialog() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_add_course_dialog, null);
        builder.setView(view);

        Button scheduleButton = (Button)view.findViewById(R.id.buttonSchedule);
        if (User.currentUser().getCurrentSchedule() != null &&
                User.currentUser().getCurrentSchedule().getCourses().contains(course)) {
            scheduleButton.setEnabled(false);
            scheduleButton.setAlpha(0.5f);
            scheduleButton.setText("Added to Schedule");
        }
        else
            scheduleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (delegate != null)
                        delegate.addCourseDialogAddedToSemester(course, ADD_TO_SCHEDULE);
                }
            });

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewSelectSemester);
        SelectSemesterAdapter listAdapter = new SelectSemesterAdapter();
        listAdapter.delegate = new WeakReference<SelectSemesterAdapter.Delegate>(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(listAdapter);

        final Button priorCreditButton = view.findViewById(R.id.button0);
        final Semester priorCreditSemester = new Semester(true);
        if(courseInSemester(priorCreditSemester)){
            priorCreditButton.setEnabled(false);
            priorCreditButton.setAlpha(0.5f);
            priorCreditButton.setText("Added");
        }else{
            if(courseNotOfferedInSemester(priorCreditSemester))
                priorCreditButton.setAlpha(0.5f);
            priorCreditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectSemester(priorCreditSemester);
                }
            });
        }
        listAdapter.numYears=4;
        listAdapter.notifyDataSetChanged();

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

    public void selectSemester(Semester semester){
        if(delegate!= null)
            delegate.addCourseDialogAddedToSemester(course,semester.semesterID());
    }

    public boolean courseInSemester(Semester semester){
        return doc != null && doc.coursesForSemester(semester).contains(course);
    }
    public boolean courseNotOfferedInSemester(Semester semester){
        return (semester.getSeason()== Semester.Season.Fall && !course.isOfferedFall) ||
                (semester.getSeason()== Semester.Season.IAP && !course.isOfferedIAP) ||
                (semester.getSeason()== Semester.Season.Spring && !course.isOfferedSpring)||
                (semester.getSeason()== Semester.Season.Summer && !course.isOfferedSummer);
    }
}
