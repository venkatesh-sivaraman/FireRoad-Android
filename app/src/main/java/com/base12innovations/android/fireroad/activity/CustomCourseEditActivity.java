package com.base12innovations.android.fireroad.activity;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.doc.NetworkManager;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.models.schedule.ScheduleSlots;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomCourseEditActivity extends AppCompatActivity {

    public static final String SUBJECT_ID_EXTRA = "com.base12innovations.android.fireroad.subject_id";
    public static final String SUBJECT_TITLE_EXTRA = "com.base12innovations.android.fireroad.subject_title";
    private Course course = null;
    private Course courseCopy = null;
    private LinearLayout layout;
    private Button addScheduleItemButton;

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

        layout = findViewById(R.id.editItemsLinearLayout);
        addScheduleItemButton = findViewById(R.id.addScheduleItem);

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

        addScheduleItems();
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

    private Map<Integer, Integer> colorCache = new HashMap<>();

    private void addScheduleItems() {

        Button addButton = findViewById(R.id.addScheduleItem);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Course.ScheduleItem newItem = courseCopy.addScheduleItem(Course.ScheduleType.CUSTOM);
                insertScheduleItem(newItem);
            }
        });

        Map<String, List<List<Course.ScheduleItem>>> schedule = courseCopy.getSchedule();
        if (schedule == null || !schedule.containsKey(Course.ScheduleType.CUSTOM) ||
                schedule.get(Course.ScheduleType.CUSTOM).size() < 1)
            return;

        List<Course.ScheduleItem> scheduleItems = schedule.get(Course.ScheduleType.CUSTOM).get(0);
        for (Course.ScheduleItem item: scheduleItems) {
            insertScheduleItem(item);
        }
    }

    private void insertScheduleItem(final Course.ScheduleItem item) {
        int insertIndex = layout.indexOfChild(addScheduleItemButton);

        final View view = getLayoutInflater().inflate(R.layout.cell_custom_course_schedule, null);
        layout.addView(view, insertIndex);

        Button[] buttons = { view.findViewById(R.id.buttonMonday), view.findViewById(R.id.buttonTuesday),
                view.findViewById(R.id.buttonWednesday), view.findViewById(R.id.buttonThursday),
                view.findViewById(R.id.buttonFriday) };
        for (int i = 0; i < buttons.length; i++) {
            final Button btn = buttons[i];
            final int day = Course.ScheduleDay.ordering[i];
            updateButton(btn, item, day);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if ((item.days & day) != 0) {
                        item.days = item.days - day;
                    } else {
                        item.days = (item.days | day);
                    }
                    updateButton(btn, item, day);
                }
            });
        }

        Spinner startTime = view.findViewById(R.id.startTimeSpinner);
        for (int i = 0; i < ScheduleSlots.slots.size(); i++) {
            Course.ScheduleTime time = ScheduleSlots.slots.get(i);
            if (time.hour24() == item.startTime.hour24() && time.minute == item.startTime.minute) {
                startTime.setSelection(i);
                break;
            }
        }
        startTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int selectedPosition, long l) {
                item.startTime = ScheduleSlots.slots.get(selectedPosition);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Spinner endTime = view.findViewById(R.id.endTimeSpinner);
        for (int i = 0; i < ScheduleSlots.slots.size(); i++) {
            Course.ScheduleTime time = ScheduleSlots.slots.get(i);
            if (time.hour24() == item.endTime.hour24() && time.minute == item.endTime.minute) {
                endTime.setSelection(i);
                break;
            }
        }
        endTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int selectedPosition, long l) {
                item.endTime = ScheduleSlots.slots.get(selectedPosition);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ImageButton deleteButton = view.findViewById(R.id.deleteScheduleItem);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, List<List<Course.ScheduleItem>>> schedule = courseCopy.getSchedule();
                if (schedule == null || !schedule.containsKey(Course.ScheduleType.CUSTOM) ||
                        schedule.get(Course.ScheduleType.CUSTOM).size() < 1) {
                    return;
                }
                List<Course.ScheduleItem> scheduleItems = schedule.get(Course.ScheduleType.CUSTOM).get(0);
                if (scheduleItems.contains(item)) {
                    scheduleItems.remove(item);
                    layout.removeView(view);
                } else {
                    Log.e("CustomCourseEdit", "Schedule doesn't contain the item being deleted");
                }
            }
        });
    }

    private void updateButton(Button button, Course.ScheduleItem item, int day) {
        int resId = button.getId();
        if ((item.days & day) != 0) {
            if (colorCache.containsKey(resId)) {
                ColorStateList myColorStateList = new ColorStateList(
                        new int[][]{
                                new int[]{android.R.attr.state_enabled}
                        },
                        new int[] {
                                colorCache.get(resId)
                        }
                );
                button.setBackgroundTintList(myColorStateList);
            }
            button.setTextColor(0xFFFFFFFF);
        } else {
            if (!colorCache.containsKey(resId)) {
                try {
                    int color = button.getBackgroundTintList().getDefaultColor();
                    if (color != 0xFFFFFFFF) {
                        colorCache.put(resId, color);
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            ColorStateList myColorStateList = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_enabled}
                    },
                    new int[] {
                            0xFFFFFFFF
                    }
            );
            button.setBackgroundTintList(myColorStateList);
            if (colorCache.containsKey(resId))
                button.setTextColor(colorCache.get(resId));
            else
                button.setTextColor(0xFF000000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_custom_course_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.actionDone) {
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

        Map<String, List<List<Course.ScheduleItem>>> schedule = courseCopy.getSchedule();
        if (schedule != null && schedule.containsKey(Course.ScheduleType.CUSTOM) &&
                schedule.get(Course.ScheduleType.CUSTOM).size() >= 1) {
            List<Course.ScheduleItem> scheduleItems = schedule.get(Course.ScheduleType.CUSTOM).get(0);
            for (Course.ScheduleItem item: scheduleItems) {
                if (item.days == 0) {
                    new AlertDialog.Builder(this)
                            .setTitle("Invalid Schedule")
                            .setMessage("Please choose at least one weekday for the schedule.")
                            .setCancelable(true)
                            .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).show();
                    return;
                }
                if (item.startTime.compareTo(item.endTime) >= 0) {
                    new AlertDialog.Builder(this)
                            .setTitle("Invalid Times")
                            .setMessage("The start time must be before the end time.")
                            .setCancelable(true)
                            .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).show();
                    return;
                }
            }
        }

        // Success - copy over the information to the course
        courseCopy.updateRawSchedule();
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
