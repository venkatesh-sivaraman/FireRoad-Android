package com.base12innovations.android.fireroad;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
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
        int barColor = ColorManager.colorForCourse(course, 0xFF);
        //ColorDrawable newColor = new ColorDrawable(barColor);
        CollapsingToolbarLayout toolbarLayout = findViewById(R.id.toolbar_layout);
        toolbarLayout.setBackgroundColor(barColor);
        toolbarLayout.setContentScrimColor(barColor);
        toolbarLayout.setStatusBarScrimColor(ColorManager.darkenColor(barColor, 0xFF));
        toolbar.setBackgroundColor(barColor);

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

        LinearLayout layout = contentView.findViewById(R.id.courseDetailsLinearLayout);
        addMetadataItem(layout, "Units", Integer.toString(course.getTotalUnits()));
        addMetadataItem(layout, "Hours", "This is a longer string that will hopefully take up multiple lines");
    }

    private void addMetadataItem(LinearLayout layout, String title, String value) {
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        View metadataView = LayoutInflater.from(this).inflate(R.layout.cell_course_details_metadata, null);
        layout.addView(metadataView);

        ((TextView)metadataView.findViewById(R.id.metadataTitle)).setText(title);
        ((TextView)metadataView.findViewById(R.id.metadataValue)).setText(value);
    }
}
