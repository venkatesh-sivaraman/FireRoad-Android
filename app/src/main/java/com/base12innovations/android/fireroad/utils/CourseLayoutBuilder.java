package com.base12innovations.android.fireroad.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;

import java.util.Locale;

public class CourseLayoutBuilder {

    private Context context;

    public int defaultMargin = 0;
    public boolean showHeadingTopMargin = false;

    private LayoutInflater inflater;
    private LayoutInflater getLayoutInflater() {
        if (inflater == null)
            inflater = LayoutInflater.from(context);
        return inflater;
    }

    public CourseLayoutBuilder(Context ctx) {
        this.context = ctx;
    }

    // Basic layouts

    public View addHeaderItem(LinearLayout layout, String title) {
        int margin = defaultMargin;

        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, showHeadingTopMargin ? margin : 0, margin, 0);

        View metadataView = getLayoutInflater().inflate(R.layout.cell_course_details_header, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        ((TextView)metadataView.findViewById(R.id.headingTitle)).setText(title);
        return metadataView;
    }

    public View addDescriptionItem(LinearLayout layout, String text) {
        int margin = defaultMargin;

        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);

        View metadataView = getLayoutInflater().inflate(R.layout.cell_course_details_description, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        ((TextView)metadataView.findViewById(R.id.descriptionLabel)).setText(text);
        return metadataView;
    }

    public View addMetadataItem(LinearLayout layout, String title, String value) {
        int margin = defaultMargin;

        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);

        View metadataView = getLayoutInflater().inflate(R.layout.cell_course_details_metadata, null);
        layout.addView(metadataView);

        ((TextView)metadataView.findViewById(R.id.metadataTitle)).setText(title);
        ((TextView)metadataView.findViewById(R.id.metadataValue)).setText(value);

