package com.base12innovations.android.fireroad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.base12innovations.android.fireroad.dialog.AddCourseDialog;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.course.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.doc.NetworkManager;
import com.base12innovations.android.fireroad.models.req.RequirementsList;
import com.base12innovations.android.fireroad.models.req.RequirementsListManager;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.utils.RequirementsListDisplay;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.util.EnumSet;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RequirementsListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RequirementsListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RequirementsListFragment extends Fragment implements AddCourseDialog.AddCourseDialogDelegate {

    private static final String REQUIREMENTS_LIST_ID = "RequirementsListFragment.requirementsListID";
    private String requirementsListID;
    public RequirementsList requirementsList;
    private RequirementsListDisplay display;

    public OnFragmentInteractionListener delegate;
    private View mLayout;

    public RequirementsListFragment() {
        // Required empty public constructor
    }

    public static RequirementsListFragment newInstance(String listID) {
        RequirementsListFragment fragment = new RequirementsListFragment();
        Bundle args = new Bundle();
        args.putString(REQUIREMENTS_LIST_ID, listID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requirementsListID = getArguments().getString(REQUIREMENTS_LIST_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_requirements_list, container, false);
        mLayout = layout;
        if (requirementsListID != null) {
            RequirementsList reqList = RequirementsListManager.sharedInstance().getRequirementsList(requirementsListID);
            if (reqList != null) {
                requirementsList = reqList;
                reqList.loadIfNeeded();
                if (User.currentUser().getCurrentDocument() != null) {
                    requirementsList.setCurrentDoc(User.currentUser().getCurrentDocument());
                    requirementsList.computeRequirementStatus(User.currentUser().getCurrentDocument().getCreditCourses());
                }
                LinearLayout contentLayout = (LinearLayout)mLayout.findViewById(R.id.reqListLinearLayout);
                buildRequirementsListLayout(contentLayout);
            }
        }
        return layout;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        updateRequirementStatus();
    }

    // Layout

    private void buildRequirementsListLayout(LinearLayout layout) {
        layout.removeAllViews();
        if (requirementsList.getRequirements() == null)
            return;

        // Create a display object which will manage rendering the requirements into the layout
        display = new RequirementsListDisplay(requirementsList, getContext());
        display.delegate = new RequirementsListDisplay.Delegate() {
            @Override public void addCourse(Course course) {
                RequirementsListFragment.this.addCourse(course);
            }
            @Override public void showDetails(Course course) {
                if (delegate != null)
                    delegate.courseNavigatorWantsCourseDetails(RequirementsListFragment.this, course);
            }
            @Override public void searchCourses(String searchTerm, EnumSet<CourseSearchEngine.Filter> filters) {
                if (delegate != null)
                    delegate.courseNavigatorWantsSearchCourses(RequirementsListFragment.this, searchTerm, filters);
            }
            @Override public void showManualProgressSelector(RequirementsListStatement req) {
                RequirementsListFragment.this.showManualProgressSelector(req);
            }
            @Override public Activity getActivity(){
                return RequirementsListFragment.this.getActivity();
            }
        };

        if (requirementsList.title != null && requirementsList.title.length() > 0) {
            display.getLayoutBuilder().addHeaderItem(layout, requirementsList.title);
        }
        if (requirementsList.contentDescription != null && requirementsList.contentDescription.length() > 0) {
            display.getLayoutBuilder().addDescriptionItem(layout, requirementsList.contentDescription);
        }
        display.getLayoutBuilder().addToggleCourseItem(layout,
                "Add to My Courses",
                "Remove from My Courses",
                User.currentUser().getCurrentDocument() != null &&
                        User.currentUser().getCurrentDocument().coursesOfStudy.contains(requirementsListID),
                User.currentUser().getCurrentDocument() != null ?
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                String titleToShow = requirementsList.mediumTitle != null ? requirementsList.mediumTitle : requirementsList.shortTitle;
                                if (b) {
                                    User.currentUser().getCurrentDocument().addCourseOfStudy(requirementsListID);
                                    Snackbar.make(mLayout, "Added " + titleToShow + " to my courses", Snackbar.LENGTH_SHORT).show();
                                } else {
                                    User.currentUser().getCurrentDocument().removeCourseOfStudy(requirementsListID);
                                    Snackbar.make(mLayout, "Removed " + titleToShow + " from my courses", Snackbar.LENGTH_SHORT).show();
                                }
                                if (delegate != null)
                                    delegate.fragmentUpdatedCoursesOfStudy(RequirementsListFragment.this);
                            }
                        } : null);

        display.layoutInView(layout);
        if (requirementsList.webURL != null) {
            display.getLayoutBuilder().addButtonItem(layout, "View on Catalog Site", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(requirementsList.webURL.toString()));
                    startActivity(i);
                }
            });
        }
        display.getLayoutBuilder().addButtonItem(layout, "Request a Correction", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(NetworkManager.CATALOG_BASE_URL + "requirements/edit/" + requirementsListID));
                startActivity(i);
            }
        });
    }

    private void showManualProgressSelector(final RequirementsListStatement req) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Set progress");
        LayoutInflater inflater = getLayoutInflater();
        View customView = inflater.inflate(R.layout.dialog_manual_progress, null, false);
        builder.setView(customView);

        final SeekBar seekBar = customView.findViewById(R.id.seekBar);
        final TextView label = customView.findViewById(R.id.progressTextView);
        seekBar.setMax(req.threshold.cutoff);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            seekBar.setMin(0);
        seekBar.setProgress(req.getManualProgress());
        label.setText(String.format(Locale.US, "%d/%d %s", req.getManualProgress(), req.threshold.cutoff,
                req.threshold.criterion == RequirementsListStatement.ThresholdCriterion.UNITS ? "units" : "subjects"));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (req.threshold.criterion == RequirementsListStatement.ThresholdCriterion.UNITS &&
                        progress % 3 != 0) {
                    seekBar.setProgress((int)(Math.round((float)progress / 3.0f) * 3));
                } else {
                    label.setText(String.format(Locale.US, "%d/%d %s", progress, req.threshold.cutoff,
                            req.threshold.criterion == RequirementsListStatement.ThresholdCriterion.UNITS ? "units" : "subjects"));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int newValue = seekBar.getProgress();
                if (newValue != req.getManualProgress()) {
                    req.setManualProgress(newValue);
                    updateRequirementStatus();
                }
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener extends CourseNavigatorDelegate {
        void fragmentUpdatedCoursesOfStudy(RequirementsListFragment fragment);
    }

    public void updateRequirementStatus() {
        if (requirementsList != null) {
            if (User.currentUser().getCurrentDocument() != null) {
                requirementsList.setCurrentDoc(User.currentUser().getCurrentDocument());
                requirementsList.computeRequirementStatus(User.currentUser().getCurrentDocument().getCreditCourses());
            }
            display.updateRequirementsDisplay();
        }
    }

    // Add course dialog

    AddCourseDialog addCourseDialog;

    private void addCourse(final Course course) {
        TaskDispatcher.perform(new TaskDispatcher.Task<Boolean>() {
            @Override
            public Boolean perform() {
                Course realCourse = CourseManager.sharedInstance().getSubjectByID(course.getSubjectID());
                return realCourse != null && realCourse.equals(course);
            }
        }, new TaskDispatcher.CompletionBlock<Boolean>() {
            @Override
            public void completed(Boolean arg) {
                addCourseDialog = new AddCourseDialog();
                addCourseDialog.course = course;
                addCourseDialog.delegate = RequirementsListFragment.this;
                FragmentActivity a = getActivity();
                if (a != null) {
                    addCourseDialog.show(a.getSupportFragmentManager(), "AddCourseFragment");
                }
            }
        });
    }

    @Override
    public void addCourseDialogDismissed() {
        addCourseDialog.dismiss();
        addCourseDialog = null;
    }

    @Override
    public void addCourseDialogAddedToSemester(Course course, int semester) {
        if (delegate != null)
            delegate.courseNavigatorAddedCourse(this, course, semester);
        addCourseDialog.dismiss();
        addCourseDialog = null;
    }
}
