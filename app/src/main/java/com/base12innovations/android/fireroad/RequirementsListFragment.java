package com.base12innovations.android.fireroad;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.base12innovations.android.fireroad.dialog.AddCourseDialog;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.course.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.req.RequirementsList;
import com.base12innovations.android.fireroad.models.req.RequirementsListManager;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.utils.CourseLayoutBuilder;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RequirementsListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RequirementsListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RequirementsListFragment extends Fragment implements AddCourseDialog.AddCourseDialogDelegate {

    private static String REQUIREMENTS_LIST_ID = "RequirementsListFragment.requirementsListID";
    private String requirementsListID;
    public RequirementsList requirementsList;

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
                if (User.currentUser().getCurrentDocument() != null)
                    requirementsList.computeRequirementStatus(User.currentUser().getCurrentDocument().getAllCourses());
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

    // Layout logic

    enum CellType {
        TITLE, TITLE_1, TITLE_2, DESCRIPTION, COURSE_LIST, URL
    }

    private class PresentationItem {
        CellType cellType;
        RequirementsListStatement statement;
        String text;

        PresentationItem(CellType type, RequirementsListStatement stmt, String text) {
            this.cellType = type;
            this.statement = stmt;
            this.text = text;
        }
    }

    private List<PresentationItem> presentationItemsForRequirement(RequirementsListStatement requirement, int level, boolean alwaysShowTitle) {
        List<PresentationItem> items = new ArrayList<>();
        if (requirement.title != null) {
            CellType cellType = (level <= 2) ? CellType.TITLE_1 : CellType.TITLE_2;
            String titleText = requirement.title;
            if (requirement.getThresholdDescription().length() > 0 &&
                    requirement.connectionType != RequirementsListStatement.ConnectionType.ALL &&
                    !requirement.isPlainString) {
                titleText += " (" + requirement.getThresholdDescription() + ")";
            }
            items.add(new PresentationItem(cellType, requirement, titleText));
        } else if (requirement.getThresholdDescription().length() > 0 &&
                (requirement.connectionType != RequirementsListStatement.ConnectionType.ALL || alwaysShowTitle) &&
                !requirement.isPlainString) {
            String desc = requirement.getThresholdDescription();
            items.add(new PresentationItem(CellType.TITLE_2, requirement, desc.substring(0, 1).toUpperCase() + desc.substring(1) + ":"));
        }
        if (requirement.contentDescription != null && requirement.contentDescription.length() > 0) {
            items.add(new PresentationItem(CellType.DESCRIPTION, requirement, requirement.contentDescription));
        }

        if (level == 0 && requirement.title == null && requirement.getThresholdDescription().length() > 0 &&
                (requirement.connectionType != RequirementsListStatement.ConnectionType.ALL || alwaysShowTitle)) {
            String desc = requirement.getThresholdDescription();
            items.add(new PresentationItem(CellType.TITLE_2, requirement, desc.substring(0, 1).toUpperCase() + desc.substring(1) + ":"));
        }
        boolean added = false;
        if (requirement.minimumNestDepth() <= 1 && (requirement.maximumNestDepth() <= 2 || level > 0)) {
            boolean hasTitle = false;
            if (requirement.getRequirements() != null) {
                for (RequirementsListStatement req : requirement.getRequirements()) {
                    if (req.title != null && req.title.length() > 0) {
                        hasTitle = true;
                        break;
                    }
                }
            }
            if (!hasTitle) {
                added = true;
                items.add(new PresentationItem(CellType.COURSE_LIST, requirement, null));
            }
        }
        if (!added && requirement.getRequirements() != null) {
            boolean containsAny = false, containsAll = false;
            for (RequirementsListStatement req : requirement.getRequirements()) {
                if (req.connectionType == RequirementsListStatement.ConnectionType.ALL && req.getRequirements() != null && req.getRequirements().size() > 0)
                    containsAll = true;
                if (req.connectionType == RequirementsListStatement.ConnectionType.ANY)
                    containsAny = true;
            }

            for (RequirementsListStatement req : requirement.getRequirements()) {
                items.addAll(presentationItemsForRequirement(req, level + 1, containsAll && containsAny));
            }
        }
        return items;
    }


    // Layout building

    private Map<RequirementsListStatement, View> headerCells;
    private Map<RequirementsListStatement, View> courseListCells;
    private CourseLayoutBuilder layoutBuilder;

    private void buildRequirementsListLayout(LinearLayout layout) {
        layout.removeAllViews();
        headerCells = new HashMap<>();
        courseListCells = new HashMap<>();

        if (layoutBuilder == null) {
            layoutBuilder = new CourseLayoutBuilder(getContext());
            layoutBuilder.defaultMargin = (int)getResources().getDimension(R.dimen.requirements_card_padding);
            layoutBuilder.showHeadingTopMargin = true;
        }

        if (requirementsList.title != null && requirementsList.title.length() > 0) {
            layoutBuilder.addHeaderItem(layout, requirementsList.title);
        }
        if (requirementsList.contentDescription != null && requirementsList.contentDescription.length() > 0) {
            layoutBuilder.addDescriptionItem(layout, requirementsList.contentDescription);
        }
        layoutBuilder.addToggleCourseItem(layout,
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

        if (requirementsList.getRequirements() == null)
            return;

        if (requirementsList.maximumNestDepth() <= 1) {
            addCard(layout, presentationItemsForRequirement(requirementsList, 0, false));
        } else {
            for (RequirementsListStatement topLevelReq : requirementsList.getRequirements()) {
                List<PresentationItem> items = presentationItemsForRequirement(topLevelReq, 0, false);

                // Remove title and add back with threshold information separate
                if (topLevelReq.title != null) {
                    items.remove(0);
                    items.add(0, new PresentationItem(CellType.TITLE, topLevelReq, topLevelReq.title));
                    int indexToInsert = 1;
                    if (items.size() > indexToInsert && items.get(indexToInsert).cellType == CellType.DESCRIPTION)
                        indexToInsert += 1;
                    String desc = topLevelReq.getThresholdDescription();
                    if (desc != null && desc.length() > 0)
                        items.add(indexToInsert, new PresentationItem(CellType.TITLE_2, topLevelReq, desc.substring(0, 1).toUpperCase() + desc.substring(1) + ":"));
                }

                addCard(layout, items);
            }
        }

        // Add URL here
    }

    public void updateRequirementStatus() {
        if (requirementsList != null) {
            if (User.currentUser().getCurrentDocument() != null)
                requirementsList.computeRequirementStatus(User.currentUser().getCurrentDocument().getAllCourses());
            updateRequirementStatus(requirementsList);
        }
    }

    public void updateRequirementStatus(RequirementsListStatement statement) {
        if (headerCells == null || courseListCells == null) return;
        if (headerCells.containsKey(statement)) {
            layoutBuilder.updateSubHeaderProgress(headerCells.get(statement), statement.percentageFulfilled());
        }
        if (courseListCells.containsKey(statement)) {
            formatCourseCellFulfillmentIndicator(courseListCells.get(statement), statement.getFulfillmentProgress());
        }
        if (statement.getRequirements() != null) {
            for (RequirementsListStatement subReq : statement.getRequirements())
                updateRequirementStatus(subReq);
        }
    }

    private void addCard(final LinearLayout layout, List<PresentationItem> items, boolean nested, int rowIndex) {
        LinearLayout card = layoutBuilder.addCard(layout, nested, rowIndex);

        // Add the presentation items
        for (PresentationItem item : items) {
            switch (item.cellType) {
                case TITLE_2:
                    layoutBuilder.addSubHeaderItem(card, item.text, 0.0f, textSize(item.cellType));
                    break;
                case TITLE:
                case TITLE_1:
                    View headerView = layoutBuilder.addSubHeaderItem(card, item.text, item.statement.percentageFulfilled(), textSize(item.cellType));
                    headerCells.put(item.statement, headerView);
                    break;
                case DESCRIPTION:
                    layoutBuilder.addDescriptionItem(card, item.text);
                    break;
                case COURSE_LIST:
                    if (item.statement.getRequirements() == null)
                        addCourseListItem(card, Arrays.asList(item.statement));
                    else
                        addCourseListItem(card, item.statement.getRequirements());
                    break;
                default:
                    break;
            }
        }
    }

    private void addCard(LinearLayout layout, List<PresentationItem> items) {
        addCard(layout, items, false, -1);
    }

    private float textSize(CellType cellType) {
        switch (cellType) {
            case TITLE:
                return 22.0f;
            case TITLE_1:
                return 18.0f;
            case TITLE_2:
                return 16.0f;
            default:
                return 14.0f;
        }
    }

    private void addCourseListItem(final LinearLayout layout, final List<RequirementsListStatement> requirements) {
        final int rowIndex = layout.getChildCount() - 1;
        final LinearLayout listLayout = layoutBuilder.addCourseListItem(layout);
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                final List<Course> courses = new ArrayList<>();
                for (RequirementsListStatement req : requirements) {
                    String shortDesc = req.getShortDescription();
                    Course newCourse = CourseManager.sharedInstance().getSubjectByID(shortDesc);
                    if (newCourse != null) {
                        courses.add(newCourse);
                    } else {
                        String[] words = shortDesc.split("\\s+");
                        Course course = new Course();
                        Course.GIRAttribute gir = Course.GIRAttribute.fromRaw(shortDesc);
                        if (gir != null) {
                            course.setSubjectID("GIR");
                            course.subjectTitle = gir.toString().replaceAll("GIR", "").trim();
                        } else if (words.length > 0 && words[0].contains(".")) {
                            course.setSubjectID(words[0]);
                            course.subjectTitle = shortDesc.substring(words[0].length());
                        } else if (shortDesc.length() > 10) {
                            course.setSubjectID("");
                            course.subjectTitle = shortDesc;
                        } else {
                            course.setSubjectID(shortDesc);
                            course.subjectTitle = "";
                        }
                        courses.add(course);
                    }
                }

                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        for (int i = 0; i < courses.size(); i++) {
                            final Course course = courses.get(i);
                            final RequirementsListStatement statement = requirements.get(i);
                            final View cell = layoutBuilder.addCourseCell(listLayout, course,
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            onClickCourseCell(layout, rowIndex, view, course, statement);
                                        }
                                    }, new View.OnLongClickListener() {
                                        @Override
                                        public boolean onLongClick(View view) {
                                            addCourse(course);
                                            return true;
                                        }
                                    });
                            courseListCells.put(requirements.get(i), cell);
                            formatCourseCellFulfillmentIndicator(cell, statement.getFulfillmentProgress());
                        }
                    }
                });
            }
        });
    }

    private void formatCourseCellFulfillmentIndicator(View courseThumbnail, RequirementsListStatement.FulfillmentProgress progress) {
        ProgressBar pBar = courseThumbnail.findViewById(R.id.requirementsProgressBar);
        if (progress != null) {
            if (progress.getProgress() == progress.getMax())
                courseThumbnail.setAlpha(0.5f);
            else
                courseThumbnail.setAlpha(1.0f);

            if (progress.getMax() != 1) {
                pBar.setVisibility(View.VISIBLE);
                pBar.setMax(progress.getMax());
                pBar.setProgress(progress.getProgress());
            } else {
                pBar.setVisibility(View.GONE);
            }
        } else {
            pBar.setVisibility(View.GONE);
        }
    }

    private void onClickCourseCell(final LinearLayout layout, final int rowIndex, View thumbnail, final Course course, final RequirementsListStatement req) {
        TaskDispatcher.perform(new TaskDispatcher.Task<Boolean>() {
            @Override
            public Boolean perform() {
                Course realCourse = CourseManager.sharedInstance().getSubjectByID(course.getSubjectID());
                return realCourse != null && realCourse.equals(course);
            }
        }, new TaskDispatcher.CompletionBlock<Boolean>() {
            @Override
            public void completed(Boolean arg) {
                if (arg) {
                    // It's a real course, show details
                    if (delegate != null) {
                        delegate.courseNavigatorWantsCourseDetails(RequirementsListFragment.this, course);
                    }
                } else if (req.isPlainString) {
                    // Show progress selector
                    showManualProgressSelector(req);
                } else if (req.requirement != null) {
                    // Search
                    String reqString = req.requirement.replaceAll("GIR:", "");

                    // Set up requirement filters based on the content of the string
                    EnumSet<CourseSearchEngine.Filter> filters = CourseSearchEngine.Filter.noFilter;
                    Course.GIRAttribute gir = Course.GIRAttribute.fromRaw(reqString);
                    Course.HASSAttribute hass = Course.HASSAttribute.fromRaw(reqString);
                    Course.CommunicationAttribute ci = Course.CommunicationAttribute.fromRaw(reqString);
                    if (ci != null) {
                        if (ci == Course.CommunicationAttribute.CI_H)
                            CourseSearchEngine.Filter.filterCI(filters, CourseSearchEngine.Filter.CI_H);
                        else if (ci == Course.CommunicationAttribute.CI_HW)
                            CourseSearchEngine.Filter.filterCI(filters, CourseSearchEngine.Filter.CI_HW);
                    } else if (hass != null) {
                        CourseSearchEngine.Filter baseOption = CourseSearchEngine.Filter.HASS_NONE;
                        switch (hass) {
                            case ANY:
                                baseOption = CourseSearchEngine.Filter.HASS;
                                break;
                            case ARTS:
                                baseOption = CourseSearchEngine.Filter.HASS_A;
                                break;
                            case HUMANITIES:
                                baseOption = CourseSearchEngine.Filter.HASS_H;
                                break;
                            case SOCIAL_SCIENCES:
                                baseOption = CourseSearchEngine.Filter.HASS_S;
                                break;
                        }
                        CourseSearchEngine.Filter.filterHASS(filters, baseOption);
                    } else if (gir != null) {
                        CourseSearchEngine.Filter.filterGIR(filters, CourseSearchEngine.Filter.GIR);
                    }
                    CourseSearchEngine.Filter.filterSearchField(filters, CourseSearchEngine.Filter.SEARCH_REQUIREMENTS);

                    if (delegate != null) {
                        delegate.courseNavigatorWantsSearchCourses(RequirementsListFragment.this, reqString, filters);
                    }
                } else {
                    // Sub-requirements page
                    addCard(layout, presentationItemsForRequirement(req, 1, false), true, rowIndex + 1);
                }
            }
        });
        //addCard(layout, Arrays.asList(new PresentationItem(CellType.COURSE_LIST, req, null)), true);
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
