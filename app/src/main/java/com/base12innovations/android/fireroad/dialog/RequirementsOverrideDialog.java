package com.base12innovations.android.fireroad.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.adapter.SearchResultsAdapter;
import com.base12innovations.android.fireroad.adapter.SelectCoursesAdapter;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.models.req.ProgressAssertion;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class RequirementsOverrideDialog extends DialogFragment implements SelectCoursesAdapter.Delegate{
    public interface RequirementsOverrideDialogDelegate{
        void requirementsOverrideDialogDismissed();
        void requirementsOverrideDialogEditOverride(boolean overridden, List<Course> courses);
        void requirementsOverrideDialogCourseClicked(Course course);
    }

    public RequirementsListStatement req;
    public ProgressAssertion progressAssertion;
    public RequirementsOverrideDialogDelegate delegate;
    public boolean overriding;
    public List<Course> replacementCourses;

    private WeakReference<AlertDialog> alertDialogWeakReference;

    private Switch switchOverride;
    private RecyclerView recyclerView;
    private SelectCoursesAdapter listAdapter;


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

        TextView titleText = view.findViewById(R.id.titleText);
        titleText.setText(String.format("Override %s", req.requirement));

        recyclerView = view.findViewById(R.id.recyclerViewCourses);
        listAdapter = new SelectCoursesAdapter(getContext(),null);
        listAdapter.delegate = new WeakReference<SelectCoursesAdapter.Delegate>(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(listAdapter);

        switchOverride = view.findViewById(R.id.switchOverride);
        switchOverride.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                overriding = isChecked;
                recyclerView.setEnabled(overriding);
                verifyValidOverride();
            }
        });

        List<Course> otherCourses = new ArrayList<>(replacementCourses);
        Log.d("Other Course", otherCourses.toString());
        List<List<Course>>allCourses = new ArrayList<>();
        for(int i = 0; i < RoadDocument.semesterNames.length;i++){
            List<Course> coursesForSemester = User.currentUser().getCurrentDocument().coursesForSemester(i);
            for(Course course : coursesForSemester){
                for(Course course2 : otherCourses){
                    if(course2.getSubjectID().equals(course.getSubjectID())){
                        otherCourses.remove(course2);
                    }
                }
                Log.d("Other Course", course.getSubjectID());
            }
            allCourses.add(coursesForSemester);
        }
        Log.d("Other Course", otherCourses.toString());
        allCourses.add(otherCourses);
        listAdapter.setCourses(allCourses);
        listAdapter.notifyDataSetChanged();

        switchOverride.setChecked(overriding);

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
        AlertDialog dialog = builder.create();
        alertDialogWeakReference = new WeakReference<>(dialog);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                verifyValidOverride();
            }
        });
        return dialog;
    }

    public void verifyValidOverride(){
        if(alertDialogWeakReference != null && alertDialogWeakReference.get() != null && alertDialogWeakReference.get().getButton(AlertDialog.BUTTON_POSITIVE)!=null) {
            if (!overriding || (overriding && !replacementCourses.isEmpty())) {
                alertDialogWeakReference.get().getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
            } else {
                alertDialogWeakReference.get().getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
            }
        }
    }

    public List<Course> getCourses(){
        return replacementCourses;
    }
    @Override
    public void selectCourseClickedCourse(Course selectedCourse){
        //delegate.requirementsOverrideDialogCourseClicked(selectedCourse);
    }
    @Override
    public void selectCourseAddCourse(Course selectedCourse){
        if(!replacementCourses.contains(selectedCourse)) {
            replacementCourses.add(selectedCourse);
        }
        verifyValidOverride();
    }
    @Override
    public void selectCourseRemoveCourse(Course selectedCourse){
        replacementCourses.remove(selectedCourse);
        verifyValidOverride();
    }
    @Override
    public boolean selectCourseIsSelected(Course selectedCourse){
        return replacementCourses.contains(selectedCourse);
    }

}
