package com.base12innovations.android.fireroad.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.base12innovations.android.fireroad.CourseNavigatorDelegate;
import com.base12innovations.android.fireroad.MyRoadFragment;
import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.RequirementsListFragment;
import com.base12innovations.android.fireroad.dialog.MarkerDialogFragment;
import com.base12innovations.android.fireroad.dialog.RequirementsOverrideDialog;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.course.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.models.req.ProgressAssertion;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for displaying a requirements list statement in a linear layout. Has a Delegate that
 * responds appropriately to taps/long taps on course thumbnails, such as searching for courses,
 * adding them, or adjusting manual progress.
 */
public class RequirementsListDisplay implements PopupMenu.OnMenuItemClickListener, RequirementsOverrideDialog.RequirementsOverrideDialogDelegate {

    public RequirementsListStatement requirementsList;
    /** Whether to always display on one row */
    public boolean singleCard = false;

    private CourseLayoutBuilder layoutBuilder;
    private PopupMenu currentPopupMenu;
    private Course currentlySelectedCourse;
    private RequirementsListStatement currentlySelectedRequirement;
    private RequirementsOverrideDialog requirementsOverrideDialog;

    private Context _context;

    public RequirementsListDisplay(RequirementsListStatement list, Context context) {
        requirementsList = list;
        _context = context;
    }

    public CourseLayoutBuilder getLayoutBuilder() {
        if (layoutBuilder == null) {
            layoutBuilder = new CourseLayoutBuilder(_context);
            layoutBuilder.defaultMargin = (int)_context.getResources().getDimension(R.dimen.requirements_card_padding);
            layoutBuilder.showHeadingTopMargin = true;
        }
        return layoutBuilder;
    }

    public interface Delegate {
        void addCourse(Course course);
        void showDetails(Course course);
        void searchCourses(String searchTerm, EnumSet<CourseSearchEngine.Filter> filters);
        void showManualProgressSelector(RequirementsListStatement req);
        Activity getActivity();
    }

    public Delegate delegate;

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

