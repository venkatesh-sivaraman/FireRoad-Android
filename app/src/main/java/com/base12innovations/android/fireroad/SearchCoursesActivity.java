package com.base12innovations.android.fireroad;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

public class SearchCoursesActivity extends AppCompatActivity implements AddCourseDialog.AddCourseDialogDelegate, SearchResultsAdapter.Delegate {

    private Toolbar toolbar;
    private SearchResultsAdapter listAdapter;
    private SearchView searchView;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_courses);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            /*String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d("SearchCoursesActivity", "searched " + query);*/
        }

        // Set up list view
        listView = findViewById(R.id.resultsList);
        listAdapter = new SearchResultsAdapter(this, null);
        listAdapter.delegate = this;
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (searchView.isFocused()) {
                    searchView.clearFocus();
                }
                Course courseToShow = (Course)listAdapter.getItem(i);
                if (courseToShow == null) {
                    return;
                }
                showCourseDetails(courseToShow);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.searchView).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, SearchCoursesActivity.class)));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint(getString(R.string.search_hint));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                loadSearchResults(s);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                loadSearchResults(s);
                return true;
            }
        });

        LinearLayout.LayoutParams navButtonsParams = new LinearLayout.LayoutParams((int)(toolbar.getHeight() * 0.5), (int)(toolbar.getHeight() * 0.5f));

        View emptyView = new View(this);
        emptyView.setBackgroundColor(0x0);

        ImageButton btnFilter = new ImageButton(this);
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.selectableItemBackgroundBorderless, outValue, true);
        btnFilter.setBackgroundResource(outValue.resourceId);
        btnFilter.setImageResource(R.drawable.filter_icon);

        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("SearchCourses", "Pressed filter button");
            }
        });

        ((LinearLayout) searchView.getChildAt(0)).addView(btnFilter, navButtonsParams);
        int width = (int)getResources().getDimension(R.dimen.filter_button_padding);
        ((LinearLayout) searchView.getChildAt(0)).addView(emptyView, new LinearLayout.LayoutParams(width, 16));
        ((LinearLayout) searchView.getChildAt(0)).setGravity(Gravity.CENTER_VERTICAL);

        menu.findItem(R.id.searchView).expandActionView();
        searchView.requestFocus();
        return true;
    }

    private boolean isSearching = false;

    public void loadSearchResults(final String query) {
        if (isSearching) {
            return;
        }
        if (query.length() == 0) {
            listAdapter.setCourses(new ArrayList<Course>());
            return;
        }

        isSearching = true;
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                final List<Course> courses = CourseManager.sharedInstance().searchSubjectsFast(query);
                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        isSearching = false;
                        listAdapter.setCourses(courses);
                        if (!searchView.getQuery().toString().equals(query)) {
                            loadSearchResults(searchView.getQuery().toString());
                        }
                    }
                });
            }
        });
    }

    public void showCourseDetails(Course course) {
        Intent intent = new Intent(this, CourseDetailsActivity.class);
        intent.putExtra(CourseDetailsActivity.COURSE_EXTRA, course);
        startActivity(intent);
    }

    private AddCourseDialog addCourseDialog;

    @Override
    public void searchResultsClickedAddButton(Course selectedCourse) {
        addCourseDialog = new AddCourseDialog();
        addCourseDialog.course = selectedCourse;
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
        if (listView != null) {
            Snackbar.make(listView, "Added " + course.getSubjectID(), Snackbar.LENGTH_LONG).show();
        }
        addCourseDialog = null;
    }
}
