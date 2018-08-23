package com.base12innovations.android.fireroad;


import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.base12innovations.android.fireroad.dialog.AddCourseDialog;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.ColorManager;
import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.CourseManager;
import com.base12innovations.android.fireroad.models.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.NetworkManager;
import com.base12innovations.android.fireroad.models.RequirementsList;
import com.base12innovations.android.fireroad.models.RequirementsListManager;
import com.base12innovations.android.fireroad.models.RequirementsListStatement;
import com.base12innovations.android.fireroad.models.RoadDocument;
import com.base12innovations.android.fireroad.models.ScheduleDocument;
import com.base12innovations.android.fireroad.models.User;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.base12innovations.android.fireroad.CourseNavigatorDelegate.ADD_TO_SCHEDULE;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ForYouFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ForYouFragment extends Fragment implements AddCourseDialog.AddCourseDialogDelegate {

    public ForYouFragment() {
        // Required empty public constructor
    }

    public interface Delegate extends CourseNavigatorDelegate {

    }

    @NonNull
    public WeakReference<Delegate> delegate = new WeakReference<>(null);
    private View mLayout;
    private LinearLayout contentLayout;
    private ProgressBar loadingIndicator;
    private LinearLayout faveCard;
    private LinearLayout recentsCard;

    boolean isAttached = false;

    public static ForYouFragment newInstance() {
        ForYouFragment fragment = new ForYouFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mLayout = inflater.inflate(R.layout.fragment_for_you, container, false);
        loadingIndicator = mLayout.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.VISIBLE);

        contentLayout = mLayout.findViewById(R.id.linearLayout);

        if (CourseManager.sharedInstance().isLoaded()) {
            getRecommendationsAndBuildLayout();
        } else {
            CourseManager.sharedInstance().waitForLoad(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    getRecommendationsAndBuildLayout();
                    return null;
                }
            });
        }

        return mLayout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        isAttached = true;
        if (faveCard != null)
            updateFavoritesLayout();
        if (recentsCard != null)
            updateRecentsLayout();
        if (context instanceof Delegate) {
            delegate = new WeakReference<>((Delegate) context);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        isAttached = false;
        delegate = new WeakReference<>(null);
        super.onDetach();
    }

    private void getRecommendationsAndBuildLayout() {
        if (CourseManager.sharedInstance().getSubjectRecommendations() != null) {
            buildLayout(contentLayout);
        } else {
            CourseManager.sharedInstance().fetchRecommendations(new CourseManager.RecommendationsFetchCompletion() {
                @Override
                public void completed(Map<String, Map<Course, Double>> result) {
                    buildLayout(contentLayout);
                }

                @Override
                public void error(int code) {
                    buildLayout(contentLayout);
                }
            });
        }
    }

    private void buildLayout(LinearLayout layout) {
        loadingIndicator.setVisibility(View.GONE);
        layout.removeAllViews();

        // Recommendations
        if (CourseManager.sharedInstance().getSubjectRecommendations() == null) {
            LinearLayout firstCard = addCard(layout, -1);
            String message;
            if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED)
                message = "Turn on Sync and Recommendations in the Settings pane to get recommended subjects.";
            else
                message = "No recommendations at the moment. Try again tomorrow!";
            addCenteredDescriptionItem(firstCard, message);
        } else {
            final Map<String, Map<Course, Double>> recs = CourseManager.sharedInstance().getSubjectRecommendations();
            if (recs.containsKey(CourseManager.RECOMMENDATION_KEY_FOR_YOU)) {
                final String key = CourseManager.RECOMMENDATION_KEY_FOR_YOU;

                LinearLayout card = addCard(layout, -1);
                addHeaderItem(card, titleForRecommendationsKey(key));

                List<Course> courses = new ArrayList<>(recs.get(key).keySet());
                Collections.sort(courses, new Comparator<Course>() {
                    @Override
                    public int compare(Course c1, Course c2) {
                        return -Double.compare(recs.get(key).get(c1), recs.get(key).get(c2));
                    }
                });
                addCourseListItem(card, courses);
            }

            for (final String key : recs.keySet()) {
                if (key.equals(CourseManager.RECOMMENDATION_KEY_FOR_YOU)) continue;

                String title = titleForRecommendationsKey(key);
                if (title == null) continue;
                LinearLayout card = addCard(layout, -1);
                addHeaderItem(card, title);

                List<Course> courses = new ArrayList<>(recs.get(key).keySet());
                Collections.sort(courses, new Comparator<Course>() {
                    @Override
                    public int compare(Course c1, Course c2) {
                        return -Double.compare(recs.get(key).get(c1), recs.get(key).get(c2));
                    }
                });
                addCourseListItem(card, courses);
            }
        }

        faveCard = addCard(layout, -1);
        updateFavoritesLayout();
        CourseManager.sharedInstance().setFavoritesChangedListener(new CourseManager.FavoritesChangedListener() {
            @Override
            public void changed(List<String> newCourses) {
                if (isAttached)
                    updateFavoritesLayout();
            }
        });

        recentsCard = addCard(layout, -1);
        updateRecentsLayout();
        CourseSearchEngine.sharedInstance().setRecentCoursesChangedListener(new CourseSearchEngine.RecentCoursesListener() {
            @Override
            public void changed(List<Course> courses) {
                if (isAttached)
                    updateRecentsLayout();
            }
        });

        if (NetworkManager.sharedInstance().isLoggedIn()) {
            String username = AppSettings.shared().getString(AppSettings.RECOMMENDER_USERNAME, "<no username>");
            addCenteredDescriptionItem(layout, "You are currently logged in as " + username + ". " +
                    "To log out, go to the Settings pane.");
        }
    }

    public void updateFavoritesLayout() {
        final List<String> favorites = CourseManager.sharedInstance().getFavoriteCourses();
        faveCard.removeAllViews();
        addHeaderItem(faveCard, "Favorites");
        if (favorites.size() == 0) {
            addCenteredDescriptionItem(faveCard, "You don't have any favorite subjects yet. " +
                    "View a subject and tap Add to Favorites to save it here.");
        } else {
            TaskDispatcher.perform(new TaskDispatcher.Task<List<Course>>() {
                @Override
                public List<Course> perform() {
                    List<Course> courses = new ArrayList<>();
                    for (String subjectID : favorites) {
                        Course newCourse = CourseManager.sharedInstance().getSubjectByID(subjectID);
                        if (newCourse != null)
                            courses.add(newCourse);
                    }
                    return courses;
                }
            }, new TaskDispatcher.CompletionBlock<List<Course>>() {
                @Override
                public void completed(List<Course> arg) {
                    addCourseListItem(faveCard, arg);
                }
            });
        }
    }

    public void updateRecentsLayout() {
        CourseSearchEngine.sharedInstance().getRecentCourses(new CourseSearchEngine.RecentCoursesCallback() {
            @Override
            public void result(List<Course> courses) {
                recentsCard.removeAllViews();
                addHeaderItem(recentsCard, "Recents");
                if (courses.size() == 0) {
                    addCenteredDescriptionItem(recentsCard, "You don't have any recent subjects yet. " +
                            "Find subjects from the search bar above.");
                } else {
                    addCourseListItem(recentsCard, courses);
                }
            }
        });
    }

    private String titleForRecommendationsKey(String recommendationsKey) {
        if (recommendationsKey.equals("for-you"))
            return "For You";

        String[] components = recommendationsKey.split(":");
        switch (components[0]) {
            case "course":
                if (components[1].equals("girs")) {
                    return null;
                }
                RequirementsList list = RequirementsListManager.sharedInstance().getRequirementsList(components[1]);
                if (list == null) return null;
                String title = list.shortTitle != null ? list.shortTitle : list.mediumTitle;

                String category;
                if (components[1].contains("major")) {
                    category = "majors";
                } else if (components[1].contains("minor")) {
                    category = "minors";
                } else if (components[1].contains("master")) {
                    category = "masters students";
                } else {
                    category = "students";
                }

                return String.format(Locale.US, "%s %s may also like...", title, category);
            case "subject":
                return "Because you selected " + components[1] + "...";
            case "top-subjects":
                return "Top rated";
            case "after":
                return "What to take after " + components[1] + "...";
            case "keyword":
                return "If you like " + components[1] + "...";
            default:
                return null;
        }
    }

    private LinearLayout addCard(final LinearLayout layout, int rowIndex) {
        int margin = (int) getResources().getDimension(R.dimen.course_details_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, margin, margin, margin / 2);
        final LinearLayout card = (LinearLayout)LayoutInflater.from(getContext()).inflate(R.layout.requirements_card, null);
        if (rowIndex == -1)
            layout.addView(card);
        else
            layout.addView(card, rowIndex);

        card.setLayoutParams(lparams);

        return card;
    }

    private void addHeaderItem(LinearLayout layout, String title) {
        int margin = (int) getResources().getDimension(R.dimen.requirements_card_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_header, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        TextView titleView = (TextView)metadataView.findViewById(R.id.headingTitle);
        titleView.setText(title);
    }

    private View addCenteredDescriptionItem(LinearLayout layout, String description) {
        int margin = (int) getResources().getDimension(R.dimen.requirements_card_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, margin, margin, margin);
        View metadataView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_description, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        TextView textView = metadataView.findViewById(R.id.descriptionLabel);
        textView.setText(description);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return metadataView;
    }

    private View addCourseListItem(final LinearLayout layout, final List<Course> courses) {
        View listView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_list, null);
        layout.addView(listView);

        final LinearLayout listLayout = listView.findViewById(R.id.courseListLayout);

        for (int i = 0; i < courses.size(); i++) {
            addCourseCell(layout, listLayout, courses.get(i));
        }

        return listView;
    }

    private View addCourseCell(final LinearLayout parentLayout, LinearLayout layout, final Course course) {
        int width = (int) getResources().getDimension(R.dimen.course_cell_default_width);
        int height = (int) getResources().getDimension(R.dimen.course_cell_height);
        int margin = (int) getResources().getDimension(R.dimen.course_cell_spacing);
        int elevation = (int) getResources().getDimension(R.dimen.course_cell_elevation);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(margin, margin, margin, margin);
        final View courseThumbnail = LayoutInflater.from(getContext()).inflate(R.layout.linearlayout_course, null);
        layout.addView(courseThumbnail);
        courseThumbnail.setLayoutParams(params);
        courseThumbnail.setElevation(elevation);

        int color = ColorManager.colorForCourse(course);
        ((GradientDrawable)courseThumbnail.getBackground()).setColor(color);
        ((TextView) courseThumbnail.findViewById(R.id.subjectIDLabel)).setText(course.getSubjectID());
        ((TextView) courseThumbnail.findViewById(R.id.subjectTitleLabel)).setText(course.subjectTitle);

        courseThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate.get() != null) {
                    delegate.get().courseNavigatorWantsCourseDetails(ForYouFragment.this, course);
                }
            }
        });
        courseThumbnail.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                addCourse(course);
                return true;
            }
        });
        return courseThumbnail;
    }

    AddCourseDialog addCourseDialog;

    private void addCourse(Course course) {
        addCourseDialog = new AddCourseDialog();
        addCourseDialog.course = course;
        addCourseDialog.delegate = this;
        FragmentActivity a = getActivity();
        if (a != null) {
            addCourseDialog.show(a.getSupportFragmentManager(), "AddCourseFragment");
        }
    }

    @Override
    public void addCourseDialogDismissed() {
        addCourseDialog.dismiss();
        addCourseDialog = null;
    }

    @Override
    public void addCourseDialogAddedToSemester(Course course, int semester) {
        if (semester == ADD_TO_SCHEDULE) {
            ScheduleDocument doc = User.currentUser().getCurrentSchedule();
            if (doc != null) {
                doc.addCourse(course);
                if (delegate.get() != null)
                    delegate.get().courseNavigatorAddedCourse(this, course, semester);
                Snackbar.make(contentLayout, "Added " + course.getSubjectID() + " to schedule", Snackbar.LENGTH_LONG).show();
            }
        } else {
            RoadDocument doc = User.currentUser().getCurrentDocument();
            if (doc != null) {
                boolean worked = doc.addCourse(course, semester);
                if (worked) {
                    if (delegate.get() != null)
                        delegate.get().courseNavigatorAddedCourse(this, course, semester);
                    Snackbar.make(contentLayout, "Added " + course.getSubjectID(), Snackbar.LENGTH_LONG).show();
                }
            }
        }
        addCourseDialog.dismiss();
        addCourseDialog = null;
    }

}
