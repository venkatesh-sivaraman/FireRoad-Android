package com.base12innovations.android.fireroad;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class SearchResultsAdapter extends BaseAdapter {

    public interface Delegate {
        void searchResultsClickedAddButton(Course selectedCourse);
    }

    public Delegate delegate;

    private List<Course> courses;
    private Context context;

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> newCourses) {
        courses = newCourses;
        notifyDataSetChanged();
    }

    public SearchResultsAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses;
    }

    @Override
    public Object getItem(int i) {
        return courses.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getCount() {
        if (courses == null) {
            return 0;
        }
        return courses.size();
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(R.layout.cell_search_result, null);
        }
        final Course course = (Course)getItem(i);
        ((TextView)view.findViewById(R.id.subjectIDLabel)).setText(course.getSubjectID());
        ((TextView)view.findViewById(R.id.subjectTitleLabel)).setText(course.subjectTitle);
        view.findViewById(R.id.colorCodingView).setBackgroundColor(ColorManager.colorForCourse(course, 0xFF));
        ImageButton addButton = view.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate != null) {
                    delegate.searchResultsClickedAddButton(course);
                }
            }
        });
        return view;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }
}
