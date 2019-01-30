package com.base12innovations.android.fireroad.adapter;

import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;

/**
 * Abstract wrapper for showing a collection of courses in a recycler view.
 */
public abstract class CourseCollectionAdapter extends RecyclerView.Adapter<CourseCollectionAdapter.ViewHolder> {

    public interface ClickListener {
        void onClick(Course course, int position, View view);
    }

    public interface HeaderClickListener {
        void onHeaderButtonClick(int semester, View view);
    }

    public ClickListener itemClickListener;
    public ClickListener itemLongClickListener;
    public HeaderClickListener onHeaderClickListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View cellView;

        public ViewHolder(View v) {
            super(v);
            this.cellView = v;
        }
    }

    protected int numColumns;

    public CourseCollectionAdapter(int numColumns) {
        this.numColumns = numColumns;
    }

    public abstract Course courseForGridPosition(int position);

    public abstract boolean isSectionHeader(int position);

    public abstract int semesterForGridPosition(int position);

    public GridLayoutManager.SpanSizeLookup spanSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int i) {
                if (isSectionHeader(i)) {
                    return numColumns;
                } else {
                    return 1;
                }
            }
        };
    }

    @Override
    public int getItemViewType(int position) {
        return isSectionHeader(position) ? 1 : 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (i == 1) {   // Header
            final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
            View convertView = layoutInflater.inflate(R.layout.header_myroad, null);
            ViewHolder vh = new ViewHolder(convertView);
            return vh;
        } else {    // Normal cell
            final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
            View convertView = layoutInflater.inflate(R.layout.linearlayout_course, null);
            ViewHolder vh = new ViewHolder(convertView);
            return vh;
        }
    }

    public abstract void formatSectionHeader(final View view, int semester);

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        final View view = viewHolder.cellView;
        if (isSectionHeader(position)) {
            final int semester = semesterForGridPosition(position);
            formatSectionHeader(view, semester);
            if (onHeaderClickListener != null) {
                view.findViewById(R.id.moreButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onHeaderClickListener.onHeaderButtonClick(semester, view.findViewById(R.id.moreButton));
                    }
                });
            }
        } else {
            final Course course = courseForGridPosition(position);
            ((GradientDrawable)view.getBackground()).setColor(ColorManager.colorForCourse(course));

            final TextView idTextView = (TextView)view.findViewById(R.id.subjectIDLabel);
            final TextView titleTextView = (TextView)view.findViewById(R.id.subjectTitleLabel);
            idTextView.setText(course.getSubjectID());
            titleTextView.setText(course.subjectTitle);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (itemClickListener != null) {
                        int newPos = viewHolder.getAdapterPosition();
                        itemClickListener.onClick(course, newPos, view);
                    }
                }
            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (itemLongClickListener != null) {
                        int newPos = viewHolder.getAdapterPosition();
                        itemLongClickListener.onClick(course, newPos, view);
                        return true;
                    }
                    return false;
                }
            });

            updateCourseDecorations(viewHolder);
        }
    }

    // Update warning view and marker view decorations for the course cell
    public abstract void updateCourseDecorations(final ViewHolder viewHolder);

    @Override
    public abstract int getItemCount();
}
