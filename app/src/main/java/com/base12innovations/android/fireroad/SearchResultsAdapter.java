package com.base12innovations.android.fireroad;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.base12innovations.android.fireroad.models.ColorManager;
import com.base12innovations.android.fireroad.models.Course;

import java.lang.ref.WeakReference;
import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    public interface Delegate {
        void searchResultsClickedAddButton(Course selectedCourse);
        void searchResultsClickedCourse(Course selectedCourse);
    }

    public WeakReference<Delegate> delegate;

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
    public int getItemCount() {
        if (courses == null) {
            return 0;
        }
        return courses.size();
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View cellView;

        public ViewHolder(View v) {
            super(v);
            this.cellView = v;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View convertView = layoutInflater.inflate(R.layout.cell_search_result, viewGroup, false);
        ViewHolder vh = new ViewHolder(convertView);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final View view = viewHolder.cellView;
        final Course course = courses.get(i);
        ((TextView)view.findViewById(R.id.subjectIDLabel)).setText(course.getSubjectID());
        ((TextView)view.findViewById(R.id.subjectTitleLabel)).setText(course.subjectTitle);
        view.findViewById(R.id.colorCodingView).setBackgroundColor(ColorManager.colorForCourse(course, 0xFF));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate.get() != null) {
                    delegate.get().searchResultsClickedCourse(course);
                }
            }
        });
        ImageButton addButton = view.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate.get() != null) {
                    delegate.get().searchResultsClickedAddButton(course);
                }
            }
        });
    }
}
