package com.base12innovations.android.fireroad.activity;

import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.doc.NetworkManager;
import com.base12innovations.android.fireroad.models.doc.User;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomCourseEditActivity extends AppCompatActivity {

    public static final String SUBJECT_ID_EXTRA = "com.base12innovations.android.fireroad.subject_id";
    public static final String SUBJECT_TITLE_EXTRA = "com.base12innovations.android.fireroad.subject_title";
    private Course course = null;
    private Course courseCopy = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_course_edit);
        if (getIntent().hasExtra(SUBJECT_ID_EXTRA) && getIntent().hasExtra(SUBJECT_TITLE_EXTRA)) {
            String subjectID = getIntent().getStringExtra(SUBJECT_ID_EXTRA);
            String title = getIntent().getStringExtra(SUBJECT_TITLE_EXTRA);
            course = CourseManager.sharedInstance().getCustomCourse(subjectID, title);
        }
        if (course == null) {
            course = new Course();
            course.setSubjectID("");
            course.subjectTitle = "";
            course.isOfferedFall = true;
            course.isOfferedIAP = true;
            course.isOfferedSpring = true;
            course.isOfferedSummer = true;
            course.isPublic = false;
            course.creator = AppSettings.shared().getString(AppSettings.RECOMMENDER_USER_ID, "");
            course.totalUnits = 0;
            course.inClassHours = 0.0f;
            course.outOfClassHours = 0.0f;
            course.customColor = "@1";
        }
        JSONObject json = course.toJSON();
        courseCopy = new Course();
        courseCopy.readJSON(json);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        EditText idView = findViewById(R.id.subjectIDField);
        idView.setText(courseCopy.getSubjectID());
        EditText titleView = findViewById(R.id.titleField);
        titleView.setText(courseCopy.subjectTitle);
        idView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                courseCopy.setSubjectID(editable.toString());
            }
        });
        formatTextViewForFocusChange(idView, "Enter short name...");

        titleView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                courseCopy.subjectTitle = editable.toString();
            }
        });
        formatTextViewForFocusChange(titleView, "Enter title...");

        EditText unitsView = findViewById(R.id.unitsField);
        unitsView.setText(String.format(Locale.US, "%d", courseCopy.totalUnits));
        unitsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    courseCopy.totalUnits = Integer.parseInt(editable.toString());
                } catch (NumberFormatException e) {
                    Log.d("CustomCourseAct", "Invalid input for units");
                }
            }
        });
        formatTextViewForFocusChange(unitsView, "Enter a number...");

        EditText inClassView = findViewById(R.id.inClassHoursField);
        inClassView.setText(String.format(Locale.US, "%.1f", courseCopy.inClassHours));
        inClassView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    courseCopy.inClassHours = Float.parseFloat(editable.toString());
                } catch (NumberFormatException e) {
                    Log.d("CustomCourseAct", "Invalid input for in class hours");
                }
            }
        });
        formatTextViewForFocusChange(inClassView, "Enter a number...");

        EditText outClassView = findViewById(R.id.outOfClassHoursField);
        outClassView.setText(String.format(Locale.US, "%.1f", courseCopy.outOfClassHours));
        outClassView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    courseCopy.outOfClassHours = Float.parseFloat(editable.toString());
                } catch (NumberFormatException e) {
                    Log.d("CustomCourseAct", "Invalid input for out of class hours");
                }
            }
        });
        formatTextViewForFocusChange(outClassView, "Enter a number...");

        formatColorButtons();
    }

    private void formatTextViewForFocusChange(final EditText textView, final String placeholder) {
        textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    textView.setHint(placeholder);
                } else {
                    textView.setHint("");
                }
            }
        });
    }

    private final int[] colorButtonTags = {
            R.id.colorButton1, R.id.colorButton2, R.id.colorButton3, R.id.colorButton4, R.id.colorButton5, R.id.colorButton6, R.id.colorButton7,
            R.id.colorButton11, R.id.colorButton12, R.id.colorButton13, R.id.colorButton14, R.id.colorButton15, R.id.colorButton16, R.id.colorButton17,
            R.id.colorButton21, R.id.colorButton22, R.id.colorButton23, R.id.colorButton24, R.id.colorButton25, R.id.colorButton26, R.id.colorButton27,
            R.id.colorButton31, R.id.colorButton32, R.id.colorButton33, R.id.colorButton34, R.id.colorButton35, R.id.colorButton36, R.id.colorButton37,
            R.id.colorButton41, R.id.colorButton42, R.id.colorButton43, R.id.colorButton44, R.id.colorButton45, R.id.colorButton46, R.id.colorButton47,
            R.id.colorButton51, R.id.colorButton52, R.id.colorButton53, R.id.colorButton54, R.id.colorButton55, R.id.colorButton56, R.id.colorButton57,
    };

    private void formatColorButtons() {
        for (int i = 0; i < colorButtonTags.length; i++) {
            ImageButton button = findViewById(colorButtonTags[i]);
            button.setElevation(4);
            final String label = "@" + Integer.toString(i);
            Drawable drawable = getResources().getDrawable(R.drawable.round_button).mutate();
            drawable.setColorFilter(ColorManager.colorByCustomColorLabel(label), PorterDuff.Mode.SRC_IN);
            button.setBackground(drawable);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    courseCopy.customColor = label;
                    updateColorButtons();
                }
            });
        }
        updateColorButtons();
    }

    private void updateColorButtons() {
        for (int i = 0; i < colorButtonTags.length; i++) {
            ImageButton button = findViewById(colorButtonTags[i]);
            String label = "@" + Integer.toString(i);

            if (courseCopy.customColor != null && label.equals(courseCopy.customColor)) {
                button.setImageResource(R.drawable.checkmark);
            } else {
                button.setImageResource(0);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_custom_course_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("CustomCourseEdit", "Options Item selected");
        if (item.getItemId() == R.id.actionDone) {
            Log.d("CustomCourseEdit", "Done");
            validateAndFinish();
            return true;
        }
        return false;
    }

    // Check inputs, and show an alert if unsatisfied
    private void validateAndFinish() {
        if (courseCopy.getSubjectID().length() == 0 || courseCopy.subjectTitle.length() == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Missing Information")
                    .setMessage("Please fill in the Short Name and Title of this activity.")
                    .setCancelable(true)
                    .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
            return;
        }

        Course oldCourse = CourseManager.sharedInstance().getCustomCourse(courseCopy.getSubjectID(), courseCopy.subjectTitle);
        if (oldCourse != null && !oldCourse.equals(course)) {
            new AlertDialog.Builder(this)
                    .setTitle("Activity Exists")
                    .setMessage("Please choose a different short name or title.")
                    .setCancelable(true)
                    .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
            return;
        }

        // Success - copy over the information to the course
        JSONObject data = courseCopy.toJSON();
        course.readJSON(data);
        CourseManager.sharedInstance().setCustomCourse(course);
        setResult(RESULT_OK);
        // Save documents that might have this item
        if (User.currentUser().getCurrentDocument() != null)
            User.currentUser().getCurrentDocument().save();
        if (User.currentUser().getCurrentSchedule() != null)
            User.currentUser().getCurrentSchedule().save();
        finish();
    }
}
