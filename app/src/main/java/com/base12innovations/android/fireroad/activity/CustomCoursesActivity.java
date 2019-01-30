package com.base12innovations.android.fireroad.activity;

import android.content.Intent;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.base12innovations.android.fireroad.MyRoadFragment;
import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.adapter.CustomCoursesAdapter;
import com.base12innovations.android.fireroad.adapter.MyRoadCoursesAdapter;
import com.base12innovations.android.fireroad.dialog.AddCourseDialog;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.util.List;

public class CustomCoursesActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, AddCourseDialog.AddCourseDialogDelegate {

    private CustomCoursesAdapter gridAdapter;
    private ProgressBar loadingIndicator;

    private int currentlySelectedSemester;
    private int currentlySelectedPosition;
    private RecyclerView recyclerView;
    private Course currentlySelectedCourse;
    private PopupMenu currentPopupMenu;
    private View noCoursesView;
    private GridLayoutManager gridLayoutManager;

    private static final int EDIT_COURSE_ACTIVITY = 1203;

    // String subject ID and title
    public static final String ADDED_SUBJECT_ID_RESULT = "com.base12innovations.android.fireroad.CustomCoursesAddedSubjectID";
    public static final String ADDED_SUBJECT_TITLE_RESULT = "com.base12innovations.android.fireroad.CustomCoursesAddedTitle";
    // Integer representing semester index
    public static final String ADDED_SEMESTER_RESULT = "com.base12innovations.android.fireroad.CustomCoursesAddedSemester";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_courses);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get view elements
        loadingIndicator = findViewById(R.id.loadingIndicator);
        noCoursesView = findViewById(R.id.noCoursesView);

        // Set up grid view
        int numColumns = 3;
        if (!getResources().getBoolean(R.bool.portrait_only))
            numColumns = 6;
        gridAdapter = new CustomCoursesAdapter(numColumns);

        recyclerView = findViewById(R.id.customCoursesRecyclerView);
        recyclerView.setHasFixedSize(false);
        gridLayoutManager = new GridLayoutManager(this, numColumns);
        recyclerView.setAdapter(gridAdapter);
        recyclerView.setLayoutManager(gridLayoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.course_cell_spacing);
        recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        gridAdapter.itemClickListener = new MyRoadCoursesAdapter.ClickListener() {
            @Override
            public void onClick(Course course, int position, View view) {
                currentlySelectedCourse = course;
                currentlySelectedSemester = gridAdapter.semesterForGridPosition(position);
                currentlySelectedPosition = position;
                final PopupMenu menu = new PopupMenu(CustomCoursesActivity.this, view);
                MenuInflater mInflater = menu.getMenuInflater();
                mInflater.inflate(R.menu.menu_course_cell, menu.getMenu());

                menu.getMenu().findItem(R.id.addCourse).setVisible(true);
                menu.getMenu().findItem(R.id.viewCourse).setVisible(false);
                menu.getMenu().findItem(R.id.editCourse).setVisible(true);
                menu.getMenu().findItem(R.id.courseWarnings).setVisible(false);
                menu.getMenu().findItem(R.id.addMarker).setVisible(false);
                menu.getMenu().findItem(R.id.overrideWarningsOn).setVisible(false);
                menu.getMenu().findItem(R.id.overrideWarningsOff).setVisible(false);

                menu.setOnMenuItemClickListener(CustomCoursesActivity.this);
                menu.show();
                currentPopupMenu = menu;
            }
        };

        gridLayoutManager.setSpanSizeLookup(gridAdapter.spanSizeLookup());
        updateRecyclerView();

        FloatingActionButton button = findViewById(R.id.addCustomCourseButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CustomCoursesActivity.this, CustomCourseEditActivity.class);
                startActivityForResult(i, EDIT_COURSE_ACTIVITY);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == EDIT_COURSE_ACTIVITY) {
            updateRecyclerView();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.editCourse) {
            Intent i = new Intent(CustomCoursesActivity.this, CustomCourseEditActivity.class);
            i.putExtra(CustomCourseEditActivity.SUBJECT_ID_EXTRA, currentlySelectedCourse.getSubjectID());
            i.putExtra(CustomCourseEditActivity.SUBJECT_TITLE_EXTRA, currentlySelectedCourse.subjectTitle);
            startActivityForResult(i, EDIT_COURSE_ACTIVITY);
        } else if (menuItem.getItemId() == R.id.deleteCourse) {
            CourseManager.sharedInstance().removeCustomCourse(currentlySelectedCourse);
            updateRecyclerView();
        } else if (menuItem.getItemId() == R.id.addCourse) {
            addCourse(currentlySelectedCourse);
        }
        return false;
    }

    private void updateRecyclerView() {
        if (CourseManager.sharedInstance().getCustomCourses().size() > 0) {
            noCoursesView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            if (noCoursesView instanceof TextView)
                ((TextView)noCoursesView).setText(Html.fromHtml("<b>No custom activities yet!</b><br/>Tap the + button to create one."));
            recyclerView.setVisibility(View.INVISIBLE);
            noCoursesView.setVisibility(View.VISIBLE);
        }

        gridAdapter.reloadCourses();
        gridAdapter.notifyDataSetChanged();
    }

    // Add course

    AddCourseDialog addCourseDialog;

    private void addCourse(Course myCourse) {
        addCourseDialog = new AddCourseDialog();
        addCourseDialog.course = myCourse;
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
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ADDED_SUBJECT_ID_RESULT, course.getSubjectID());
        resultIntent.putExtra(ADDED_SUBJECT_TITLE_RESULT, course.subjectTitle);
        resultIntent.putExtra(ADDED_SEMESTER_RESULT, semester);
        addCourseDialog.dismiss();
        addCourseDialog = null;
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // Grid view spacing

    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;
            outRect.top = space;
        }
    }
}
