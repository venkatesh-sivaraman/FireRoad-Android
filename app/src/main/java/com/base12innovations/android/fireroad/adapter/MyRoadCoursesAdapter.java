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
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.ColorManager;
import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.RoadDocument;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.util.List;

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

            final View warningView = view.findViewById(R.id.warningView);
            if (AppSettings.shared().getBoolean(AppSettings.HIDE_ALL_WARNINGS, false))
                warningView.setVisibility(View.GONE);
            else
                TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        int pos = viewHolder.getAdapterPosition();
                        final boolean showWarnings = document.warningsForCourse(course, semesterForGridPosition(pos)).size() > 0 && !document.overrideWarningsForCourse(course);
                        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                            @Override
                            public void perform() {
                                if (showWarnings)
                                    warningView.setVisibility(View.VISIBLE);
                                else
                                    warningView.setVisibility(View.GONE);
                            }
                        });
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
}