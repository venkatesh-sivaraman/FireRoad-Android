package com.base12innovations.android.fireroad;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
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

public class MyRoadCoursesAdapter extends BaseAdapter {

    private RoadDocument document;

    public void setDocument(RoadDocument document) {
        this.document = document;
        notifyDataSetChanged();
    }

    private final Context context;
    private int numColumns;
    private static int SECTION_SPACING = -100;
    private static int SECTION_END_SPACE = -200;

    private int sectionHeaderIndex(int code) {
        return -(code + 1);
    }

    private int codeForSectionHeader(int headerIndex) {
        return -headerIndex - 1;
    }

    private int courseSemesterIndex(int code) {
        return code / 100;
    }

    private int courseInnerIndex(int code) {
        return code % 100;
    }

    private int codeForCourse(int semester, int position) {
        return semester * 100 + position;
    }

    private List<Integer> cellTypes;

    private void computeCellTypes(int numColumns) {
        cellTypes = new ArrayList<>();
        if (document == null) {
            return;
        }
        for (int i = 0; i < RoadDocument.semesterNames.length; i++) {
            List<Course> semesterCourses = document.coursesForSemester(i);

            while (cellTypes.size() % numColumns != 0) {
                cellTypes.add(SECTION_END_SPACE);
            }
            // Header
            cellTypes.add(codeForSectionHeader(i));
            while (cellTypes.size() % numColumns != 0) {
                cellTypes.add(SECTION_SPACING);
            }

            // Rest of cells
            for (int j = 0; j < semesterCourses.size(); j++) {
                cellTypes.add(codeForCourse(i, j));
            }
        }
    }

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

    public Course courseForGridPosition(int position) {
        int cellType = cellTypes.get(position);
        final Course course = document.coursesForSemester(courseSemesterIndex(cellType)).get(courseInnerIndex(cellType));
        return course;
    }

    public int semesterForGridPosition(int position) {
        int cellType = cellTypes.get(position);
        return courseSemesterIndex(cellType);
    }
}