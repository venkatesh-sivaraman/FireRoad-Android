package com.base12innovations.android.fireroad.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SelectCoursesAdapter extends RecyclerView.Adapter<SelectCoursesAdapter.ViewHolder> {

    public interface Delegate{
        void selectCourseClickedCourse(Course selectedCourse);
        void selectCourseAddCourse(Course selectedCourse);
        void selectCourseRemoveCourse(Course selectedCourse);
        boolean selectCourseIsSelected(Course selectedCourse);
    }

    public WeakReference<Delegate> delegate;

    private List<List<Course>> courses;
    private List<Integer> semesters;
    private List<Integer> courseIndices;
    private Context context;

    public List<List<Course>> getCourses(){
        return courses;
    }

    public void setCourses(List<List<Course>> courses){
        this.courses = courses;
        updateIndexInfo();
        notifyDataSetChanged();
    }

    public SelectCoursesAdapter(Context context, List<List<Course>> courses){
        this.context = context;
        this.courses = courses;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View convertView;
        if(courseIndices.get(i) == -1){
            convertView=layoutInflater.inflate(R.layout.header_myroad,viewGroup,false);
        }else {
            convertView=layoutInflater.inflate(R.layout.cell_select_course, viewGroup, false);
        }
        ViewHolder vh = new ViewHolder(convertView);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final View view = viewHolder.cellView;
        if(courseIndices.get(i) == -1){
            ((TextView) view.findViewById(R.id.headerTextView)).setText(RoadDocument.semesterNames[semesters.get(i)]);
            int units = 0;
            double hours = 0.0;
            for (Course course: courses.get(semesters.get(i))){
                units += course.totalUnits;
                double courseHours = course.inClassHours + course.outOfClassHours;
                hours += (course.getQuarterOffered() != null && course.getQuarterOffered() != Course.QuarterOffered.WholeSemester) ? courseHours * 0.5 : courseHours;
            }
            ((TextView) view.findViewById(R.id.hoursTextView)).setText(String.format(Locale.US, "%d units, %.1f hours", units, hours));
            view.findViewById(R.id.moreButton).setVisibility(View.INVISIBLE);
        }else {
            final Course course = courses.get(semesters.get(i)).get(courseIndices.get(i));
            ((TextView) view.findViewById(R.id.subjectIDLabel)).setText(course.getSubjectID());
            ((TextView) view.findViewById(R.id.subjectTitleLabel)).setText(course.subjectTitle);
            view.findViewById(R.id.colorCodingView).setBackgroundColor(ColorManager.colorForCourse(course, 0xFF));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (delegate.get() != null) {
                        delegate.get().selectCourseClickedCourse(course);
                    }
                }
            });
            CheckBox checkBox = view.findViewById(R.id.checkCourse);
            if(delegate != null && delegate.get().selectCourseIsSelected(course)){
                checkBox.setChecked(true);
            }
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (delegate.get() != null) {
                            delegate.get().selectCourseAddCourse(course);
                        }
                    } else {
                        if (delegate.get() != null) {
                            delegate.get().selectCourseRemoveCourse(course);
                        }
                    }
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemCount() {
        if(courses == null) {
            return 0;
        }else{
            return semesters.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public View cellView;

        public ViewHolder(View v){
            super(v);
            this.cellView = v;
        }
    }

    private void updateIndexInfo(){
        semesters = new ArrayList<>();
        courseIndices = new ArrayList<>();
        for (int i = 0; i < RoadDocument.semesterNames.length; i++) {
            if(courses.get(i).size()>0){
                semesters.add(i);
                courseIndices.add(-1);
                for (int j = 0; j < courses.get(i).size(); j++) {
                    semesters.add(i);
                    courseIndices.add(j);
                }
            }
        }
    }
}
