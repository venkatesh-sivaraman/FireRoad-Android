package com.base12innovations.android.fireroad.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.dialog.RequirementsOverrideDialog;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.Semester;
import com.base12innovations.android.fireroad.models.doc.User;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SelectCoursesAdapter extends RecyclerView.Adapter<SelectCoursesAdapter.ViewHolder> {

    public interface Delegate{
        void selectCourseAddCourse(Course selectedCourse);
        void selectCourseRemoveCourse(Course selectedCourse);
        boolean selectCourseIsSelected(Course selectedCourse);
    }

    public WeakReference<Delegate> delegate;

    private Map<Semester,List<Course>> courses;
    private List<Semester> semesters;
    private List<Integer> courseIndices;
    private Context context;

    public Map<Semester,List<Course>> getCourses(){
        return courses;
    }

    public void setCourses(Map<Semester,List<Course>> courses){
        this.courses = courses;
        updateIndexInfo();
        notifyDataSetChanged();
    }

    public SelectCoursesAdapter(Context context, Map<Semester,List<Course>> courses){
        this.context = context;
        this.courses = courses;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View convertView;
        if(courseIndices.get(i) == -1){
            convertView=layoutInflater.inflate(R.layout.header_select_course,viewGroup,false);
            convertView.setBackgroundColor(Color.rgb(250,250,250));
        }else {
            convertView=layoutInflater.inflate(R.layout.cell_select_course, viewGroup, false);
            convertView.setBackgroundColor(Color.rgb(250,250,250));
        }
        ViewHolder vh = new ViewHolder(convertView);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final View view = viewHolder.cellView;
        if(courseIndices.get(i) == -1){
            view.findViewById(R.id.selectCourseHeaderTextView).setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            view.findViewById(R.id.selectCourseColorCodingView).setBackgroundColor(Color.rgb(128,128,128));
            if(semesters.get(i).getSeason() == null && semesters.get(i).getYear() == -1 && !semesters.get(i).isPriorCredit()){
                ((TextView) view.findViewById(R.id.selectCourseHeaderTextView)).setText(String.format(Locale.US,"Other Courses"));
            }else {
                ((TextView) view.findViewById(R.id.selectCourseHeaderTextView)).setText(semesters.get(i).toString());
            }
        }else {
            final Course course = courses.get(semesters.get(i)).get(courseIndices.get(i));
            ((TextView) view.findViewById(R.id.subjectIDLabel)).setText(course.getSubjectID());
            ((TextView) view.findViewById(R.id.subjectTitleLabel)).setText(course.subjectTitle);
            view.findViewById(R.id.colorCodingView).setBackgroundColor(ColorManager.colorForCourse(course, 0xFF));
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

    static class ViewHolder extends RecyclerView.ViewHolder{
        View cellView;

        ViewHolder(View v){
            super(v);
            this.cellView = v;
        }
    }

    private void updateIndexInfo(){
        semesters = new ArrayList<>();
        courseIndices = new ArrayList<>();
        for(Semester semester : User.currentUser().getCurrentDocument().getSemesters()){
            if(courses.get(semester).size()>0){
                semesters.add(semester);
                courseIndices.add(-1);
                for (int j = 0; j < courses.get(semester).size(); j++) {
                    semesters.add(semester);
                    courseIndices.add(j);
                }
            }
        }
    }
}
