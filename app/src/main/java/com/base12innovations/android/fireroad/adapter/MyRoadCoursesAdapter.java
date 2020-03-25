package com.base12innovations.android.fireroad.adapter;

import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.Semester;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.util.List;
import java.util.Locale;

public class MyRoadCoursesAdapter extends CourseCollectionAdapter { //BaseAdapter {

    private RoadDocument document;

    public void setDocument(RoadDocument document) {
        this.document = document;
        notifyDataSetChanged();
    }

    public MyRoadCoursesAdapter(RoadDocument document, int numColumns) {
        super(numColumns);
        this.document = document;
    }

    @Override public Course courseForGridPosition(int position) {
        if (document == null) {
            return null;
        }
        int cursor = position;
        for (Semester semester : Semester.semesterNames.keySet()) {
            List<Course> semCourses = document.coursesForSemester(semester);
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

    public int headerPositionForSemester(Semester semester) {
        if (document == null) {
            return 0;
        }
        int cursor = 0;
        for (Semester otherSemester : Semester.semesterNames.keySet()) {
            if(!otherSemester.isBefore(semester))
                break;
            List<Course> semCourses = document.coursesForSemester(otherSemester);
            cursor += semCourses.size() + 1;
        }
        return cursor;
    }

    /**
     * Returns the index of the last course in the given semester.
     * @param semester the semester number.
     * @return an integer indicating the index of the last course.
     */
    public int lastPositionForSemester(Semester semester) {
        if (document == null) {
            return 0;
        }
        int cursor = 0;
        for (Semester otherSemester : Semester.semesterNames.keySet()) {
            if(!otherSemester.isBeforeOrEqual(otherSemester))
                break;
            List<Course> semCourses = document.coursesForSemester(otherSemester);
            cursor += semCourses.size() + 1;
        }
        return cursor - 1;
    }

    @Override public boolean isSectionHeader(int position) {
        if (document == null) {
            return false;
        }
        int cursor = position;
        for (Semester semester : Semester.semesterNames.keySet()) {
            List<Course> semCourses = document.coursesForSemester(semester);
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

    @Override public Semester semesterForGridPosition(int position) {
        if (document == null) {
            return new Semester(true);
        }
        int cursor = position;
        for (Semester semester : Semester.semesterNames.keySet()) {
            List<Course> semCourses = document.coursesForSemester(semester);
            if (cursor >= semCourses.size() + 1) {
                cursor -= semCourses.size() + 1;
            } else {
                return semester;
            }
        }
        return new Semester(true);
    }

    public int semesterPositionForGridPosition(int position) {
        if (document == null) {
            return 0;
        }
        int cursor = position;
        for (Semester semester : Semester.semesterNames.keySet()) {
            List<Course> semCourses = document.coursesForSemester(semester);
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
            Semester startSem = semesterForGridPosition(originalPos);
            int startPos = semesterPositionForGridPosition(originalPos);
            Semester endSem = semesterForGridPosition(finalPos);
            int endPos = semesterPositionForGridPosition(finalPos);
            if (endPos == -1) {
                // Hovering over a header
                endSem = endSem.prevSemester();
                if (!endSem.isValid()) {
                    return false;
                }
                // Move to last index in the previous semester
                endPos = document.coursesForSemester(endSem).size();
                if (endSem == startSem) {
                    // First index in next semester
                    endSem = endSem.nextSemester();
                    endPos = 0;
                }
            }
            if (startSem != endSem &&
                    document.coursesForSemester(endSem).contains(courseForGridPosition(originalPos))) {
                return false;
            }
            document.moveCourse(startSem, startPos, endSem, endPos);
            notifyItemMoved(originalPos, finalPos);
            return true;
        }
        return false;
    }

    @Override
    public void formatSectionHeader(View view, Semester semester) {
        final TextView textView = (TextView)view.findViewById(R.id.headerTextView);
        textView.setText(semester.toString());
        List<Course> courses = document.coursesForSemester(semester);
        TextView hoursView = view.findViewById(R.id.hoursTextView);
        if (courses.size() > 0 && !semester.isPriorCredit()) {
            int units = 0;
            double hours = 0.0;
            for (Course course : courses) {
                units += course.totalUnits;
                double courseHours = course.inClassHours + course.outOfClassHours;
                hours += (course.getQuarterOffered() != null && course.getQuarterOffered() != Course.QuarterOffered.WholeSemester) ? courseHours * 0.5 : courseHours;
            }
            hoursView.setText(String.format(Locale.US, "%d units, %.1f hours", units, hours));
        } else {
            hoursView.setText("");
        }

        view.findViewById(R.id.moreButton).setVisibility(document.coursesForSemester(semester).size() > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    // Update warning view and marker view decorations for the course cell
    @Override
    public void updateCourseDecorations(final ViewHolder viewHolder) {
        if (viewHolder.getAdapterPosition() < 0 || viewHolder.getAdapterPosition() >= getItemCount() ||
                isSectionHeader(viewHolder.getAdapterPosition()))
            return;
        final View warningView = viewHolder.cellView.findViewById(R.id.warningView);
        final ImageView markerView = viewHolder.cellView.findViewById(R.id.markerView);

        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                final Course course = courseForGridPosition(viewHolder.getAdapterPosition());
                if (AppSettings.shared().getBoolean(AppSettings.HIDE_ALL_WARNINGS, false) || course == null) {
                    TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                        @Override
                        public void perform() {
                            warningView.setVisibility(View.GONE);
                            if (course == null)
                                markerView.setVisibility(View.GONE);
                        }
                    });
                    if (course == null) // If non-null, we may want to add a marker, so don't return
                        return;
                }

                int pos = viewHolder.getAdapterPosition();
                final boolean showWarnings = document.warningsForCourse(course, semesterForGridPosition(pos)).size() > 0 && !document.overrideWarningsForCourse(course);
                final RoadDocument.SubjectMarker marker = document.subjectMarkerForCourse(course, semesterForGridPosition(pos));

                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        if (showWarnings)
                            warningView.setVisibility(View.VISIBLE);
                        else
                            warningView.setVisibility(View.GONE);

                        if (marker != null) {
                            markerView.setVisibility(View.VISIBLE);
                            markerView.setImageResource(marker.getImageResource());
                        } else
                            markerView.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        if (document == null) {
            return 0;
        }
        return document.getAllCourses().size() + Semester.semesterNames.size();
    }
}