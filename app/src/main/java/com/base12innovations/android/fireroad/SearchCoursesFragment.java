package com.base12innovations.android.fireroad;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.RoadDocument;
import com.base12innovations.android.fireroad.models.User;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SearchCoursesFragment extends Fragment implements BottomSheetNavFragment, AddCourseDialog.AddCourseDialogDelegate, SearchResultsAdapter.Delegate {

    private static String SEARCH_QUERY_EXTRA = "_searchQueryString";
    private Toolbar toolbar;
    private SearchResultsAdapter listAdapter;
    private RecyclerView resultsView;
    public String searchQuery;
    public boolean canGoBack = false;

    private ProgressBar progressIndicator;

    public SearchCoursesFragment() {

    }

    public static SearchCoursesFragment newInstance(String query) {
        SearchCoursesFragment fragment = new SearchCoursesFragment();
        Bundle args = new Bundle();
        args.putString(SEARCH_QUERY_EXTRA, query);
        fragment.setArguments(args);
        return fragment;
    }

    public WeakReference<Delegate> delegate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_search_courses, container, false);

        searchQuery = getArguments().getString(SEARCH_QUERY_EXTRA);
        // Set up recycler view
        listAdapter = new SearchResultsAdapter(getContext(), null);
        listAdapter.delegate = new WeakReference<SearchResultsAdapter.Delegate>(this);
        resultsView = layout.findViewById(R.id.resultsList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        resultsView.setLayoutManager(layoutManager);
        resultsView.setAdapter(listAdapter);

        toolbar = (Toolbar) layout.findViewById(R.id.toolbar);
        toolbar.setClickable(true);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate.get() != null) {
                    delegate.get().navFragmentClickedToolbar(SearchCoursesFragment.this);
                }
            }
        });
        if (canGoBack) {
            toolbar.setNavigationIcon(R.drawable.back_icon);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (delegate.get() != null) {
                        delegate.get().navFragmentWantsBack(SearchCoursesFragment.this);
                    }
                }
            });
        }
        toolbar.setTitle("Searching...");
        TypedValue value = new TypedValue ();
        getContext().getTheme ().resolveAttribute (R.attr.colorPrimary, value, true);
        toolbar.setBackgroundColor(value.data);

        progressIndicator = layout.findViewById(R.id.loadingIndicator);
        if (searchQuery != null && searchQuery.length() > 0)
            loadSearchResults(searchQuery);

        return layout;
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
        progressIndicator.setVisibility(ProgressBar.VISIBLE);
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {

                // Do the search
                final List<Course> courses = CourseSearchEngine.sharedInstance().searchSubjects(query);

                // On completion
                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        isSearching = false;
                        toolbar.setTitle(Integer.toString(courses.size()) + " Search Results");
                        progressIndicator.setVisibility(ProgressBar.INVISIBLE);
                        listAdapter.setCourses(courses);
                        if (!searchQuery.equals(query)) {
                            loadSearchResults(searchQuery);
                        }
                    }
                });
            }
        });
    }


    private AddCourseDialog addCourseDialog;

    @Override
    public void searchResultsClickedAddButton(Course selectedCourse) {
        addCourseDialog = new AddCourseDialog();
        addCourseDialog.course = selectedCourse;
        addCourseDialog.delegate = this;
        try {
            addCourseDialog.show(getActivity().getSupportFragmentManager(), "AddCourseFragment");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void searchResultsClickedCourse(Course selectedCourse) {
        if (delegate.get() != null) {
            delegate.get().courseNavigatorWantsCourseDetails(SearchCoursesFragment.this, selectedCourse);
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
            if (worked && delegate.get() != null) {
                delegate.get().courseNavigatorAddedCourse(this, course, semester);
            }
        }
        addCourseDialog.dismiss();
        if (resultsView != null) {
            Snackbar.make(resultsView, "Added " + course.getSubjectID(), Snackbar.LENGTH_LONG).show();
        }
        addCourseDialog = null;
    }
}
