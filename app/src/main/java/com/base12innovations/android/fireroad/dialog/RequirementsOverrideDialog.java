package com.base12innovations.android.fireroad.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.adapter.SelectCoursesAdapter;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.models.req.ProgressAssertion;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequirementsOverrideDialog extends DialogFragment implements SelectCoursesAdapter.Delegate{
    public interface RequirementsOverrideDialogDelegate{
        void requirementsOverrideDialogDismissed();
        void requirementsOverrideDialogEditOverride(boolean overridden, List<Course> courses);
    }

    public RequirementsListStatement req;
    public ProgressAssertion progressAssertion;
    public RequirementsOverrideDialogDelegate delegate;
    public boolean isEditing;
    public List<Course> replacementCourses;

    private WeakReference<AlertDialog> alertDialogWeakReference;

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
        titleText.setText(String.format("Substitute course(s) for %s", req.requirement));

        recyclerView = view.findViewById(R.id.recyclerViewCourses);
        listAdapter = new SelectCoursesAdapter(getContext(),null);
        listAdapter.delegate = new WeakReference<SelectCoursesAdapter.Delegate>(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(listAdapter);

        /*switchOverride = view.findViewById(R.id.switchOverride);
        switchOverride.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                overriding = isChecked;
                recyclerView.setEnabled(overriding);
                verifyValidOverride();
            }
        });*/

        // otherCourses should be the courses that aren't currently in the road, but have been
        // marked as substituted
        Set<Course> otherCourses = new HashSet<>(replacementCourses);
        List<List<Course>> allCourses = new ArrayList<>();
        for (int i = 0; i < RoadDocument.semesterNames.length;i++) {
            List<Course> coursesForSemester = User.currentUser().getCurrentDocument().coursesForSemester(i);
            for (Course course : coursesForSemester) {
                otherCourses.remove(course);
            }
            allCourses.add(coursesForSemester);
        }
        allCourses.add(new ArrayList<>(otherCourses));
        listAdapter.setCourses(allCourses);
        listAdapter.notifyDataSetChanged();

        builder.setNegativeButton(isEditing?"Cancel Edits":"Cancel Override", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                delegate.requirementsOverrideDialogDismissed();
            }
        });
        builder.setPositiveButton(isEditing?"Confirm Edits":"Confirm Override", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                delegate.requirementsOverrideDialogEditOverride(true,getCourses());
            }
        });
        AlertDialog dialog = builder.create();
        alertDialogWeakReference = new WeakReference<>(dialog);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialogWeakReference.get().getButton(DialogInterface.BUTTON_POSITIVE).setTextScaleX(0.9f);
                alertDialogWeakReference.get().getButton(DialogInterface.BUTTON_NEGATIVE).setTextScaleX(0.9f);
                verifyValidOverride();
            }
        });
        return dialog;
    }

    public void verifyValidOverride(){
        if(alertDialogWeakReference != null && alertDialogWeakReference.get() != null && alertDialogWeakReference.get().getButton(AlertDialog.BUTTON_POSITIVE)!=null) {
            alertDialogWeakReference.get().getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!replacementCourses.isEmpty());
        }
    }

    public List<Course> getCourses(){
        return replacementCourses;
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
