package com.base12innovations.android.fireroad;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.arlib.floatingsearchview.util.view.MenuView;
import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.CourseManager;
import com.base12innovations.android.fireroad.models.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.Document;
import com.base12innovations.android.fireroad.models.NetworkManager;
import com.base12innovations.android.fireroad.models.ScheduleConfiguration;
import com.base12innovations.android.fireroad.models.ScheduleGenerator;
import com.base12innovations.android.fireroad.models.User;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;

public class MainActivity extends AppCompatActivity implements RequirementsFragment.OnFragmentInteractionListener, BottomSheetNavFragment.Delegate,
        FilterDialogFragment.Delegate {

    private static String CURRENT_FRAGMENT_TAG = "currentlyDisplayedFragment";

    private static int ROAD_BROWSER_REQUEST = 1234;
    private static int SCHEDULE_BROWSER_REQUEST = 5678;

    private static int AUTHENTICATION_INTENT_TAG = 1425;
    private NetworkManager.AsyncResponse<JSONObject> authenticationCompletion;

    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView navDrawer;
    private FloatingSearchView mSearchView;
    private MenuItem filterItem;

    private MyRoadFragment myRoadFragment;
    private RequirementsFragment requirementsFragment;

    private CourseLoadingDialogFragment loadingDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!CourseManager.sharedInstance().isLoaded()) {
            CourseManager.sharedInstance().initializeDatabase(this);
            User.currentUser().initialize(this);
        }

        mDrawer = (DrawerLayout) findViewById(R.id.main_content);
        navDrawer = (NavigationView)findViewById(R.id.nav_view);
        setupSearchView();
        setupBottomSheet();
        hideBottomSheet();

        restoreFragments();
        if (getSupportFragmentManager().findFragmentByTag(CURRENT_FRAGMENT_TAG) == null) {
            int lastShown = lastShownFragmentID();
            if (lastShown != 0) {
                showContentFragment(lastShown);
            } else
                showContentFragment(R.id.my_road_menu_item);
        }
        setupDrawerContent(navDrawer);

        NetworkManager.sharedInstance().authenticationListener = new NetworkManager.AuthenticationListener() {
            @Override
            public void showAuthenticationView(String url, NetworkManager.AsyncResponse<JSONObject> completion) {
                authenticationCompletion = completion;
                Intent i = new Intent(MainActivity.this, AuthenticationActivity.class);
                i.putExtra(AuthenticationActivity.AUTH_URL_EXTRA, url);
                startActivityForResult(i, AUTHENTICATION_INTENT_TAG);
            }
        };

        if (!CourseManager.sharedInstance().isLoaded()) {
            CourseManager.sharedInstance().postLoadBlock = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    User.currentUser().loadRecentDocuments();
                    if (User.currentUser().getCurrentDocument() == null) {
                        MyRoadFragment.createInitialDocument(MainActivity.this, null);
                    }
                    if (User.currentUser().getCurrentSchedule() == null) {
                        ScheduleFragment.createInitialDocument(MainActivity.this, null);
                    }
                    return null;
                }
            };
            CourseManager.sharedInstance().loadCourses(new CourseManager.LoadCoursesListener() {
                @Override
                public void completion() {
                    if (loadingDialogFragment != null) {
                        loadingDialogFragment.dismiss();
                    }
                    CourseManager.sharedInstance().syncPreferences();
                }

                @Override
                public void error() {
                    if (loadingDialogFragment != null) {
                        loadingDialogFragment.dismiss();
                    }
                }

                @Override
                public void needsFullLoad() {
                    loadingDialogFragment = new CourseLoadingDialogFragment();
                    loadingDialogFragment.setCancelable(false);
                    loadingDialogFragment.show(getSupportFragmentManager(), "LoadingDialogFragment");
                }
            });
        }

        NetworkManager.sharedInstance().loginIfNeeded(new NetworkManager.AsyncResponse<Boolean>() {
            @Override
            public void success(Boolean result) {
                Log.d("MainActivity", "Logged in");
            }

            @Override
            public void failure() {
                Log.d("MainActivity", "Failed");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == ROAD_BROWSER_REQUEST && resultCode == RESULT_OK) {
            if (myRoadFragment != null)
                myRoadFragment.reloadView();
        } else if (requestCode == SCHEDULE_BROWSER_REQUEST && resultCode == RESULT_OK) {
            if (scheduleFragment != null)
                scheduleFragment.loadSchedules(true);
        } else if (requestCode == AUTHENTICATION_INTENT_TAG && authenticationCompletion != null) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    String json = data.getStringExtra(AuthenticationActivity.AUTH_RESULT_EXTRA);
                    json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1);
                    json = json.replaceAll("\\\\[\"]", "\"");
                    Log.d("MainActivity", json);
                    JSONObject result = new JSONObject(json);
                    Log.d("MainActivity", "Success with " + result.toString());
                    authenticationCompletion.success(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                    authenticationCompletion.failure();
                }
            } else {
                authenticationCompletion.failure();
            }
        }
    }

    /*@Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(CURRENT_VISIBLE_FRAGMENT, currentVisibleFragment);
    }*/

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
        int lastShown = lastShownFragmentID();
        if (lastShown != 0 && navigationView.getMenu().findItem(lastShown) != null) {
            navigationView.getMenu().findItem(lastShown).setChecked(true);
        }
    }

    public void selectDrawerItem(MenuItem menuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        showContentFragment(menuItem.getItemId());
        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);
        // Set action bar title
        setTitle(menuItem.getTitle());
        // Close the navigation drawer
        mDrawer.closeDrawers();
    }

    private void restoreFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentByTag(CURRENT_FRAGMENT_TAG);
        if (currentFragment != null) {
            if (currentFragment instanceof MyRoadFragment)
                myRoadFragment = (MyRoadFragment)currentFragment;
            else if (currentFragment instanceof ScheduleFragment)
                scheduleFragment = (ScheduleFragment)currentFragment;
            else if (currentFragment instanceof RequirementsFragment)
                requirementsFragment = (RequirementsFragment)requirementsFragment;
        }
    }

    private void showContentFragment(int id) {
        Fragment fragment = null;
        switch(id) {
            /*case R.id.browse_menu_item:
                fragmentClass = FirstFragment.class;
                break;
            case R.id.nav_second_fragment:
                fragmentClass = SecondFragment.class;
                break;
            case R.id.nav_third_fragment:
                fragmentClass = ThirdFragment.class;
                break;*/
            case R.id.schedule_menu_item:
                fragment = getScheduleFragment();
                mSearchView.inflateOverflowMenu(R.menu.menu_main_schedule);
                break;
            case R.id.requirements_menu_item:
                fragment = getRequirementsFragment();
                mSearchView.inflateOverflowMenu(R.menu.menu_main);
                break;
            default:
                fragment = getMyRoadFragment();
                mSearchView.inflateOverflowMenu(R.menu.menu_main_road);
                break;
        }
        setLastShownFragmentID(id);

        showContentFragment(fragment);

    }

    private void showContentFragment(Fragment fragment) {
        if (fragment != null) {
            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.fr_content, fragment, CURRENT_FRAGMENT_TAG).commit();
        }
    }

    // Searching

    private static int NUM_SEARCH_SUGGESTIONS = 5;
    private boolean isSearching = false;
    private FilterDialogFragment mFilterDialog;
    private EnumSet<CourseSearchEngine.Filter> filters = EnumSet.copyOf(CourseSearchEngine.Filter.noFilter);

    public void setFilters(EnumSet<CourseSearchEngine.Filter> newValue) {
        filters = newValue;
        if (filterItem != null) {
            if (filters.equals(CourseSearchEngine.Filter.noFilter))
                filterItem.setIcon(R.drawable.filter_icon);
            else
                filterItem.setIcon(R.drawable.filter_icon_filled);
            // This is needed to refresh the icon, due to peculiarities with floatingsearchview
            mSearchView.clearSearchFocus();
        }
    }

    private void setupSearchView() {
        mSearchView = findViewById(R.id.floating_search_view);
        mSearchView.attachNavigationDrawerToMenuButton(mDrawer);
        mSearchView.setOnLeftMenuClickListener(new FloatingSearchView.OnLeftMenuClickListener() {
            @Override
            public void onMenuOpened() {
                mDrawer.openDrawer(Gravity.START);
            }

            @Override
            public void onMenuClosed() {
                mDrawer.closeDrawers();
            }
        });
        mSearchView.setOnClearSearchActionListener(new FloatingSearchView.OnClearSearchActionListener() {
            @Override
            public void onClearSearchClicked() {
                loadSearchResults("");
                if (searchCoursesFragment != null)
                    hideBottomSheet();
            }
        });
        mSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, String newQuery) {
                loadSearchResults(newQuery);
            }
        });
        mSearchView.setOnFocusChangeListener(new FloatingSearchView.OnFocusChangeListener() {
            @Override
            public void onFocus() {
                loadSearchResults(mSearchView.getQuery());
            }

            @Override
            public void onFocusCleared() { }
        });
        mSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
            @Override
            public void onBindSuggestion(View suggestionView, ImageView leftIcon, TextView textView, SearchSuggestion item, int itemPosition) {
                CourseSearchSuggestion suggestion = (CourseSearchSuggestion)item;
                if (suggestion.isRecent) {
                    leftIcon.setImageResource(R.drawable.recent_icon);
                } else {
                    leftIcon.setImageDrawable(null);
                }
                textView.setLines(1);
                textView.setText(Html.fromHtml(item.getBody(), Html.FROM_HTML_MODE_LEGACY));
            }
        });

        mSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                final CourseSearchSuggestion suggestion = (CourseSearchSuggestion)searchSuggestion;
                mSearchView.clearSuggestions();
                mSearchView.clearSearchFocus();
                detailsStack = null;
                TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        final Course course = CourseManager.sharedInstance().getSubjectByID(suggestion.subjectID);
                        if (course == null) {
                            return;
                        }
                        CourseSearchEngine.sharedInstance().addRecentCourse(course);
                        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                            @Override
                            public void perform() {
                                onShowCourseDetails(course);
                            }
                        });
                    }
                });
            }

            @Override
            public void onSearchAction(final String currentQuery) {
                mSearchView.clearSuggestions();
                mSearchView.clearSearchFocus();
                detailsStack = null;
                TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        final Course course = CourseManager.sharedInstance().getSubjectByID(currentQuery);
                        if (course != null)
                            CourseSearchEngine.sharedInstance().addRecentCourse(course);
                        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                            @Override
                            public void perform() {
                                if (course == null) {
                                    showSearchCoursesView(currentQuery);
                                } else {
                                    onShowCourseDetails(course);
                                }
                            }
                        });
                    }
                });
            }
        });

        mSearchView.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
            @Override
            public void onActionMenuItemSelected(MenuItem item) {
                if (item.getItemId() == R.id.action_filter) {
                    // Show filter dialog
                    mFilterDialog = new FilterDialogFragment();
                    mFilterDialog.filters = filters;
                    mFilterDialog.delegate = new WeakReference<FilterDialogFragment.Delegate>(MainActivity.this);
                    mFilterDialog.show(getSupportFragmentManager(), "FilterDialog");
                    filterItem = item;
                    // This is needed to refresh the icon, due to peculiarities with floatingsearchview
                    mSearchView.getBackground().setAlpha(150);
                } else if (item.getItemId() == R.id.action_browse_road) {
                    Intent browseIntent = new Intent(MainActivity.this, DocumentBrowserActivity.class);
                    browseIntent.putExtra(DocumentBrowserActivity.DOCUMENT_TYPE_EXTRA, Document.ROAD_DOCUMENT_TYPE);
                    startActivityForResult(browseIntent, ROAD_BROWSER_REQUEST);
                } else if (item.getItemId() == R.id.action_browse_schedule) {
                    Intent browseIntent = new Intent(MainActivity.this, DocumentBrowserActivity.class);
                    browseIntent.putExtra(DocumentBrowserActivity.DOCUMENT_TYPE_EXTRA, Document.SCHEDULE_DOCUMENT_TYPE);
                    startActivityForResult(browseIntent, SCHEDULE_BROWSER_REQUEST);
                }
            }
        });
    }

    public void loadSearchResults(final String query) {
        if (isSearching) {
            return;
        }
        if (query.length() == 0) {
            // Put recents here
            CourseSearchEngine.sharedInstance().getRecentCourses(new CourseSearchEngine.RecentCoursesCallback() {
                @Override
                public void result(List<Course> courses) {
                    List<CourseSearchSuggestion> suggestions = new ArrayList<>();
                    for (Course course : courses) {
                        if (suggestions.size() > NUM_SEARCH_SUGGESTIONS)
                            break;
                        suggestions.add(new CourseSearchSuggestion(course.getSubjectID(), course.subjectTitle, true));
                    }
                    if (mSearchView.getQuery().length() == 0) {
                        mSearchView.swapSuggestions(suggestions);
                    }
                }
            });
            return;
        }

        isSearching = true;
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                final List<Course> courses = CourseSearchEngine.sharedInstance().searchSubjectsFast(query, filters);
                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        isSearching = false;
                        List<CourseSearchSuggestion> suggestions = new ArrayList<>();
                        for (Course course : courses) {
                            if (suggestions.size() > NUM_SEARCH_SUGGESTIONS)
                                break;
                            suggestions.add(new CourseSearchSuggestion(course.getSubjectID(), course.subjectTitle, false));
                        }
                        mSearchView.swapSuggestions(suggestions);
                        if (!mSearchView.getQuery().equals(query)) {
                            loadSearchResults(mSearchView.getQuery());
                        }
                    }
                });
            }
        });
    }

    // Bottom sheet

    private class BottomSheetItem {
        String searchQuery;
        Course course;

        BottomSheetItem(String query) {
            this.searchQuery = query;
        }

        BottomSheetItem(Course course) {
            this.course = course;
        }
    }

    private View bottomSheet;
    private SearchCoursesFragment searchCoursesFragment;
    private CourseDetailsFragment currentDetailsFragment;
    private List<BottomSheetItem> detailsStack;

    private void setupBottomSheet() {
        bottomSheet = findViewById(R.id.bottom_sheet);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        currentDetailsFragment = null;
                        detailsStack = null;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        View dimmer = findViewById(R.id.backgroundDimmer);
                        dimmer.setVisibility(View.GONE);
                        dimmer.setClickable(false);
                        if (currentDetailsFragment != null) {
                            currentDetailsFragment.scaleFAB(0.0f, true);
                        }
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        if (currentDetailsFragment != null) {
                            currentDetailsFragment.scaleFAB(1.0f, true);
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                View dimmer = findViewById(R.id.backgroundDimmer);
                dimmer.setVisibility(View.VISIBLE);
                dimmer.setClickable(true);
                dimmer.setAlpha(slideOffset);
                if (currentDetailsFragment != null) {
                    currentDetailsFragment.scaleFAB(slideOffset);
                }
            }
        });

        View dimmer = findViewById(R.id.backgroundDimmer);
        dimmer.setAlpha(0.0f);
        dimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });
    }

    private void presentBottomSheet(Fragment fragment) {
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.detail_content, fragment).commit();
        expandBottomSheet();
    }

    private void dimViewOn() {
        final View dimmer = findViewById(R.id.backgroundDimmer);
        dimmer.setClickable(true);
        dimmer.animate().alpha(1.0f).setDuration(300).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) { }

            @Override
            public void onAnimationEnd(Animator animator) {
                dimmer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) { }

            @Override
            public void onAnimationRepeat(Animator animator) { }
        });
    }

    private void dimViewOff() {
        Log.d("MainActivity", "Dimming off");
        final View dimmer = findViewById(R.id.backgroundDimmer);
        dimmer.setClickable(false);
        if (dimmer.getVisibility() == View.VISIBLE) {
            AlphaAnimation animation1 = new AlphaAnimation(dimmer.getAlpha(), 0.0f);
            animation1.setDuration(300);
            animation1.setFillAfter(true);
            animation1.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    dimmer.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            dimmer.startAnimation(animation1);
        }
    }

    private void expandBottomSheet() {
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        dimViewOn();
    }

    private void hideBottomSheet() {
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        currentDetailsFragment = null;
        searchCoursesFragment = null;
        detailsStack = null;
        dimViewOff();
    }

    private void collapseBottomSheet() {
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        dimViewOff();
    }

    @Override
    public void navFragmentClickedToolbar(BottomSheetNavFragment fragment) {
        // Expand bottom sheet
        expandBottomSheet();
    }

    @Override
    public void courseNavigatorWantsCourseDetails(Fragment source, Course course) {
        onShowCourseDetails(course);
    }

    @Override
    public void navFragmentWantsBack(BottomSheetNavFragment fragment) {
        if (detailsStack != null && detailsStack.size() >= 2) {
            detailsStack.remove(detailsStack.size() - 1);
            BottomSheetItem last = detailsStack.remove(detailsStack.size() - 1);
            if (last.searchQuery != null) {
                showSearchCoursesView(last.searchQuery);
            } else if (last.course != null) {
                onShowCourseDetails(last.course);
            }
        }
    }

    @Override
    public void courseNavigatorAddedCourse(Fragment source, Course course, int semester) {
        if (semester == ADD_TO_SCHEDULE) {
            if (scheduleFragment != null)
                scheduleFragment.scheduleAddedCourse(course);
        } else {
            getMyRoadFragment().roadAddedCourse(course, semester);
            if (requirementsFragment != null) {
                requirementsFragment.notifyRequirementsStatusChanged();
            }
        }
        collapseBottomSheet();
    }

    @Override
    public void courseNavigatorWantsSearchCourses(Fragment source, String searchTerm, EnumSet<CourseSearchEngine.Filter> filters) {
        currentDetailsFragment = null;
        searchCoursesFragment = null;
        showSearchCoursesView(searchTerm, filters);
    }

    public void showSearchCoursesView(String query, EnumSet<CourseSearchEngine.Filter> filters) {
        SearchCoursesFragment fragment = SearchCoursesFragment.newInstance(query, filters);
        searchCoursesFragment = fragment;
        currentDetailsFragment = null;
        if (detailsStack == null) {
            detailsStack = new Stack<>();
        }
        fragment.canGoBack = detailsStack.size() > 0;
        detailsStack.add(new BottomSheetItem(query));
        fragment.delegate = new WeakReference<BottomSheetNavFragment.Delegate>(this);
        presentBottomSheet(fragment);
    }

    public void showSearchCoursesView(String query) {
        // Get the current selected filter here
        showSearchCoursesView(query, filters);
    }

    // Filter

    @Override
    public void filterDialogDismissed(FilterDialogFragment fragment) {
        setFilters(fragment.filters);
        fragment.dismiss();

        if (searchCoursesFragment != null) {
            searchCoursesFragment.filters = filters;
            searchCoursesFragment.loadSearchResults(searchCoursesFragment.searchQuery);
        }
    }


    // My Road

    public MyRoadFragment getMyRoadFragment() {
        if (myRoadFragment == null) {
            myRoadFragment = new MyRoadFragment();
        }
        return myRoadFragment;
    }

    public void onShowCourseDetails(Course course) {
        if (detailsStack != null && detailsStack.size() > 0) {
            Course lastCourse = detailsStack.get(detailsStack.size() - 1).course;
            if (lastCourse != null && lastCourse.getSubjectID().equals(course.getSubjectID())) {
                expandBottomSheet();
                return;
            }
        }
        CourseDetailsFragment fragment = CourseDetailsFragment.newInstance(course);
        currentDetailsFragment = fragment;
        searchCoursesFragment = null;
        if (detailsStack == null) {
            detailsStack = new Stack<>();
        }
        fragment.canGoBack = detailsStack.size() > 0;
        detailsStack.add(new BottomSheetItem(course));
        fragment.delegate = new WeakReference<BottomSheetNavFragment.Delegate>(this);
        presentBottomSheet(fragment);
    }

    // Requirements
    private RequirementsFragment getRequirementsFragment() {
        if (requirementsFragment == null)
            requirementsFragment = RequirementsFragment.newInstance();
        return requirementsFragment;
    }

    // Schedule
    private ScheduleFragment scheduleFragment;
    private ScheduleFragment getScheduleFragment() {
        if (scheduleFragment == null) {
            scheduleFragment = ScheduleFragment.newInstance();
        }
        return scheduleFragment;
    }

    // Preferences
    private static String PREFERENCES = "com.base12innovations.android.fireroad.MainActivity.Preferences";
    private static String LAST_SHOWN_FRAGMENT = "lastShownFragment";

    public int lastShownFragmentID() {
        SharedPreferences prefs = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getInt(LAST_SHOWN_FRAGMENT, 0);
    }

    public void setLastShownFragmentID(int id) {
        SharedPreferences prefs = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(LAST_SHOWN_FRAGMENT, id);
        editor.apply();
    }
}
