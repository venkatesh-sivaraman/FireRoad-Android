package com.base12innovations.android.fireroad;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class CourseDetailsActivity extends AppCompatActivity {

    public static String COURSE_EXTRA = "CourseDetails_Course";
    public Course course;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the Intent that started this activity and extract the course
        Intent intent = getIntent();
        course = (Course)intent.getParcelableExtra(COURSE_EXTRA);

        setContentView(R.layout.activity_course_details);
        setupContentView(findViewById(R.id.content));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(course.getSubjectID());
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void setupContentView(View contentView) {
        TextView subjectTitleView = contentView.findViewById(R.id.detailsTitleView);
        subjectTitleView.setText(course.getSubjectTitle());
        TextView subjectDescriptionView = contentView.findViewById(R.id.detailsDescriptionView);
        subjectDescriptionView.setText(course.getSubjectDescription());
    }
}
