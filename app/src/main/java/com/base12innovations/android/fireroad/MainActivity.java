package com.base12innovations.android.fireroad;

import android.animation.Animator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.base12innovations.android.fireroad.activity.AuthenticationActivity;
import com.base12innovations.android.fireroad.activity.CustomCourseEditActivity;
import com.base12innovations.android.fireroad.activity.CustomCoursesActivity;
import com.base12innovations.android.fireroad.activity.DocumentBrowserActivity;
import com.base12innovations.android.fireroad.activity.IntroActivity;
import com.base12innovations.android.fireroad.activity.SettingsActivity;
import com.base12innovations.android.fireroad.dialog.CourseLoadingDialogFragment;
import com.base12innovations.android.fireroad.dialog.FilterDialogFragment;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.course.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.doc.Document;
import com.base12innovations.android.fireroad.models.doc.DocumentManager;
import com.base12innovations.android.fireroad.models.doc.NetworkManager;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.ScheduleDocument;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.utils.BottomSheetNavFragment;
import com.base12innovations.android.fireroad.utils.CourseSearchSuggestion;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements RequirementsFragment.OnFragmentInteractionListener, BottomSheetNavFragment.Delegate,
        FilterDialogFragment.Delegate, ForYouFragment.Delegate, MyRoadFragment.Delegate, DocumentManager.SyncResponseHandler, DocumentManager.SyncFileListener, ScheduleFragment.ScheduleFragmentDelegate {

    private static final String CURRENT_FRAGMENT_TAG = "currentlyDisplayedFragment";

    private static final int ROAD_BROWSER_REQUEST = 1234;
    private static final int SCHEDULE_BROWSER_REQUEST = 5678;

    private static final int AUTHENTICATION_INTENT_TAG = 1425;
    private NetworkManager.AsyncResponse<JSONObject> authenticationCompletion;

    private static final int INTRO_INTENT_TAG = 2514;
    private static final int CUSTOM_COURSES_INTENT_TAG = 1928;
    private static final int CUSTOM_COURSE_EDIT_INTENT_TAG = 12073;

    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView navDrawer;
    private FloatingSearchView mSearchView;
    private MenuItem filterItem;
    private ScheduledExecutorService syncExecutor;
    private ScheduledFuture syncFuture;

    private MyRoadFragment myRoadFragment;
    private RequirementsFragment requirementsFragment;
    private int backStackFragment = 0;  // Fragment to go back to if back button is pressed

    private CourseLoadingDialogFragment loadingDialogFragment;
    private boolean isActivityPaused = false;

    private String courseLoadErrorMessage;

    //region Lifecycle

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

        loadCourseManagerIfNeeded();

        if (!AppSettings.shared().getBoolean(AppSettings.SHOWN_INTRO, false)) {
            Intent i = new Intent(this, IntroActivity.class);
            startActivityForResult(i, INTRO_INTENT_TAG);
        } else {
            setupLogin();
        }
    }

    private void loadCourseManagerIfNeeded() {
        if (!CourseManager.sharedInstance().isLoaded() || CourseManager.sharedInstance().needsUpdateOnLaunch()) {
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
            if (CourseManager.sharedInstance().isUpdatingDatabase()) {
                Log.d("MainActivity", "Updating database");
                loadingDialogFragment = new CourseLoadingDialogFragment();
                loadingDialogFragment.setCancelable(false);
                loadingDialogFragment.show(getSupportFragmentManager(), "LoadingDialogFragment");
                CourseManager.sharedInstance().waitForLoad(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        if (loadingDialogFragment != null) {
                            loadingDialogFragment.dismiss();
                        }
                        CourseManager.sharedInstance().syncPreferences();
                        return null;
                    }
                });
            } else {
                CourseManager.sharedInstance().loadCourses(new CourseManager.LoadCoursesListener() {
                    @Override
                    public void completion() {
                        if (loadingDialogFragment != null && !isActivityPaused) {
                            loadingDialogFragment.dismiss();
                        }
                        loadingDialogFragment = null;
                        CourseManager.sharedInstance().syncPreferences();
                    }

                    @Override
                    public void error(String message) {
                        if (loadingDialogFragment != null && !isActivityPaused) {
                            loadingDialogFragment.dismiss();
                        }
                        loadingDialogFragment = null;
                        if (message != null) {
                            courseLoadErrorMessage = message;
                            if (!isActivityPaused)
                                showCourseLoadErrorMessage();
                        }
                    }

                    @Override
                    public void needsFullLoad() {
                        if (!isActivityPaused) {
                            loadingDialogFragment = new CourseLoadingDialogFragment();
                            loadingDialogFragment.setCancelable(false);
                            loadingDialogFragment.show(getSupportFragmentManager(), "LoadingDialogFragment");
                        }
                    }
                });
            }
        }
    }

    private void showCourseLoadErrorMessage() {
        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Courses failed to load")
                        .setMessage(courseLoadErrorMessage)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
                courseLoadErrorMessage = null;
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
                    Log.d("MainActivity", "result OK");
                    String json = data.getStringExtra(AuthenticationActivity.AUTH_RESULT_EXTRA);
                    json = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);
                    json = json.replaceAll("\\\\[\"]", "\"");
                    JSONObject result = new JSONObject(json);
                    Log.d("MainActivity", "Authentication worked, result is "+ json);
                    authenticationCompletion.success(result);
                } catch (JSONException | StringIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    authenticationCompletion.failure();
                }
            } else {
                Log.d("MainActivity", "result fail");
                authenticationCompletion.failure();
            }
        } else if (requestCode == INTRO_INTENT_TAG && resultCode == RESULT_OK) {
            AppSettings.shared().edit().putBoolean(AppSettings.SHOWN_INTRO, true).apply();
            setupLogin();
        } else if (requestCode == CUSTOM_COURSES_INTENT_TAG) {
            if (data != null &&
                    data.hasExtra(CustomCoursesActivity.ADDED_SUBJECT_ID_RESULT) &&
                    data.hasExtra(CustomCoursesActivity.ADDED_SUBJECT_TITLE_RESULT) &&
                    data.hasExtra(CustomCoursesActivity.ADDED_SEMESTER_RESULT)) {
                Course course = CourseManager.sharedInstance().getCustomCourse(
                        data.getStringExtra(CustomCoursesActivity.ADDED_SUBJECT_ID_RESULT),
                        data.getStringExtra(CustomCoursesActivity.ADDED_SUBJECT_TITLE_RESULT));
                courseNavigatorAddedCourse(null, course, data.getIntExtra(CustomCoursesActivity.ADDED_SEMESTER_RESULT, 0));
            }
        } else if (requestCode == CUSTOM_COURSE_EDIT_INTENT_TAG && resultCode == RESULT_OK) {
            if (myRoadFragment != null) {
                myRoadFragment.reloadView();
            }
            if (scheduleFragment != null && lastShownFragmentID() == R.id.schedule_menu_item) {
                scheduleFragment.loadSchedules(false);
            }
        }
    }

    @Override
    protected void onPause() {
        isActivityPaused = true;
        backStackFragment = 0;
        if (loadingDialogFragment != null) {
            loadingDialogFragment.dismiss();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityPaused = false;
        if (loadingDialogFragment != null) {
            loadingDialogFragment.dismiss();
        }
        if (!CourseManager.sharedInstance().isLoaded() && CourseManager.sharedInstance().isUpdatingDatabase()) {
            loadingDialogFragment = new CourseLoadingDialogFragment();
            loadingDialogFragment.setCancelable(false);
            loadingDialogFragment.show(getSupportFragmentManager(), "LoadingDialogFragment");
            CourseManager.sharedInstance().waitForLoad(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (loadingDialogFragment != null) {
                        loadingDialogFragment.dismiss();
                    }
                    CourseManager.sharedInstance().syncPreferences();
                    return null;
                }
            });
        } else if (courseLoadErrorMessage != null)
            showCourseLoadErrorMessage();
        else {
            loadCourseManagerIfNeeded();
            if (!NetworkManager.sharedInstance().isLoggedIn() &&
                    AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) == AppSettings.RECOMMENDATIONS_ALLOWED)
                setupLogin();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (syncFuture != null)
            syncFuture.cancel(false);
        syncExecutor = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        if (loadingDialogFragment != null) {
            loadingDialogFragment.dismiss();
        }
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            BottomSheetBehavior b = BottomSheetBehavior.from(bottomSheet);
            if (detailsStack != null && b != null && b.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                if (detailsStack.size() > 1) {
                    navFragmentWantsBack(null);
                } else {
                    hideBottomSheet();
                }
                return true;
            } else if (mDrawer.isDrawerOpen(GravityCompat.START)) {
                mDrawer.closeDrawers();
                return true;
            } else if (mFilterDialog != null && mFilterDialog.isVisible()) {
                filterDialogDismissed(mFilterDialog);
                return true;
            } else if (backStackFragment != 0) {
                showContentFragment(backStackFragment);
                backStackFragment = 0;
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    //endregion
    //region Content Fragments

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
            navigationView.setCheckedItem(lastShown);
        }
    }

    public void selectDrawerItem(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.settings_menu_item) {
            mDrawer.closeDrawers();
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return;
        } else if (menuItem.getItemId() == R.id.feedback_menu_item) {
            mDrawer.closeDrawers();
            showContactDialog();
            return;
        }
        // Create a new fragment and specify the fragment to show based on nav item clicked
        showContentFragment(menuItem.getItemId());
        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);
        // Set action bar title
        setTitle(menuItem.getTitle());
        // Close the navigation drawer
        mDrawer.closeDrawers();

        // Reset the fake backstack
        backStackFragment = 0;
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
                requirementsFragment = (RequirementsFragment)currentFragment;
            else if (currentFragment instanceof ForYouFragment)
                forYouFragment = (ForYouFragment)currentFragment;
        }
    }

    private void showContentFragment(int id) {
        if (isActivityPaused)
            return;
        Fragment fragment;
        switch(id) {
            case R.id.for_you_menu_item:
                fragment = getForYouFragment();
                mSearchView.inflateOverflowMenu(R.menu.menu_main);
                break;
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
        backStackFragment = lastShownFragmentID();
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

    //endregion

    //region Sync

    private void setupLogin() {
        NetworkManager.sharedInstance().loginIfNeeded(new NetworkManager.AsyncResponse<Boolean>() {
            @Override
            public void success(Boolean result) {
                NetworkManager.sharedInstance().getRoadManager().fileListener = new WeakReference<DocumentManager.SyncFileListener>(MainActivity.this);
                NetworkManager.sharedInstance().getScheduleManager().fileListener = new WeakReference<DocumentManager.SyncFileListener>(MainActivity.this);
                syncExecutor = Executors.newScheduledThreadPool(1);
                syncFuture = syncExecutor.scheduleAtFixedRate(new Runnable() {
                    public void run() {
                        performSync();
                    }
                }, 5, 60, TimeUnit.SECONDS);
                performSync();
            }

            @Override
            public void failure() {
                Log.d("MainActivity", "Failed");
            }
        });
    }

    public void performSync() {
        if (!CourseManager.sharedInstance().isLoaded() || CourseManager.sharedInstance().isUpdatingDatabase())
            return;
        Log.d("MainActivity", "Syncing");
        if (!handlingConflict) {
            NetworkManager.sharedInstance().getRoadManager().syncAllFiles(this);
            NetworkManager.sharedInstance().getScheduleManager().syncAllFiles(this);
        }
        CourseManager.sharedInstance().syncPreferences();
    }

    @Override
    public void documentManagerSyncedSuccessfully(DocumentManager manager) {
        Log.d("MainActivity", "Documents synced successfully");
    }

    @Override
    public void documentManagerSyncError(DocumentManager manager, String message) {
        if (message != null) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("Sync Error");
            b.setMessage(message);
            b.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            b.show();
        } else {
            Log.d("MainActivity", "Documents sync error");
        }
    }

    private boolean handlingConflict = false;

    @Override
    public void documentManagerSyncConflict(DocumentManager manager, final DocumentManager.SyncConflict conflict, final DocumentManager.SyncConflictResponse response) {
        handlingConflict = true;
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(conflict.title);
        b.setMessage(conflict.message);
        if (conflict.positiveButton != null)
            b.setPositiveButton(conflict.positiveButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    handlingConflict = false;
                    response.response(conflict.positiveButton);
                }
            });
        if (conflict.neutralButton != null)
            b.setNeutralButton(conflict.neutralButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    handlingConflict = false;
                    response.response(conflict.neutralButton);
                }
            });
        if (conflict.negativeButton != null)
            b.setNegativeButton(conflict.negativeButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    handlingConflict = false;
                    response.response(conflict.negativeButton);
                }
            });
        b.setCancelable(false);
        b.show();
    }

    @Override
    public void documentManagerModifiedFile(DocumentManager manager, String name) {
        if (manager.getDocumentType().equals(Document.ROAD_DOCUMENT_TYPE) &&
                User.currentUser().getCurrentDocument() != null &&
                name.equals(User.currentUser().getCurrentDocument().getFileName()) &&
                myRoadFragment != null) {
            User.currentUser().getCurrentDocument().readInBackground(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    myRoadFragment.reloadView();
                }
            });
        } else if (manager.getDocumentType().equals(Document.SCHEDULE_DOCUMENT_TYPE) &&
                User.currentUser().getCurrentSchedule() != null &&
                name.equals(User.currentUser().getCurrentSchedule().getFileName()) &&
                scheduleFragment != null) {
            User.currentUser().getCurrentSchedule().readInBackground(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    scheduleFragment.loadSchedules(false);
                }
            });
        }
    }

    @Override
    public void documentManagerDeletedFile(final DocumentManager manager, String name) {
        final Document current = manager.getCurrent();
        final File deletingFile = manager.getFileHandle(name);
        if (current != null && name.equals(current.getFileName())) {
            TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    boolean hasChangedDocument = false;
                    if (manager.getItemCount() > 0) {
                        for (int i = 0; i < manager.getItemCount(); i++) {
                            File f = manager.getFileHandle(i);
                            if (f.equals(current.file) || f.equals(deletingFile)) continue;
                            final Document doc = manager.getNonTemporaryDocument(i);
                            TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                                @Override
                                public void perform() {
                                    manager.setAsCurrent(doc);
                                }
                            });
                            hasChangedDocument = true;
                            break;
                        }
                    }

                    if (!hasChangedDocument) {
                        // Create new document
                        try {
                            final Document doc = manager.getNewDocument(manager.noConflictName(Document.INITIAL_DOCUMENT_TITLE));
                            TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                                @Override
                                public void perform() {
                                    manager.setAsCurrent(doc);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void documentManagerRenamedFile(final DocumentManager manager, String oldName, String newName) {
        Document doc = manager.getCurrent();
        if (doc.getFileName().equals(oldName)) {
            doc.file = manager.getFileHandle(newName);
            doc.readInBackground(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    if (manager.getDocumentType().equals(Document.ROAD_DOCUMENT_TYPE) && myRoadFragment != null)
                        myRoadFragment.reloadView();
                    else if (manager.getDocumentType().equals(Document.SCHEDULE_DOCUMENT_TYPE) && scheduleFragment != null)
                        scheduleFragment.loadSchedules(false);
                }
            });
        }
    }

    //endregion
    //region Searching

    private static int NUM_SEARCH_SUGGESTIONS = 5;
    private boolean isSearching = false;
    private FilterDialogFragment mFilterDialog;
    private EnumSet<CourseSearchEngine.Filter> filters = CourseSearchEngine.Filter.noFilter();

    public void setFilters(EnumSet<CourseSearchEngine.Filter> newValue) {
        filters = newValue;
        if (filterItem != null) {
            if (filters.equals(CourseSearchEngine.Filter.noFilter()))
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
                textView.setText(Html.fromHtml(item.getBody()));
            }
        });

        mSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                final CourseSearchSuggestion suggestion = (CourseSearchSuggestion)searchSuggestion;
                if (suggestion.subjectTitle.equals(getResources().getString(R.string.no_suggestions_message)))
                    return;
                if (suggestion.subjectTitle.equals(getResources().getString(R.string.more_suggestions))) {
                    performFullSearch(mSearchView.getQuery());
                    return;
                }
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
                performFullSearch(currentQuery);
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
                    mFilterDialog.setCancelable(false);
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
                } else if (item.getItemId() == R.id.action_share_road_file) {

                    shareFile(User.currentUser().getCurrentDocument().file);
                } else if (item.getItemId() == R.id.action_share_road_text) {

                    RoadDocument doc = User.currentUser().getCurrentDocument();
                    String base = doc.file.getName();
                    String fileName = base.substring(0, base.lastIndexOf('.'));
                    String text = doc.plainTextRepresentation();
                    shareText(text, fileName);
                } else if (item.getItemId() == R.id.action_share_sched_text) {

                    ScheduleDocument doc = User.currentUser().getCurrentSchedule();
                    String base = doc.file.getName();
                    String fileName = base.substring(0, base.lastIndexOf('.'));
                    String text = doc.plainTextRepresentation();
                    shareText(text, fileName);
                } else if (item.getItemId() == R.id.action_custom_courses) {
                    showCustomCourses(null);
                }
            }
        });
    }

    private void performFullSearch(final String currentQuery) {
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
                        if (suggestions.size() >= NUM_SEARCH_SUGGESTIONS)
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
                            if (suggestions.size() >= NUM_SEARCH_SUGGESTIONS - 1 && courses.size() > NUM_SEARCH_SUGGESTIONS)
                                break;
                            suggestions.add(new CourseSearchSuggestion(course.getSubjectID(), course.subjectTitle, false));
                        }
                        if (courses.size() > NUM_SEARCH_SUGGESTIONS)
                            suggestions.add(new CourseSearchSuggestion("", getResources().getString(R.string.more_suggestions), false));
                        if (suggestions.size() == 0)
                            suggestions.add(new CourseSearchSuggestion("", getResources().getString(R.string.no_suggestions_message), false));
                        mSearchView.swapSuggestions(suggestions);
                        if (!mSearchView.getQuery().equals(query)) {
                            loadSearchResults(mSearchView.getQuery());
                        }
                    }
                });
            }
        });
    }

    //endregion
    //region Bottom Sheet

    private class BottomSheetItem {
        String searchQuery;
        EnumSet<CourseSearchEngine.Filter> filters;
        Course course;
        int scrollOffset;

        BottomSheetItem(String query, EnumSet<CourseSearchEngine.Filter> filters) {
            this.searchQuery = query;
            this.filters = filters;
        }

        BottomSheetItem(Course course) {
            this.course = course;
        }

        @Override
        public String toString() {
            if (searchQuery != null) {
                return "<Search \"" + searchQuery + "\", offset " + scrollOffset + ">";
            } else if (course != null) {
                return "<Details for " + course.getSubjectID() + ", offset " + scrollOffset + ">";
            }
            return "<BottomSheetItem???>";
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
        //Log.d("MainActivity", "Dimming on");
        final View dimmer = findViewById(R.id.backgroundDimmer);
        dimmer.setClickable(true);
        dimmer.setVisibility(View.VISIBLE);
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
        //Log.d("MainActivity", "Dimming off");
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

    //endregion
    //region Fragment Delegates

    @Override
    public void navFragmentClickedToolbar(BottomSheetNavFragment fragment) {
        // Expand bottom sheet
        expandBottomSheet();
    }

    @Override
    public void courseNavigatorWantsCourseDetails(Fragment source, Course course) {
        if (!source.equals(currentDetailsFragment) && !source.equals(searchCoursesFragment)) {
            // Clear the navigation stack
            detailsStack = null;
        }
        onShowCourseDetails(course);
    }

    @Override
    public void navFragmentWantsBack(BottomSheetNavFragment fragment) {
        Log.d("MainActivity", "Nav wants back, stack is " + detailsStack);
        if (detailsStack != null && detailsStack.size() >= 2) {
            detailsStack.remove(detailsStack.size() - 1);
            BottomSheetItem last = detailsStack.remove(detailsStack.size() - 1);
            Log.d("MainActivity", "Last scroll offset: " + last.scrollOffset);
            if (last.searchQuery != null) {
                showSearchCoursesView(last.searchQuery, last.filters, last.scrollOffset);
            } else if (last.course != null) {
                onShowCourseDetails(last.course, last.scrollOffset);
            }
        }
    }

    @Override
    public void courseNavigatorAddedCourse(Fragment source, Course course, int semester) {
        if (semester == ADD_TO_SCHEDULE) {
            ScheduleDocument doc = User.currentUser().getCurrentSchedule();
            if (doc != null) {
                doc.addCourse(course);
                Snackbar.make(mSearchView, "Added " + course.getSubjectID() + " to schedule", Snackbar.LENGTH_LONG).show();
            }
            showContentFragment(R.id.schedule_menu_item);
            if (scheduleFragment != null)
                scheduleFragment.scheduleAddedCourse(course);
        } else {
            RoadDocument doc = User.currentUser().getCurrentDocument();
            if (doc != null) {
                boolean worked = doc.addCourse(course, semester);
                if (worked) {
                    Snackbar.make(mSearchView, "Added " + course.getSubjectID() + " to " + RoadDocument.semesterNames[semester], Snackbar.LENGTH_LONG).show();
                }
            }
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
        showSearchCoursesView(searchTerm, filters, 0);
    }

    public void showSearchCoursesView(String query, EnumSet<CourseSearchEngine.Filter> filters, int scrollOffset) {
        //Log.d("MainActivity", "Search courses filters: " + filters.toString());
        SearchCoursesFragment fragment = SearchCoursesFragment.newInstance(query, filters);
        fragment.setScrollOffset(scrollOffset);
        updateLastScrollIndex();
        Log.d("MainActivity", "Searching courses, stack is " + detailsStack);
        searchCoursesFragment = fragment;
        currentDetailsFragment = null;
        if (detailsStack == null) {
            detailsStack = new Stack<>();
        }
        fragment.canGoBack = detailsStack.size() > 0;
        detailsStack.add(new BottomSheetItem(query, filters));
        fragment.delegate = new WeakReference<BottomSheetNavFragment.Delegate>(this);
        presentBottomSheet(fragment);
    }

    private void updateLastScrollIndex() {
        if (detailsStack != null && detailsStack.size() > 0) {
            BottomSheetItem last = detailsStack.get(detailsStack.size() - 1);
            if (searchCoursesFragment != null && searchCoursesFragment.searchQuery != null && searchCoursesFragment.searchQuery.equals(last.searchQuery)) {
                last.scrollOffset = searchCoursesFragment.getScrollOffset();
            } else if (currentDetailsFragment != null && currentDetailsFragment.course != null && currentDetailsFragment.course.equals(last.course)) {
                last.scrollOffset = currentDetailsFragment.getScrollOffset();
            }
        }
    }

    public void showSearchCoursesView(String query) {
        // Get the current selected filter here
        showSearchCoursesView(query, filters, 0);
    }

    @Override
    public void filterDialogDismissed(FilterDialogFragment fragment) {
        setFilters(fragment.filters);
        fragment.dismiss();

        if (searchCoursesFragment != null) {
            searchCoursesFragment.filters = filters;
            searchCoursesFragment.loadSearchResults(searchCoursesFragment.searchQuery);
        }
    }

    public MyRoadFragment getMyRoadFragment() {
        if (myRoadFragment == null) {
            myRoadFragment = new MyRoadFragment();
        }
        return myRoadFragment;
    }

    @Override
    public void myRoadFragmentAddedCoursesToSchedule(List<Course> courses, String fileName) {
        showContentFragment(R.id.schedule_menu_item);
        if (scheduleFragment != null)
            scheduleFragment.addAllCourses(courses, fileName);
    }

    @Override
    public void myRoadFragmentWantsCustomCoursesActivity(Course editCourse) {
        showCustomCourses(editCourse);
    }

    @Override
    public void scheduleFragmentWantsCustomCoursesActivity(Course editCourse) {
        showCustomCourses(editCourse);
    }

    public void onShowCourseDetails(Course course, int scrollOffset) {
        if (detailsStack != null && detailsStack.size() > 0) {
            Course lastCourse = detailsStack.get(detailsStack.size() - 1).course;
            if (lastCourse != null && lastCourse.getSubjectID().equals(course.getSubjectID())) {
                expandBottomSheet();
                return;
            }
        }
        CourseDetailsFragment fragment = CourseDetailsFragment.newInstance(course);
        fragment.setScrollOffset(scrollOffset);
        updateLastScrollIndex();
        Log.d("MainActivity", "Showing course details, stack is " + detailsStack);
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

    public void onShowCourseDetails(Course course) {
        onShowCourseDetails(course, 0);
    }

    // Requirements
    private RequirementsFragment getRequirementsFragment() {
        if (requirementsFragment == null)
            requirementsFragment = RequirementsFragment.newInstance();
        return requirementsFragment;
    }

    // For You
    private ForYouFragment forYouFragment;
    private ForYouFragment getForYouFragment() {
        if (forYouFragment == null)
            forYouFragment = ForYouFragment.newInstance();
        return forYouFragment;
    }

    // Schedule
    private ScheduleFragment scheduleFragment;
    private ScheduleFragment getScheduleFragment() {
        if (scheduleFragment == null) {
            scheduleFragment = ScheduleFragment.newInstance();
        }
        return scheduleFragment;
    }

    //endregion
    //region Custom Courses

    private void showCustomCourses(Course editCourse) {
        if (editCourse == null) {
            // Show the main custom course view
            Intent i = new Intent(MainActivity.this, CustomCoursesActivity.class);
            startActivityForResult(i, CUSTOM_COURSES_INTENT_TAG);
        } else {
            // Edit a specific custom course
            Intent i = new Intent(MainActivity.this, CustomCourseEditActivity.class);
            i.putExtra(CustomCourseEditActivity.SUBJECT_ID_EXTRA, editCourse.getSubjectID());
            i.putExtra(CustomCourseEditActivity.SUBJECT_TITLE_EXTRA, editCourse.subjectTitle);
            startActivityForResult(i, CUSTOM_COURSE_EDIT_INTENT_TAG);
        }
    }
    //endregion
    //region Preferences

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

    //endregion
    //region Sharing and Contact

    public void shareText(String text, String fileName) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(sharingIntent, "Share \"" + fileName + "\""));
    }

    public void shareFile(File location) {
        String base = location.getName();
        String fileName = base.substring(0, base.lastIndexOf('.'));

        Intent intentShareFile = new Intent(Intent.ACTION_SEND);

        if (!location.exists()) {
            Toast.makeText(this, "Could not share file - try again later", Toast.LENGTH_LONG).show();
            return;
        }

        intentShareFile.setType("application/json");
        Uri fileURI = FileProvider.getUriForFile(this, "com.base12innovations.android.fireroad.myFileProviderAuthority", location);
        intentShareFile.putExtra(Intent.EXTRA_STREAM, fileURI);
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, fileName);
        startActivity(Intent.createChooser(intentShareFile, "Share \"" + fileName + "\""));
    }

    public void showContactDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Report Issue");
        b.setMessage("To report corrections in the course requirements, please submit a modification request in the online Requirements Editor. For other bugs or corrections, send us an email.");
        b.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        b.setNegativeButton("Send Email", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, "base12apps@gmail.com");
                intent.putExtra(Intent.EXTRA_SUBJECT, "FireRoad Feedback");
                intent.putExtra(Intent.EXTRA_TEXT, "Platform:\nOS version:\nFeedback:");
                startActivity(Intent.createChooser(intent, "Contact developer"));
            }
        });
        b.setPositiveButton("Requirements Editor", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://fireroad.mit.edu/requirements"));
                startActivity(i);
            }
        });
        b.show();
    }

    //endregion
}
