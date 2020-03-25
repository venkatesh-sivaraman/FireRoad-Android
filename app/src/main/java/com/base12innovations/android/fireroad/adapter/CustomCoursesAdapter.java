package com.base12innovations.android.fireroad.adapter;

import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.Semester;
import com.base12innovations.android.fireroad.utils.ListHelper;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CustomCoursesAdapter extends CourseCollectionAdapter {

    private List<Course> courses;

    public CustomCoursesAdapter(int numColumns) {
        super(numColumns);
        reloadCourses();
    }

    public void reloadCourses() {
        courses = CourseManager.sharedInstance().getCustomCourses();
        Collections.sort(courses, new Comparator<Course>() {
            @Override
            public int compare(Course course1, Course course2) {
                return course1.getSubjectID().compareTo(course2.getSubjectID());
            }
        });
    }

    @Override public Course courseForGridPosition(int position) {
        return courses.get(position);
    }

    @Override public boolean isSectionHeader(int position) {
        return false;
    }

    @Override public Semester semesterForGridPosition(int position) {
        return new Semester(true);
    }

    @Override
    public void formatSectionHeader(View view, Semester semester) {
        // Never gets here
    }

    // Update warning view and marker view decorations for the course cell
    @Override
    public void updateCourseDecorations(final ViewHolder viewHolder) {
        if (viewHolder.getAdapterPosition() < 0 || viewHolder.getAdapterPosition() >= getItemCount() ||
                isSectionHeader(viewHolder.getAdapterPosition()))
            return;
        final View warningView = viewHolder.cellView.findViewById(R.id.warningView);
        final ImageView markerView = viewHolder.cellView.findViewById(R.id.markerView);

        warningView.setVisibility(View.GONE);
        markerView.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }
}