        return metadataView;
    }

    public View addCenteredDescriptionItem(LinearLayout layout, String description) {
        int margin = defaultMargin;
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, margin, margin, margin);
        View metadataView = getLayoutInflater().inflate(R.layout.cell_course_details_description, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        TextView textView = metadataView.findViewById(R.id.descriptionLabel);
        textView.setText(description);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return metadataView;
    }

    public View addSubHeaderItem(LinearLayout layout, String title, float progress, float textSize) {
        int margin = defaultMargin;
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = getLayoutInflater().inflate(R.layout.cell_requirements_header, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        TextView titleView = (TextView)metadataView.findViewById(R.id.headingTitle);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        titleView.setText(title);
        updateSubHeaderProgress(metadataView, progress);

        return metadataView;
    }

    public void updateSubHeaderProgress(View metadataView, float progress) {
        if (progress <= 0.5f) {
            metadataView.findViewById(R.id.progressLabel).setVisibility(View.GONE);
        } else {
            TextView progressLabel = metadataView.findViewById(R.id.progressLabel);
            progressLabel.setVisibility(View.VISIBLE);
            progressLabel.setText(String.format(Locale.US, "%d%%", (int)progress));

            int color = Color.HSVToColor(new float[] { 1.8f * progress, 0.5f, 0.8f });
            ((GradientDrawable)progressLabel.getBackground()).setColor(color);
        }
    }

    // Card views

    public LinearLayout addCard(final LinearLayout layout, boolean nested, int rowIndex) {
        int margin = defaultMargin;

        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, margin, margin, margin / 2);

        final LinearLayout card = (LinearLayout)getLayoutInflater().inflate(R.layout.requirements_card, null);
        if (rowIndex == -1)
            layout.addView(card);
        else
            layout.addView(card, rowIndex);
        if (nested) {
            card.setBackgroundResource(R.drawable.requirements_nested_card_background);
            card.setElevation(0.0f);
            addCloseButtonItem(card, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    layout.removeView(card);
                }
            });
        }
        card.setLayoutParams(lparams);

        return card;
    }

    public LinearLayout addCard(LinearLayout layout) {
        return addCard(layout, false, -1);
    }

    // Specialized views

    public View addRatingItem(LinearLayout layout, String title, final Course course) {
        View metadataView = getLayoutInflater().inflate(R.layout.cell_course_details_rating, null);
        layout.addView(metadataView);

        ((TextView)metadataView.findViewById(R.id.metadataTitle)).setText(title);
        RatingBar ratingBar = metadataView.findViewById(R.id.ratingBar);
        int rating = CourseManager.sharedInstance().getRatingForCourse(course);
        if (rating != CourseManager.NO_RATING)
            ratingBar.setRating((float)(rating + 5) / 2.0f);
        else
            ratingBar.setRating(0.0f);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float newValue, boolean b) {
                CourseManager.sharedInstance().setRatingForCourse(course, (int)Math.round(newValue * 2.0f - 5.0f));
            }
        });
        return metadataView;
    }

    public View addFavoritesItem(LinearLayout layout, final Course course) {
        View metadataView = getLayoutInflater().inflate(R.layout.cell_course_details_toggle_button, null);
        layout.addView(metadataView);

        ToggleButton button = metadataView.findViewById(R.id.toggleButton);
        button.setChecked(CourseManager.sharedInstance().getFavoriteCourses().contains(course.getSubjectID()));
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    CourseManager.sharedInstance().addCourseToFavorites(course);
                else
                    CourseManager.sharedInstance().removeCourseFromFavorites(course);
            }
        });
        return metadataView;
    }

    public View addButtonItem(LinearLayout layout, String title, View.OnClickListener clickListener) {
        View metadataView = getLayoutInflater().inflate(R.layout.cell_course_details_button, null);
        layout.addView(metadataView);

        TextView button = metadataView.findViewById(R.id.buttonTitle);
        button.setText(title);
        metadataView.setOnClickListener(clickListener);
        return metadataView;
    }

    public View addEditTextItem(LinearLayout layout, String text, View.OnFocusChangeListener textViewCompletion) {
        int margin = defaultMargin;
        int height = (int) context.getResources().getDimension(R.dimen.course_details_edittext_height);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = getLayoutInflater().inflate(R.layout.cell_course_details_edittext, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        final EditText textView = metadataView.findViewById(R.id.editText);
        textView.setText(text);
        textView.setOnFocusChangeListener(textViewCompletion);
        return metadataView;
    }

    public void addCloseButtonItem(LinearLayout layout, View.OnClickListener listener) {
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        View buttonView = getLayoutInflater().inflate(R.layout.cell_course_details_close, null);
        layout.addView(buttonView);
        buttonView.setLayoutParams(lparams);

        ImageButton button = (ImageButton)buttonView.findViewById(R.id.closeButton);
        button.setOnClickListener(listener);
    }

    public View addToggleCourseItem(LinearLayout layout, String textOff, String textOn, boolean currentlyChecked, CompoundButton.OnCheckedChangeListener action) {
        int margin = defaultMargin;

        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = getLayoutInflater().inflate(R.layout.cell_course_details_toggle_button, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        ToggleButton button = metadataView.findViewById(R.id.toggleButton);
        button.setTextOff(textOff);
        button.setTextOn(textOn);
        button.setChecked(currentlyChecked);
        button.setOnCheckedChangeListener(action);
        return metadataView;
    }

    // Course list items

    public LinearLayout addCourseListItem(final LinearLayout layout) {
        View listView = getLayoutInflater().inflate(R.layout.cell_course_details_list, null);
        layout.addView(listView);
        return listView.findViewById(R.id.courseListLayout);
    }

    public View addCourseCell(LinearLayout layout, final Course course, View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        int width = (int) context.getResources().getDimension(R.dimen.course_cell_default_width);
        int height = (int) context.getResources().getDimension(R.dimen.course_cell_height);
        int margin = (int) context.getResources().getDimension(R.dimen.course_cell_spacing);
        int elevation = (int) context.getResources().getDimension(R.dimen.course_cell_elevation);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(margin, margin, margin, margin);
        final View courseThumbnail = getLayoutInflater().inflate(R.layout.linearlayout_course, null);
        layout.addView(courseThumbnail);
        courseThumbnail.setLayoutParams(params);
        courseThumbnail.setElevation(elevation);

        int color = ColorManager.colorForCourse(course);
        ProgressBar pBar = courseThumbnail.findViewById(R.id.requirementsProgressBar);
        pBar.setProgressTintList(ColorStateList.valueOf(ColorManager.darkenColor(color, 0xFF)));
        pBar.setProgressBackgroundTintList(ColorStateList.valueOf(ColorManager.lightenColor(color, 0xFF)));
        ((GradientDrawable)courseThumbnail.getBackground()).setColor(color);
        ((TextView) courseThumbnail.findViewById(R.id.subjectIDLabel)).setText(course.getSubjectID());
        ((TextView) courseThumbnail.findViewById(R.id.subjectTitleLabel)).setText(course.subjectTitle);

        courseThumbnail.setOnClickListener(clickListener);
        courseThumbnail.setOnLongClickListener(longClickListener);
        return courseThumbnail;
    }
}