        @Override
        public String toString() {
            return "<PresentationItem " + cellType + " - " + text + ">";
        }
    }

    private List<PresentationItem> presentationItemsForRequirement(RequirementsListStatement requirement, int level, boolean alwaysShowTitle) {
        List<PresentationItem> items = new ArrayList<>();
        boolean addedTitle = false;
        if (requirement.title != null && requirement.title.length() > 0) {
            CellType cellType = (level <= 2) ? CellType.TITLE_1 : CellType.TITLE_2;
            String titleText = requirement.title;
            if (requirement.getThresholdDescription().length() > 0 &&
                    requirement.connectionType != RequirementsListStatement.ConnectionType.ALL &&
                    !requirement.isPlainString) {
                titleText += " (" + requirement.getThresholdDescription() + ")";
            }
            items.add(new PresentationItem(cellType, requirement, titleText));
            addedTitle = true;
        } else if (requirement.getThresholdDescription().length() > 0 &&
                (requirement.connectionType != RequirementsListStatement.ConnectionType.ALL || alwaysShowTitle) &&
                !requirement.isPlainString) {
            String desc = requirement.getThresholdDescription();
            items.add(new PresentationItem(CellType.TITLE_2, requirement, desc.substring(0, 1).toUpperCase() + desc.substring(1) + ":"));
            addedTitle = true;
        }
        if (requirement.contentDescription != null && requirement.contentDescription.length() > 0) {
            items.add(new PresentationItem(CellType.DESCRIPTION, requirement, requirement.contentDescription));
        }

        // If top level and didn't add a title before, add one
        if (level == 0 && !addedTitle &&
                requirement.getThresholdDescription().length() > 0 &&
                (requirement.connectionType != RequirementsListStatement.ConnectionType.ALL || alwaysShowTitle)) {
            String desc = requirement.getThresholdDescription();
            items.add(new PresentationItem(CellType.TITLE_2, requirement, desc.substring(0, 1).toUpperCase() + desc.substring(1) + ":"));
        }
        boolean added = false;
        if (requirement.minimumNestDepth() <= 1 ||
                (requirement.getRequirements() != null && ListHelper.containsElement(requirement.getRequirements(), new ListHelper.Predicate<RequirementsListStatement>() {
                    @Override
                    public boolean test(RequirementsListStatement element) {
                        return element.requirement != null;
                    }
                }))) {
            boolean shouldCollapse = true; // Whether to show on one row
            if (requirement.getRequirements() != null) {
                for (RequirementsListStatement req : requirement.getRequirements()) {
                    if ((req.title != null && req.title.length() > 0)) {
                        shouldCollapse = false;
                        break;
                    }
                }
            }
            if (shouldCollapse) {
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
    private Set<RequirementsListStatement> visibleNestedReqs;

    /**
     * Renders the requirements list statement in the given linear
     * layout view.
     * @param layout the layout in which to render
     */
    public void layoutInView(LinearLayout layout) {

        //layout.removeAllViews();
        headerCells = new HashMap<>();
        courseListCells = new HashMap<>();

        if (requirementsList.maximumNestDepth() <= 1 || singleCard) {
            List<PresentationItem> items = presentationItemsForRequirement(requirementsList, 0, false);
            addCard(layout, items);
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

    /**
     * Updates the display of the requirements after a change in their status.
     */
    public void updateRequirementsDisplay() {
        if (requirementsList != null) {
            updateRequirementsDisplay(requirementsList);
        }
    }

    public void updateRequirementsDisplay(RequirementsListStatement statement) {
        if (headerCells == null || courseListCells == null) return;
        if (headerCells.containsKey(statement)) {
            getLayoutBuilder().updateSubHeaderProgress(headerCells.get(statement), statement.percentageFulfilled());
        }
        if (courseListCells.containsKey(statement)) {
            formatCourseCellFulfillmentIndicator(courseListCells.get(statement), statement.getFulfillmentProgress());
        }
        if (statement.getRequirements() != null) {
            for (RequirementsListStatement subReq : statement.getRequirements())
                updateRequirementsDisplay(subReq);
        }
    }

    private void addCard(final LinearLayout layout, List<PresentationItem> items, int rowIndex, View.OnClickListener nestedListener) {
        LinearLayout card = getLayoutBuilder().addCard(layout, rowIndex, nestedListener);

        // Add the presentation items
        for (PresentationItem item : items) {
            switch (item.cellType) {
                case TITLE_2:
                    getLayoutBuilder().addSubHeaderItem(card, item.text, 0.0f, textSize(item.cellType));
                    break;
                case TITLE:
                case TITLE_1:
                    View headerView = getLayoutBuilder().addSubHeaderItem(card, item.text, item.statement.percentageFulfilled(), textSize(item.cellType));
                    headerCells.put(item.statement, headerView);
                    break;
                case DESCRIPTION:
                    getLayoutBuilder().addDescriptionItem(card, item.text);
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
        addCard(layout, items, -1, null);
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
        final int rowIndex = layout.getChildCount();
        final LinearLayout listLayout = getLayoutBuilder().addCourseListItem(layout);
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                final List<Course> courses = new ArrayList<>();
                for (RequirementsListStatement req : requirements) {
                    String shortDesc = req.getShortDescription();
                    Course newCourse = CourseManager.sharedInstance().getSubjectByID(shortDesc);
                    if (newCourse != null && !newCourse.isGeneric) {
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
                            final View cell = getLayoutBuilder().addCourseCell(listLayout, course,
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            onClickCourseCell(layout, rowIndex, view, course, statement);
                                        }
                                    }, new View.OnLongClickListener() {
                                        @Override
                                        public boolean onLongClick(View view) {
                                            TaskDispatcher.perform(new TaskDispatcher.Task<Boolean>() {
                                                @Override
                                                public Boolean perform() {
                                                    Course realCourse = CourseManager.sharedInstance().getSubjectByID(course.getSubjectID());
                                                    return realCourse != null && realCourse.equals(course) && !realCourse.isGeneric;
                                                }
                                            }, new TaskDispatcher.CompletionBlock<Boolean>() {
                                                @Override
                                                public void completed(Boolean arg) {
                                                    if (arg && delegate != null) {
                                                        delegate.addCourse(course);
                                                    }
                                                }
                                            });
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

    private void onClickCourseCell(final LinearLayout layout, final int rowIndex, final View thumbnail, final Course course, final RequirementsListStatement req) {
        TaskDispatcher.perform(new TaskDispatcher.Task<Boolean>() {
            @Override
            public Boolean perform() {
                Course realCourse = CourseManager.sharedInstance().getSubjectByID(course.getSubjectID());
                return realCourse != null && realCourse.equals(course) && !realCourse.isGeneric;
            }
        }, new TaskDispatcher.CompletionBlock<Boolean>() {
            @Override
            public void completed(Boolean arg) {
                if (arg) {
                    System.out.println("Case 1");
                    // It's a real course, show details
                    /*
                    if (delegate != null) {
                        delegate.showDetails(course);
                        //delegate.courseNavigatorWantsCourseDetails(RequirementsListFragment.this, course);
                    }*/
                    if(delegate != null) {
                        currentlySelectedCourse = course;
                        currentlySelectedRequirement = req;
                        final PopupMenu menu = new PopupMenu(delegate.getActivity(),thumbnail);
                        MenuInflater mInflater = menu.getMenuInflater();
                        mInflater.inflate(R.menu.menu_course_requirements_cell, menu.getMenu());

                        final MenuItem owOn = menu.getMenu().findItem(R.id.overrideOn);
                        final MenuItem owOff = menu.getMenu().findItem(R.id.overrideOff);
                        final MenuItem owInfo = menu.getMenu().findItem(R.id.viewOverride);
                        menu.getMenu().findItem(R.id.viewCourse).setVisible(course.isPublic);
                        menu.getMenu().findItem(R.id.addCourse).setVisible(course.isPublic);
                        owOn.setVisible(course.isPublic);
                        owOff.setVisible(course.isPublic);
                        owInfo.setVisible(course.isPublic);

                        if(course.isPublic){
                            boolean overrideStatus = User.currentUser().getCurrentDocument().getProgressOverride(req.keyPath()) != null;
                            if(overrideStatus){
                                owOn.setVisible(false);
                            }else{
                                owOff.setVisible(false);
                                owInfo.setVisible(false);
                            }
                        }

                        /*
                        if (course.isPublic) {
                            List<RoadDocument.Warning> warnings = User.currentUser().getCurrentDocument().warningsForCourseCached(course);
                            if (warnings == null) {
                                TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                                    @Override
                                    public void perform() {
                                        final List<RoadDocument.Warning> newWarnings = User.currentUser().getCurrentDocument().warningsForCourse(course);
                                        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                                            @Override
                                            public void perform() {
                                                menu.getMenu().findItem(R.id.courseWarnings).setEnabled(newWarnings.size() > 0);
                                                if (newWarnings.size() == 0) {
                                                    owOn.setEnabled(false);
                                                    owOff.setEnabled(false);
                                                }
                                            }
                                        });
                                    }
                                });
                            } else {
                                menu.getMenu().findItem(R.id.courseWarnings).setEnabled(warnings.size() > 0);
                                if (warnings.size() == 0) {
                                    owOn.setEnabled(false);
                                    owOff.setEnabled(false);
                                }
                            }
                            if (User.currentUser().getCurrentDocument().overrideWarningsForCourse(course)) {
                                owOn.setVisible(false);
                            } else {
                                owOff.setVisible(false);
                            }
                        }
                        */
                        menu.setOnMenuItemClickListener(RequirementsListDisplay.this);
                        menu.show();
                        currentPopupMenu = menu;
                    }


                } else if (req.isPlainString) {
                    System.out.println("Case 3");
                    // Show progress selector
                    if (delegate != null)
                        delegate.showManualProgressSelector(req);
                } else if (req.requirement != null) {
                    System.out.println("Case 4");
                    // Search
                    String reqString = req.requirement.replaceAll("GIR:", "");

                    // Set up requirement filters based on the content of the string
                    EnumSet<CourseSearchEngine.Filter> filters = CourseSearchEngine.Filter.noFilter();
                    Course.GIRAttribute gir = Course.GIRAttribute.fromRaw(reqString);
                    Course.HASSAttribute hass = Course.HASSAttribute.fromRaw(reqString);
                    Course.CommunicationAttribute ci = Course.CommunicationAttribute.fromRaw(reqString);
                    if (ci != null) {
                        if (ci == Course.CommunicationAttribute.CI_H)
                            CourseSearchEngine.Filter.filterCI(filters, CourseSearchEngine.Filter.CI_H);
                        else if (ci == Course.CommunicationAttribute.CI_HW)
                            CourseSearchEngine.Filter.filterCI(filters, CourseSearchEngine.Filter.CI_HW);
                        reqString = "";
                    } else if (hass != null) {
                        CourseSearchEngine.Filter baseOption = CourseSearchEngine.Filter.HASS_NONE;
                        switch (hass) {
                            case ARTS:
                                baseOption = CourseSearchEngine.Filter.HASS_A;
                                break;
                            case HUMANITIES:
                                baseOption = CourseSearchEngine.Filter.HASS_H;
                                break;
                            case SOCIAL_SCIENCES:
                                baseOption = CourseSearchEngine.Filter.HASS_S;
                                break;
                            default:
                                baseOption = CourseSearchEngine.Filter.HASS;
                                break;
                        }
                        CourseSearchEngine.Filter.filterHASS(filters, baseOption);
                        reqString = "";
                    } else if (gir != null) {
                        CourseSearchEngine.Filter.filterGIR(filters, CourseSearchEngine.Filter.GIR);
                        //reqString = "";
                    }
                    CourseSearchEngine.Filter.filterSearchField(filters, CourseSearchEngine.Filter.SEARCH_REQUIREMENTS);

                    if (delegate != null) {
                        delegate.searchCourses(reqString, filters);
                    }
                } else {
                    System.out.println("Case 5");
                    // Sub-requirements page
                    if (visibleNestedReqs == null)
                        visibleNestedReqs = new HashSet<>();
                    if (!visibleNestedReqs.contains(req)) {
                        visibleNestedReqs.add(req);
                        addCard(layout, presentationItemsForRequirement(req, 1, false), rowIndex + 1, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (visibleNestedReqs != null) {
                                    markNestedReqsInvisible(req);
                                }
                            }
                        });
                    }
                }
            }
        });
        //addCard(layout, Arrays.asList(new PresentationItem(CellType.COURSE_LIST, req, null)), true);
    }

    private void markNestedReqsInvisible(RequirementsListStatement req) {
        visibleNestedReqs.remove(req);
        if (req.getRequirements() != null) {
            for (RequirementsListStatement subReq : req.getRequirements())
                markNestedReqsInvisible(subReq);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        currentPopupMenu.dismiss();
        currentPopupMenu = null;
        switch (menuItem.getItemId()) {
            case R.id.viewCourse:
                delegate.showDetails(currentlySelectedCourse);
                return true;
            case R.id.addCourse:
                delegate.addCourse(currentlySelectedCourse);
                return true;
            case R.id.overrideOn:
                //User.currentUser().getCurrentDocument().setOverrideWarningsForCourse(currentlySelectedCourse, true);
                //gridAdapter.notifyItemChanged(currentlySelectedPosition);
                requirementsOverrideDialog = new RequirementsOverrideDialog();
                requirementsOverrideDialog.setOverrideStatus(true);
                return true;
            case R.id.overrideOff:
                //User.currentUser().getCurrentDocument().setOverrideWarningsForCourse(currentlySelectedCourse, false);
                //gridAdapter.notifyItemChanged(currentlySelectedPosition);
                requirementsOverrideDialog = new RequirementsOverrideDialog();
                requirementsOverrideDialog.setOverrideStatus(false);
                return true;
            case R.id.viewOverride:
                requirementsOverrideDialog = new RequirementsOverrideDialog();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void requirementsOverrideDialogDismissed(){
        requirementsOverrideDialog.dismiss();
        requirementsOverrideDialog = null;
    }
    @Override
    public void requirementsOverrideDialogEditOverride(boolean overriden, List<Course> courses){
        requirementsOverrideDialog.dismiss();
        requirementsOverrideDialog = null;
        User.currentUser().getCurrentDocument().setProgressOverride(currentlySelectedRequirement.keyPath(), new ProgressAssertion(courses));
    }
}
