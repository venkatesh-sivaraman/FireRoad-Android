package com.base12innovations.android.fireroad;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyRoadCoursesAdapter extends RecyclerView.Adapter<MyRoadCoursesAdapter.ViewHolder> { //BaseAdapter {

    public interface ClickListener {
        void onClick(Course course, int position, View view);
    }

    public ClickListener itemClickListener;
    public ClickListener itemLongClickListener;

    private RoadDocument document;

    public void setDocument(RoadDocument document) {
        this.document = document;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View cellView;

        public ViewHolder(View v) {
            super(v);
            this.cellView = v;
        }
    }

    private int numColumns;

    public MyRoadCoursesAdapter(RoadDocument document, int numColumns) {
        this.document = document;
        this.numColumns = numColumns;
    }

    public Course courseForGridPosition(int position) {
        if (document == null) {
            return null;
        }
        int cursor = position;
        for (int i = 0; i < RoadDocument.semesterNames.length; i++) {
            List<Course> semCourses = document.coursesForSemester(i);
            if (cursor >= semCourses.size() + 1) {
                cursor -= semCourses.size() + 1;
            } else {
                if (cursor == 0) {
                    // Header
                    return null;
                } else {
                    return semCourses.get(cursor - 1);
                }
            }
        }
        return null;
    }

    /**
     * Returns the index of the last course in the given semester.
     * @param semester the semester number.
     * @return an integer indicating the index of the last course.
     */
    public int lastPositionForSemester(int semester) {
        if (document == null) {
            return 0;
        }
        int cursor = 0;
        for (int i = 0; i <= semester; i++) {
            List<Course> semCourses = document.coursesForSemester(i);
            cursor += semCourses.size() + 1;
        }
        return cursor - 1;
    }

    public boolean isSectionHeader(int position) {
        if (document == null) {
            return false;
        }
        int cursor = position;
        for (int i = 0; i < RoadDocument.semesterNames.length; i++) {
            List<Course> semCourses = document.coursesForSemester(i);
            if (cursor >= semCourses.size() + 1) {
                cursor -= semCourses.size() + 1;
            } else {
                if (cursor == 0) {
                    // Header
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public int semesterForGridPosition(int position) {
        if (document == null) {
            return 0;
        }
        int cursor = position;
        for (int i = 0; i < RoadDocument.semesterNames.length; i++) {
            List<Course> semCourses = document.coursesForSemester(i);
            if (cursor >= semCourses.size() + 1) {
                cursor -= semCourses.size() + 1;
            } else {
                return i;
            }
        }
        return 0;
    }

    public int semesterPositionForGridPosition(int position) {
        if (document == null) {
            return 0;
        }
        int cursor = position;
        for (int i = 0; i < RoadDocument.semesterNames.length; i++) {
            List<Course> semCourses = document.coursesForSemester(i);
            if (cursor >= semCourses.size() + 1) {
                cursor -= semCourses.size() + 1;
            } else {
                return cursor - 1;
            }
        }
        return 0;
    }

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

    public boolean moveCourse(int originalPos, int finalPos) {
        if (document != null) {
            int startSem = semesterForGridPosition(originalPos);
            int startPos = semesterPositionForGridPosition(originalPos);
            int endSem = semesterForGridPosition(finalPos);
            int endPos = semesterPositionForGridPosition(finalPos);
            if (endPos == -1) {
                // Hovering over a header
                endSem -= 1;
                if (endSem < 0) {
                    return false;
                }
                // Move to last index in the previous semester
                endPos = document.coursesForSemester(endSem).size();
                if (endSem == startSem) {
                    // First index in next semester
                    endSem += 1;
                    endPos = 0;
                }
            }
            document.moveCourse(startSem, startPos, endSem, endPos);
            notifyItemMoved(originalPos, finalPos);
            return true;
        }
        return false;
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

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        final View view = viewHolder.cellView;
        if (isSectionHeader(position)) {
            final TextView textView = (TextView)view.findViewById(R.id.headerTextView);
            textView.setText(RoadDocument.semesterNames[semesterForGridPosition(position)]);
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
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (document == null) {
            return 0;
        }
        return document.getAllCourses().size() + RoadDocument.semesterNames.length;
    }

    /*private final Context context;

    public MyRoadCoursesAdapter(Context context, RoadDocument document, int numColumns) {
        this.document = document;
        this.context = context;
        this.numColumns = numColumns;
        computeCellTypes(numColumns);
    }

    @Override
    public void notifyDataSetChanged() {
        computeCellTypes(this.numColumns);
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        computeCellTypes(this.numColumns);
        super.notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        return cellTypes.size();
    }

    // 3
    @Override
    public long getItemId(int position) {
        return 0;
    }

    // 4
    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        int cellType = cellTypes.get(position);
        if (cellType == SECTION_SPACING || cellType == SECTION_END_SPACE) {
            // Empty view
            return 2;
        } else if (cellType < 0) {
            // Header
            return 1;
        }
        // Normal cell
        return 0;
    }

    @Override
    public boolean isEnabled(int position) {
        int cellType = cellTypes.get(position);
        return cellType >= 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int cellType = cellTypes.get(position);
        if (cellType == SECTION_SPACING || cellType == SECTION_END_SPACE) {
            // Empty view
            if (convertView == null) {
                convertView = new View(context);
            }
            int height;
            if (cellType == SECTION_END_SPACE) {
                height = (int) context.getResources().getDimension(R.dimen.course_cell_height);
            } else {
                height = (int) context.getResources().getDimension(R.dimen.my_road_header_height);
            }
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            convertView.setLayoutParams(layoutParams);
            return convertView;
        } else if (cellType < 0) {
            // Header
            if (convertView == null) {
                final LayoutInflater layoutInflater = LayoutInflater.from(context);
                convertView = layoutInflater.inflate(R.layout.header_myroad, null);
            }
            final TextView textView = (TextView)convertView.findViewById(R.id.headerTextView);
            textView.setText(RoadDocument.semesterNames[sectionHeaderIndex(cellType)]);
            return convertView;
        }
        // Normal cell

        final Course course = courseForGridPosition(position);

        // 2
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.linearlayout_course, null);
        }
        ((GradientDrawable)convertView.getBackground()).setColor(ColorManager.colorForCourse(course));

        // 3
        final TextView idTextView = (TextView)convertView.findViewById(R.id.subjectIDLabel);
        final TextView titleTextView = (TextView)convertView.findViewById(R.id.subjectTitleLabel);

        // 4
        idTextView.setText(course.getSubjectID());
        titleTextView.setText(course.getSubjectTitle());

        return convertView;
    }

    */
}