package com.base12innovations.android.fireroad;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourseDetailsFragment extends Fragment implements BottomSheetNavFragment, AddCourseDialog.AddCourseDialogDelegate {

    public static String COURSE_EXTRA = "CourseDetails_Course";
    public Course course;
    private AddCourseDialog addCourseDialog;
    private View mContentView;
    private FloatingActionButton fab;
    public boolean canGoBack = false;

    public CourseDetailsFragment() {

    }

    public WeakReference<Delegate> delegate;

    public static CourseDetailsFragment newInstance(Course course) {
        CourseDetailsFragment fragment = new CourseDetailsFragment();
        fragment.course = course;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_course_details, container, false);

        mContentView = layout.findViewById(R.id.content);

        if (course != null) {
            setupContentView(mContentView);

            Toolbar toolbar = (Toolbar) layout.findViewById(R.id.toolbar);
            toolbar.setClickable(true);
            toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (delegate.get() != null) {
                        delegate.get().navFragmentClickedToolbar(CourseDetailsFragment.this);
                    }
                }
            });
            if (canGoBack) {
                toolbar.setNavigationIcon(R.drawable.back_icon);
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (delegate.get() != null) {
                            delegate.get().navFragmentWantsBack(CourseDetailsFragment.this);
                        }
                    }
                });
            }
            toolbar.setTitle(course.getSubjectID());
            int barColor = ColorManager.colorForCourse(course, 0xFF);
            toolbar.setBackgroundColor(barColor);
        } else {
            Log.d("CourseDetailsFragment", "Subject ID is null");
        }

        //setContentView(R.layout.fragment_course_details);
        //setupContentView(mContentView);

        //setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fab = (FloatingActionButton) layout.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCourse();
            }
        });

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        delegate = null;
    }

    public void scaleFAB(float newValue, boolean animated) {
        if (fab == null) {
            return;
        }
        if (animated) {
            ScaleAnimation scaler = new ScaleAnimation(fab.getScaleX(), newValue, fab.getScaleY(), newValue);
            scaler.setDuration(500);
            scaler.setFillAfter(true);
            fab.startAnimation(scaler);
        } else {
            fab.setScaleX(newValue);
            fab.setScaleY(newValue);
        }
    }

    public void scaleFAB(float newValue) {
        scaleFAB(newValue, false);
    }

    private void setupContentView(View contentView) {
        TextView subjectTitleView = contentView.findViewById(R.id.detailsTitleView);
        subjectTitleView.setText(course.subjectTitle);
        TextView subjectDescriptionView = contentView.findViewById(R.id.detailsDescriptionView);
        subjectDescriptionView.setText(course.subjectDescription);

        LinearLayout layout = contentView.findViewById(R.id.courseDetailsLinearLayout);

        String unitsString;
        if (course.variableUnits) {
            unitsString = "arranged";
        } else {
            unitsString = String.format(Locale.US, "%d total (%d-%d-%d)", course.totalUnits, course.lectureUnits, course.labUnits, course.preparationUnits);
        }
        if (course.hasFinal) {
            unitsString += "\nHas final";
        }
        if (course.pdfOption) {
            unitsString += "\n[P/D/F]";
        }
        addMetadataItem(layout, "Units", unitsString);

        List<String> offeredItems = new ArrayList<>();
        if (course.isOfferedFall) {
            offeredItems.add("fall");
        }
        if (course.isOfferedIAP) {
            offeredItems.add("IAP");
        }
        if (course.isOfferedSpring) {
            offeredItems.add("spring");
        }
        if (course.isOfferedSummer) {
            offeredItems.add("summer");
        }
        String offeredString = TextUtils.join(", ", offeredItems);
        if (offeredString.length() == 0) {
            offeredString = "information unavailable";
        }
        offeredString = offeredString.substring(0, 1).toUpperCase() + offeredString.substring(1);
        addMetadataItem(layout, "Offered", offeredString);

        String[] instructors = course.getInstructorsList();
        if (instructors.length > 0) {
            addMetadataItem(layout, "Instructor" + (instructors.length != 1 ? "s" : ""), TextUtils.join(", ", instructors));
        }

        if (course.enrollmentNumber > 0) {
            addMetadataItem(layout, "Average Enrollment", Integer.toString(course.enrollmentNumber));
        }

        addHeaderItem(layout, "Ratings");
        if (course.rating != 0.0) {
            addMetadataItem(layout, "Average Rating", Double.toString(course.rating));
        }
        if (course.inClassHours != 0.0 || course.outOfClassHours != 0.0) {
            addMetadataItem(layout, "Hours", String.format(Locale.US, "%.2g in class\n%.2g out of class", course.inClassHours, course.outOfClassHours));
        }
        addMetadataItem(layout, "My Rating", "Rating widget should go here");

        List<String> subjectList = course.getEquivalentSubjectsList();
        if (subjectList.size() > 0) {
            addHeaderItem(layout, "Equivalent Subjects");
            addCourseListItem(layout, subjectList);
        }

        subjectList = course.getJointSubjectsList();
        if (subjectList.size() > 0) {
            addHeaderItem(layout, "Joint Subjects");
            addCourseListItem(layout, subjectList);
        }

        subjectList = course.getMeetsWithSubjectsList();
        if (subjectList.size() > 0) {
            addHeaderItem(layout, "Meets With Subjects");
            addCourseListItem(layout, subjectList);
        }
    }

    private void addMetadataItem(LinearLayout layout, String title, String value) {
        int margin = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_details_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_metadata, null);
        layout.addView(metadataView);

        ((TextView)metadataView.findViewById(R.id.metadataTitle)).setText(title);
        ((TextView)metadataView.findViewById(R.id.metadataValue)).setText(value);
    }

    private void addHeaderItem(LinearLayout layout, String title) {
        int margin = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_details_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, margin, margin, 0);
        View metadataView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_header, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        ((TextView)metadataView.findViewById(R.id.headingTitle)).setText(title);
    }

    private void addCourseListItem(LinearLayout layout, final List<String> subjectIDs) {
        View listView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_list, null);
        layout.addView(listView);

        final LinearLayout listLayout = listView.findViewById(R.id.courseListLayout);
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                final List<Course> courses = new ArrayList<>();
                for (String subjectID : subjectIDs) {
                    Course newCourse = CourseManager.sharedInstance().getSubjectByID(subjectID);
                    if (newCourse != null) {
                        courses.add(newCourse);
                    }
                }

                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        for (final Course course : courses) {
                            int width = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_cell_default_width);
                            int height = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_cell_height);
                            int margin = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_cell_spacing);
                            int elevation = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_cell_elevation);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
                            params.setMargins(margin, margin, margin, margin);
                            View courseThumbnail = LayoutInflater.from(CourseDetailsFragment.this.getContext()).inflate(R.layout.linearlayout_course, null);
                            listLayout.addView(courseThumbnail);
                            courseThumbnail.setLayoutParams(params);
                            courseThumbnail.setElevation(elevation);
                            ((GradientDrawable)courseThumbnail.getBackground()).setColor(ColorManager.colorForCourse(course));
                            ((TextView) courseThumbnail.findViewById(R.id.subjectIDLabel)).setText(course.getSubjectID());
                            ((TextView) courseThumbnail.findViewById(R.id.subjectTitleLabel)).setText(course.subjectTitle);

                            courseThumbnail.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    showCourseDetails(course);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void addCourse() {
        addCourseDialog = new AddCourseDialog();
        addCourseDialog.course = course;
        addCourseDialog.delegate = this;
        FragmentActivity a = getActivity();
        if (a != null) {
              addCourseDialog.show(a.getSupportFragmentManager(), "AddCourseFragment");
        }
    }

    @Override
    public void addCourseDialogDismissed() {
        addCourseDialog.dismiss();
        addCourseDialog = null;
    }

    @Override
    public void addCourseDialogAddedToSemester(Course course, int semester) {
        RoadDocument doc = User.currentUser().getCurrentDocument();
        if (doc != null) {
            boolean worked = doc.addCourse(course, semester);
            if (worked && delegate.get() != null)
                delegate.get().navFragmentAddedCourse(this, course, semester);
        }
        addCourseDialog.dismiss();
        Snackbar.make(mContentView, "Added " + course.getSubjectID(), Snackbar.LENGTH_LONG).show();
        addCourseDialog = null;
    }

    public void showCourseDetails(Course course) {
        if (delegate.get() != null) {
            delegate.get().navFragmentWantsCourseDetails(this, course);
        }
    }
}
