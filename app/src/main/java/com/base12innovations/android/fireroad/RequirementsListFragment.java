package com.base12innovations.android.fireroad;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.base12innovations.android.fireroad.models.ColorManager;
import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.CourseManager;
import com.base12innovations.android.fireroad.models.RequirementsList;
import com.base12innovations.android.fireroad.models.RequirementsListManager;
import com.base12innovations.android.fireroad.models.RequirementsListStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RequirementsListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RequirementsListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RequirementsListFragment extends Fragment {

    private static String REQUIREMENTS_LIST_ID = "RequirementsListFragment.requirementsListID";
    private String requirementsListID;
    public RequirementsList requirementsList;

    public OnFragmentInteractionListener delegate;

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
        if (requirementsListID != null) {
            RequirementsList reqList = RequirementsListManager.sharedInstance().getRequirementsList(requirementsListID);
            if (reqList != null) {
                requirementsList = reqList;
                reqList.loadIfNeeded();
                LinearLayout contentLayout = (LinearLayout)layout.findViewById(R.id.reqListLinearLayout);
                buildRequirementsListLayout(contentLayout);
            }
        }
        return layout;
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

    private void buildRequirementsListLayout(LinearLayout layout) {
        if (requirementsList.title != null && requirementsList.title.length() > 0) {
            addHeaderItem(layout, requirementsList.title);
        }
        if (requirementsList.contentDescription != null && requirementsList.contentDescription.length() > 0) {
            addDescriptionItem(layout, requirementsList.contentDescription);
        }

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
                    items.add(0, new PresentationItem(CellType.TITLE, null, topLevelReq.title));
                    int indexToInsert = 1;
                    if (items.size() > indexToInsert && items.get(indexToInsert).cellType == CellType.DESCRIPTION)
                        indexToInsert += 1;
                    String desc = topLevelReq.getThresholdDescription();
                    if (desc != null && desc.length() > 0)
                        items.add(indexToInsert, new PresentationItem(CellType.TITLE_2, null, desc.substring(0, 1).toUpperCase() + desc.substring(1) + ":"));
                }

                addCard(layout, items);
            }
        }

        // Add URL here
    }

    private void addCard(final LinearLayout layout, List<PresentationItem> items, boolean nested, int rowIndex) {
        int margin = (int) getResources().getDimension(R.dimen.course_details_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, margin, margin, margin / 2);
        final LinearLayout card = (LinearLayout)LayoutInflater.from(getContext()).inflate(R.layout.requirements_card, null);
        if (rowIndex == -1)
            layout.addView(card);
        else
            layout.addView(card, rowIndex);
        if (nested) {
            card.setBackgroundResource(R.drawable.requirements_nested_card_background);
            card.setElevation(0.0f);
            addCloseButtonItem(card, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    layout.removeView(card);
                }
            });
        }
        card.setLayoutParams(lparams);

        // Add the presentation items
        for (PresentationItem item : items) {
            switch (item.cellType) {
                case TITLE:
                case TITLE_1:
                case TITLE_2:
                    addSubHeaderItem(card, item.text, textSize(item.cellType));
                    break;
                case DESCRIPTION:
                    addDescriptionItem(card, item.text);
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

    private void addCloseButtonItem(LinearLayout layout, View.OnClickListener listener) {
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        View buttonView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_close, null);
        layout.addView(buttonView);
        buttonView.setLayoutParams(lparams);

        ImageButton button = (ImageButton)buttonView.findViewById(R.id.closeButton);
        button.setOnClickListener(listener);
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

    private void addSubHeaderItem(LinearLayout layout, String title, float textSize) {
        int margin = (int) getResources().getDimension(R.dimen.requirements_card_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_header, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        TextView titleView = (TextView)metadataView.findViewById(R.id.headingTitle);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        titleView.setText(title);
    }

    private void addDescriptionItem(LinearLayout layout, String description) {
        int margin = (int) getResources().getDimension(R.dimen.requirements_card_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_description, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        ((TextView)metadataView.findViewById(R.id.descriptionLabel)).setText(description);
    }

    private void addCourseListItem(final LinearLayout layout, final List<RequirementsListStatement> requirements) {
        View listView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_list, null);
        layout.addView(listView);
        final int rowIndex = layout.getChildCount() - 1;

        final LinearLayout listLayout = listView.findViewById(R.id.courseListLayout);
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
                            addCourseCell(layout, rowIndex, listLayout, courses.get(i), requirements.get(i));
                        }
                    }
                });
            }
        });
    }

    private void addCourseCell(final LinearLayout parentLayout, final int rowIndex, LinearLayout layout, final Course course, final RequirementsListStatement statement) {
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
        ((GradientDrawable)courseThumbnail.getBackground()).setColor(ColorManager.colorForCourse(course));
        ((TextView) courseThumbnail.findViewById(R.id.subjectIDLabel)).setText(course.getSubjectID());
        ((TextView) courseThumbnail.findViewById(R.id.subjectTitleLabel)).setText(course.subjectTitle);

        courseThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickCourseCell(parentLayout, rowIndex, courseThumbnail, course, statement);
            }
        });
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
                } else if (req.requirement != null) {
                    // Search
                    String reqString = req.requirement.replaceAll("GIR:", "");
                    if (delegate != null) {
                        delegate.courseNavigatorWantsSearchCourses(RequirementsListFragment.this, reqString);
                    }
                } else {
                    // Sub-requirements page
                    addCard(layout, presentationItemsForRequirement(req, 1, false), true, rowIndex + 1);
                }
            }
        });
        //addCard(layout, Arrays.asList(new PresentationItem(CellType.COURSE_LIST, req, null)), true);
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

    }
}
