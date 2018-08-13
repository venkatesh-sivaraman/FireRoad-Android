package com.base12innovations.android.fireroad;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourseDetailsActivity extends AppCompatActivity implements AddCourseDialog.AddCourseDialogDelegate {

    public static String COURSE_EXTRA = "CourseDetails_Course";
    public Course course;
    private AddCourseDialog addCourseDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the Intent that started this activity and extract the course
        Intent intent = getIntent();
        final Course preCourse = (Course)intent.getParcelableExtra(COURSE_EXTRA);
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                course = CourseManager.sharedInstance().getSubjectByID(preCourse.getSubjectID());
                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        setupContentView(findViewById(R.id.content));
                    }
                });
            }
        });

        setContentView(R.layout.activity_course_details);
        //setupContentView(findViewById(R.id.content));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(course.getSubjectID());
        int barColor = ColorManager.colorForCourse(course, 0xFF);
        //ColorDrawable newColor = new ColorDrawable(barColor);
        CollapsingToolbarLayout toolbarLayout = findViewById(R.id.toolbar_layout);
        toolbarLayout.setBackgroundColor(barColor);
        toolbarLayout.setContentScrimColor(barColor);
        toolbarLayout.setStatusBarScrimColor(ColorManager.darkenColor(barColor, 0xFF));
        toolbar.setBackgroundColor(barColor);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               addCourse();
            }
        });
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

        List<String> subjectList = new ArrayList<>(); //course.getEquivalentSubjectsList();
        subjectList.add("8.021");
        subjectList.add("7.03");
        subjectList.add("18.03");
        subjectList.add("6.046");
        subjectList.add("8.03");
        Log.d("CourseDetails", Integer.toString(course.getEquivalentSubjects().length()));
        if (subjectList.size() > 0) {
            addHeaderItem(layout, "Equivalent Subjects");
            addCourseListItem(layout, subjectList);
        }
    }

    private void addMetadataItem(LinearLayout layout, String title, String value) {
        int margin = (int) CourseDetailsActivity.this.getResources().getDimension(R.dimen.course_details_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = LayoutInflater.from(this).inflate(R.layout.cell_course_details_metadata, null);
        layout.addView(metadataView);

        ((TextView)metadataView.findViewById(R.id.metadataTitle)).setText(title);
        ((TextView)metadataView.findViewById(R.id.metadataValue)).setText(value);
    }

    private void addHeaderItem(LinearLayout layout, String title) {
        int margin = (int) CourseDetailsActivity.this.getResources().getDimension(R.dimen.course_details_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, margin, margin, 0);
        View metadataView = LayoutInflater.from(this).inflate(R.layout.cell_course_details_header, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        ((TextView)metadataView.findViewById(R.id.headingTitle)).setText(title);
    }

    private void addCourseListItem(LinearLayout layout, final List<String> subjectIDs) {
        View listView = LayoutInflater.from(this).inflate(R.layout.cell_course_details_list, null);
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
                            int width = (int) CourseDetailsActivity.this.getResources().getDimension(R.dimen.course_cell_default_width);
                            int height = (int) CourseDetailsActivity.this.getResources().getDimension(R.dimen.course_cell_height);
                            int margin = (int) CourseDetailsActivity.this.getResources().getDimension(R.dimen.course_cell_spacing);
                            int elevation = (int) CourseDetailsActivity.this.getResources().getDimension(R.dimen.course_cell_elevation);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
                            params.setMargins(margin, margin, margin, margin);
                            View courseThumbnail = LayoutInflater.from(CourseDetailsActivity.this).inflate(R.layout.linearlayout_course, null);
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
        addCourseDialog.show(getSupportFragmentManager(), "AddCourseFragment");
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
            doc.addCourse(course, semester);
        }
        addCourseDialog.dismiss();
        Snackbar.make(findViewById(R.id.content), "Added " + course.getSubjectID(), Snackbar.LENGTH_LONG).show();
        addCourseDialog = null;
    }

    public void showCourseDetails(Course course) {

    }
}
