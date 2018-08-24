package com.base12innovations.android.fireroad;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.base12innovations.android.fireroad.dialog.AddCourseDialog;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.CourseManager;
import com.base12innovations.android.fireroad.models.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.NetworkManager;
import com.base12innovations.android.fireroad.models.RequirementsList;
import com.base12innovations.android.fireroad.models.RequirementsListManager;
import com.base12innovations.android.fireroad.utils.CourseLayoutBuilder;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;


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

    private CourseLayoutBuilder layoutBuilder;

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

        if (layoutBuilder == null) {
            layoutBuilder = new CourseLayoutBuilder(getContext());
            layoutBuilder.showHeadingTopMargin = false;
            layoutBuilder.defaultMargin = (int)getResources().getDimension(R.dimen.requirements_card_padding);
        }

        // Recommendations
        if (CourseManager.sharedInstance().getSubjectRecommendations() == null) {
            LinearLayout firstCard = layoutBuilder.addCard(layout);
            String message;
            if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED)
                message = "Turn on Sync and Recommendations in the Settings pane to get recommended subjects.";
            else
                message = "No recommendations at the moment. Try again tomorrow!";
            layoutBuilder.addCenteredDescriptionItem(firstCard, message);
        } else {
            final Map<String, Map<Course, Double>> recs = CourseManager.sharedInstance().getSubjectRecommendations();
            if (recs.containsKey(CourseManager.RECOMMENDATION_KEY_FOR_YOU)) {
                final String key = CourseManager.RECOMMENDATION_KEY_FOR_YOU;

                LinearLayout card = layoutBuilder.addCard(layout);
                layoutBuilder.addHeaderItem(card, titleForRecommendationsKey(key));

                List<Course> courses = new ArrayList<>(recs.get(key).keySet());
                Collections.sort(courses, new Comparator<Course>() {
                    @Override
                    public int compare(Course c1, Course c2) {
                        return -Double.compare(recs.get(key).get(c1), recs.get(key).get(c2));
                    }
                });
                LinearLayout listItem = layoutBuilder.addCourseListItem(card);
                for (Course course: courses)
                    addCourseCell(listItem, course);
            }

            for (final String key : recs.keySet()) {
                if (key.equals(CourseManager.RECOMMENDATION_KEY_FOR_YOU)) continue;

                String title = titleForRecommendationsKey(key);
                if (title == null) continue;
                LinearLayout card = layoutBuilder.addCard(layout);
                layoutBuilder.addSubHeaderItem(card, title, 0.0f, 18.0f);

                List<Course> courses = new ArrayList<>(recs.get(key).keySet());
                Collections.sort(courses, new Comparator<Course>() {
                    @Override
                    public int compare(Course c1, Course c2) {
                        return -Double.compare(recs.get(key).get(c1), recs.get(key).get(c2));
                    }
                });
                LinearLayout listItem = layoutBuilder.addCourseListItem(card);
                for (Course course: courses)
                    addCourseCell(listItem, course);
            }
        }

        faveCard = layoutBuilder.addCard(layout);
        updateFavoritesLayout();
        CourseManager.sharedInstance().setFavoritesChangedListener(new CourseManager.FavoritesChangedListener() {
            @Override
            public void changed(List<String> newCourses) {
                if (isAttached)
                    updateFavoritesLayout();
            }
        });

        recentsCard = layoutBuilder.addCard(layout);
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
            layoutBuilder.addCenteredDescriptionItem(layout, "You are currently logged in as " + username + ". " +
                    "To log out, go to the Settings pane.");
        }
    }

    public void updateFavoritesLayout() {
        final List<String> favorites = CourseManager.sharedInstance().getFavoriteCourses();
        faveCard.removeAllViews();
        layoutBuilder.addHeaderItem(faveCard, "Favorites");
        if (favorites.size() == 0) {
            layoutBuilder.addCenteredDescriptionItem(faveCard, "You don't have any favorite subjects yet. " +
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
                    LinearLayout listItem = layoutBuilder.addCourseListItem(faveCard);
                    for (Course course : arg)
                        addCourseCell(listItem, course);
                }
            });
        }
    }

    public void updateRecentsLayout() {
        CourseSearchEngine.sharedInstance().getRecentCourses(new CourseSearchEngine.RecentCoursesCallback() {
            @Override
            public void result(List<Course> courses) {
                recentsCard.removeAllViews();
                layoutBuilder.addHeaderItem(recentsCard, "Recents");
                if (courses.size() == 0) {
                    layoutBuilder.addCenteredDescriptionItem(recentsCard, "You don't have any recent subjects yet. " +
                            "Find subjects from the search bar above.");
                } else {
                    LinearLayout listItem = layoutBuilder.addCourseListItem(recentsCard);
                    for (Course course : courses)
                        addCourseCell(listItem, course);
                }
            }
        });
    }

    private String titleForRecommendationsKey(String recommendationsKey) {
        if (recommendationsKey.equals(CourseManager.RECOMMENDATION_KEY_FOR_YOU))
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

    private void addCourseCell(LinearLayout layout, final Course course) {
        layoutBuilder.addCourseCell(layout, course, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate.get() != null) {
                    delegate.get().courseNavigatorWantsCourseDetails(ForYouFragment.this, course);
                }
            }
        }, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                addCourse(course);
                return true;
            }
        });
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
        if (delegate.get() != null)
            delegate.get().courseNavigatorAddedCourse(this, course, semester);
        addCourseDialog.dismiss();
        addCourseDialog = null;
    }

}
