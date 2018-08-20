package com.base12innovations.android.fireroad;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.base12innovations.android.fireroad.models.ColorManager;
import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.CourseManager;
import com.base12innovations.android.fireroad.models.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.RoadDocument;
import com.base12innovations.android.fireroad.models.ScheduleDocument;
import com.base12innovations.android.fireroad.models.User;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.base12innovations.android.fireroad.CourseNavigatorDelegate.ADD_TO_SCHEDULE;

public class CourseDetailsFragment extends Fragment implements BottomSheetNavFragment, AddCourseDialog.AddCourseDialogDelegate {

    public static String SUBJECT_ID_EXTRA = "CourseDetails_SubjectID";
    public Course course;
    private AddCourseDialog addCourseDialog;
    private View mContentView;
    private FloatingActionButton fab;
    public boolean canGoBack = false;

    public CourseDetailsFragment() {

    }

    public WeakReference<Delegate> delegate;

    public static CourseDetailsFragment newInstance(Course course) {
        CourseDetailsFragment fragment = new CourseDetailsFragment();
        fragment.course = course;
        Bundle args = new Bundle();
        args.putString(SUBJECT_ID_EXTRA, course.getSubjectID());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_course_details, container, false);

        mContentView = layout.findViewById(R.id.content);

        if (course != null) {
            setupContentView(mContentView);
            setupToolbar(layout);
        } else {
            final String subjectID = getArguments().getString(SUBJECT_ID_EXTRA);

            TaskDispatcher.perform(new TaskDispatcher.Task<Course>() {
                @Override
                public Course perform() {
                    if (subjectID != null)
                        return CourseManager.sharedInstance().getSubjectByID(subjectID);
                    return null;
                }
            }, new TaskDispatcher.CompletionBlock<Course>() {
                @Override
                public void completed(Course arg) {
                    course = arg;
                    if (course != null) {
                        setupContentView(mContentView);
                        setupToolbar(layout);
                    }
                }
            });
        }

        fab = (FloatingActionButton) layout.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCourse();
            }
        });

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        delegate = null;
    }

    public void scaleFAB(float newValue, boolean animated) {
        if (fab == null) {
            return;
        }
        if (animated) {
            ScaleAnimation scaler = new ScaleAnimation(fab.getScaleX(), newValue, fab.getScaleY(), newValue);
            scaler.setDuration(500);
            scaler.setFillAfter(true);
            fab.startAnimation(scaler);
        } else {
            fab.setScaleX(newValue);
            fab.setScaleY(newValue);
        }
    }

    public void scaleFAB(float newValue) {
        scaleFAB(newValue, false);
    }

    private void setupToolbar(View layout) {
        Toolbar toolbar = (Toolbar) layout.findViewById(R.id.toolbar);
        toolbar.setClickable(true);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate.get() != null) {
                    delegate.get().navFragmentClickedToolbar(CourseDetailsFragment.this);
                }
            }
        });
        if (canGoBack) {
            toolbar.setNavigationIcon(R.drawable.back_icon);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (delegate.get() != null) {
                        delegate.get().navFragmentWantsBack(CourseDetailsFragment.this);
                    }
                }
            });
        }
        toolbar.setTitle(course.getSubjectID());
        int barColor = ColorManager.colorForCourse(course, 0xFF);
        toolbar.setBackgroundColor(barColor);
    }

    private void setupContentView(View contentView) {
        TextView subjectTitleView = contentView.findViewById(R.id.detailsTitleView);
        subjectTitleView.setText(course.subjectTitle);
        TextView subjectDescriptionView = contentView.findViewById(R.id.detailsDescriptionView);
        subjectDescriptionView.setText(course.subjectDescription);

        LinearLayout layout = contentView.findViewById(R.id.courseDetailsLinearLayout);

        addUnitsItem(layout);
        addRequirementsItem(layout);
        addOfferedItem(layout);

        List<String> instructors = course.getInstructorsList();
        if (instructors.size() > 0) {
            addMetadataItem(layout, "Instructor" + (instructors.size() != 1 ? "s" : ""), TextUtils.join(", ", instructors));
        }

        if (course.enrollmentNumber > 0) {
            addMetadataItem(layout, "Average Enrollment", Integer.toString(course.enrollmentNumber));
        }

        addHeaderItem(layout, "Ratings");
        if (course.rating != 0.0) {
            addMetadataItem(layout, "Average Rating", Double.toString(course.rating) + " out of 7");
        }
        if (course.inClassHours != 0.0 || course.outOfClassHours != 0.0) {
            addMetadataItem(layout, "Hours", String.format(Locale.US, "%.2g in class\n%.2g out of class", course.inClassHours, course.outOfClassHours));
        }
        addMetadataItem(layout, "My Rating", "Rating widget should go here");

        List<String> subjectList = course.getEquivalentSubjectsList();
        if (subjectList.size() > 0) {
            addHeaderItem(layout, "Equivalent Subjects");
            addCourseListItem(layout, subjectList);
        }

        subjectList = course.getJointSubjectsList();
        if (subjectList.size() > 0) {
            addHeaderItem(layout, "Joint Subjects");
            addCourseListItem(layout, subjectList);
        }

        subjectList = course.getMeetsWithSubjectsList();
        if (subjectList.size() > 0) {
            addHeaderItem(layout, "Meets With Subjects");
            addCourseListItem(layout, subjectList);
        }

        List<List<String>> prereqs = course.getPrerequisitesList();
        if (prereqs != null && prereqs.size() > 0) {
            addHeaderItem(layout, "Prerequisites");
            if (course.getEitherPrereqOrCoreq()) {
                addDescriptionItem(layout, "Fulfill either the prerequisites or the corequisites.\n\nPrereqs: ");
            }
            addNestedCourseListItem(layout, prereqs);
        }
        List<List<String>> coreqs = course.getCorequisitesList();
        if (coreqs != null && coreqs.size() > 0) {
            addHeaderItem(layout, "Corequisites");
            addNestedCourseListItem(layout, coreqs);
        }
    }

    // Adding information types

    private void addUnitsItem(LinearLayout layout) {
        String unitsString;
        if (course.variableUnits) {
            unitsString = "arranged";
        } else {
            unitsString = String.format(Locale.US, "%d total (%d-%d-%d)", course.totalUnits, course.lectureUnits, course.labUnits, course.preparationUnits);
        }
        if (course.hasFinal) {
            unitsString += "\nHas final";
        }
        if (course.pdfOption) {
            unitsString += "\n[P/D/F]";
        }
        addMetadataItem(layout, "Units", unitsString);
    }

    private void addOfferedItem(LinearLayout layout) {
        List<String> offeredItems = new ArrayList<>();
        if (course.isOfferedFall) {
            offeredItems.add("Fall");
        }
        if (course.isOfferedIAP) {
            offeredItems.add("IAP");
        }
        if (course.isOfferedSpring) {
            offeredItems.add("Spring");
        }
        if (course.isOfferedSummer) {
            offeredItems.add("Summer");
        }
        String offeredString = TextUtils.join(", ", offeredItems);
        if (offeredString.length() == 0) {
            offeredString = "Information unavailable";
        }

        if (course.getQuarterOffered() == Course.QuarterOffered.BeginningOnly) {
            offeredString += "\n1st quarter";
            if (course.getQuarterBoundaryDate() != null) {
                offeredString += " - ends " + course.getQuarterBoundaryDate().substring(0, 1).toUpperCase() + course.getQuarterBoundaryDate().substring(1);
            }
        } else if (course.getQuarterOffered() == Course.QuarterOffered.EndOnly) {
            offeredString += "\n2nd quarter";
            if (course.getQuarterBoundaryDate() != null) {
                offeredString += " - starts " + course.getQuarterBoundaryDate().substring(0, 1).toUpperCase() + course.getQuarterBoundaryDate().substring(1);
            }
        }
        //offeredString = offeredString.substring(0, 1).toUpperCase() + offeredString.substring(1);
        addMetadataItem(layout, "Offered", offeredString);
    }

    private void addRequirementsItem(LinearLayout layout) {
        List<String> reqs = new ArrayList<>();
        Course.GIRAttribute gir = course.getGIRAttribute();
        if (gir != null) {
            reqs.add(gir.toString());
        }
        Course.CommunicationAttribute comm = course.getCommunicationRequirement();
        if (comm != null) {
            reqs.add(comm.toString());
        }
        Course.HASSAttribute hass = course.getHASSAttribute();
        if (hass!= null) {
            reqs.add(hass.toString());
        }

        if (reqs.size() > 0) {
            addMetadataItem(layout, "Fulfills", TextUtils.join(", ", reqs));
        }
    }

    private void addNestedCourseListItem(LinearLayout layout, List<List<String>> courses) {
        boolean containsOrClause = false;
        int totalCount = 0;
        for (List<String> group : courses) {
            totalCount += group.size();
            if (group.size() > 1) {
                containsOrClause = true;
            }
        }
        List<List<String>> newCourses = courses;

        if (totalCount > 1) {
            String command;
            if (!containsOrClause) {
                command = "Fulfill all of the following:";
                newCourses = new ArrayList<>();
                newCourses.add(new ArrayList<String>());
                for (List<String> group : courses) {
                    newCourses.get(0).addAll(group);
                }
            } else if (courses.size() == 1)
                command = "Fulfill any of the following:";
            else
                command = "Fulfill one from each row:";
            addDescriptionItem(layout, command);
        }

        for (List<String> group : newCourses) {
            addCourseListItem(layout, group);
        }
    }

    // Layout

    private void addMetadataItem(LinearLayout layout, String title, String value) {
        int margin = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_details_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_metadata, null);
        layout.addView(metadataView);

        ((TextView)metadataView.findViewById(R.id.metadataTitle)).setText(title);
        ((TextView)metadataView.findViewById(R.id.metadataValue)).setText(value);
    }

    private void addHeaderItem(LinearLayout layout, String title) {
        int margin = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_details_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, margin, margin, 0);
        View metadataView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_header, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        ((TextView)metadataView.findViewById(R.id.headingTitle)).setText(title);
    }

    private void addDescriptionItem(LinearLayout layout, String text) {
        int margin = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_details_padding);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(margin, 0, margin, 0);
        View metadataView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_description, null);
        layout.addView(metadataView);
        metadataView.setLayoutParams(lparams);

        ((TextView)metadataView.findViewById(R.id.descriptionLabel)).setText(text);
    }

    private void addCourseListItem(LinearLayout layout, final List<String> subjectIDs) {
        View listView = LayoutInflater.from(getContext()).inflate(R.layout.cell_course_details_list, null);
        layout.addView(listView);

        final LinearLayout listLayout = listView.findViewById(R.id.courseListLayout);
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                final List<Course> courses = new ArrayList<>();
                final Set<Course> realCourses = new HashSet<>();
                for (String subjectID : subjectIDs) {
                    Course newCourse = CourseManager.sharedInstance().getSubjectByID(subjectID);
                    if (newCourse != null) {
                        realCourses.add(newCourse);
                        courses.add(newCourse);
                    } else if (subjectID.toLowerCase().contains("permission of instructor")) {
                        Course poi = new Course();
                        poi.setSubjectID("--");
                        poi.subjectTitle = "Permission of Instructor";
                        courses.add(poi);
                    } else if (Course.GIRAttribute.fromRaw(subjectID) != null) {
                        Course.GIRAttribute gir = Course.GIRAttribute.fromRaw(subjectID);
                        newCourse = new Course();
                        newCourse.setSubjectID("GIR");
                        newCourse.subjectTitle = gir.toString().replaceAll("GIR", "").trim();
                        newCourse.girAttribute = gir.rawValue;
                        courses.add(newCourse);
                    } else if (subjectID.length() > 0) {
                        newCourse = new Course();
                        newCourse.setSubjectID("--");
                        newCourse.subjectTitle = subjectID;
                        courses.add(newCourse);
                    }
                }

                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        for (final Course course : courses) {
                            int width = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_cell_default_width);
                            int height = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_cell_height);
                            int margin = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_cell_spacing);
                            int elevation = (int) CourseDetailsFragment.this.getResources().getDimension(R.dimen.course_cell_elevation);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
                            params.setMargins(margin, margin, margin, margin);
                            View courseThumbnail = LayoutInflater.from(CourseDetailsFragment.this.getContext()).inflate(R.layout.linearlayout_course, null);
                            listLayout.addView(courseThumbnail);
                            courseThumbnail.setLayoutParams(params);
                            courseThumbnail.setElevation(elevation);
                            ((GradientDrawable)courseThumbnail.getBackground()).setColor(ColorManager.colorForCourse(course));
                            ((TextView) courseThumbnail.findViewById(R.id.subjectIDLabel)).setText(course.getSubjectID());
                            ((TextView) courseThumbnail.findViewById(R.id.subjectTitleLabel)).setText(course.subjectTitle);

                            if (realCourses.contains(course) || course.getGIRAttribute() != null) {
                                courseThumbnail.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (realCourses.contains(course))
                                            showCourseDetails(course);
                                        else if (delegate.get() != null) {
                                            EnumSet<CourseSearchEngine.Filter> filters = EnumSet.copyOf(CourseSearchEngine.Filter.noFilter);
                                            CourseSearchEngine.Filter.filterGIR(filters, CourseSearchEngine.Filter.GIR);
                                            CourseSearchEngine.Filter.filterSearchField(filters, CourseSearchEngine.Filter.SEARCH_REQUIREMENTS);
                                            delegate.get().courseNavigatorWantsSearchCourses(CourseDetailsFragment.this, course.subjectTitle, filters);
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    private void addCourse() {
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
                Snackbar.make(mContentView, "Added " + course.getSubjectID() + " to schedule", Snackbar.LENGTH_LONG).show();
            }
        } else {
            RoadDocument doc = User.currentUser().getCurrentDocument();
            if (doc != null) {
                boolean worked = doc.addCourse(course, semester);
                if (worked) {
                    if (delegate.get() != null)
                        delegate.get().courseNavigatorAddedCourse(this, course, semester);
                    Snackbar.make(mContentView, "Added " + course.getSubjectID(), Snackbar.LENGTH_LONG).show();
                }
            }
        }
        addCourseDialog.dismiss();
        addCourseDialog = null;
    }

    public void showCourseDetails(Course course) {
        if (delegate.get() != null) {
            delegate.get().courseNavigatorWantsCourseDetails(this, course);
        }
    }
}
