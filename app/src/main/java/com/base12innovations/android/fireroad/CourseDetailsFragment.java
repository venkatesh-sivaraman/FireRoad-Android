package com.base12innovations.android.fireroad;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ScrollingView;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.base12innovations.android.fireroad.dialog.AddCourseDialog;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.course.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.models.req.RequirementsListStatement;
import com.base12innovations.android.fireroad.utils.BottomSheetNavFragment;
import com.base12innovations.android.fireroad.utils.CourseLayoutBuilder;
import com.base12innovations.android.fireroad.utils.RequirementsListDisplay;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CourseDetailsFragment extends Fragment implements BottomSheetNavFragment, AddCourseDialog.AddCourseDialogDelegate {

    public static final String SUBJECT_ID_EXTRA = "CourseDetails_SubjectID";
    public Course course;
    private AddCourseDialog addCourseDialog;
    private View mContentView;
    private NestedScrollView scrollView;
    private FloatingActionButton fab;
    public boolean canGoBack = false;

    private int cacheScrollOffset = 0;

    public int getScrollOffset() {
        return cacheScrollOffset;
    }

    public void setScrollOffset(int offset) {
        cacheScrollOffset = offset;
    }

    private RequirementsListDisplay prereqDisplay, coreqDisplay;

    private CourseLayoutBuilder layoutBuilder;

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
        scrollView = (NestedScrollView)mContentView;
        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView nestedScrollView, int scrollX, int scrollY, int oldX, int oldY) {
                cacheScrollOffset = scrollY;
            }
        });

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
                addCourse(course);
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

        if (layoutBuilder == null) {
            layoutBuilder = new CourseLayoutBuilder(getContext());
            layoutBuilder.defaultMargin = (int)getResources().getDimension(R.dimen.course_details_padding);
            layoutBuilder.showHeadingTopMargin = false;
        }

        if (course.isHistorical) {
            String sourceSemester = course.sourceSemester;
            String warningText;
            if (sourceSemester != null && sourceSemester.length() > 0) {
                warningText = "This subject is no longer offered (last offered " + String.join(" ", sourceSemester.split("-")) + ").";
            } else {
                warningText = "This subject is no longer offered.";
            }
            layoutBuilder.addWarningItem(layout, warningText);
        }

        if (!course.isGeneric)
            addUnitsItem(layout);
        addRequirementsItem(layout);

        if (course.isGeneric)
            return;

        addOfferedItem(layout);

        List<String> instructors = course.getInstructorsList();
        if (instructors.size() > 0) {
            layoutBuilder.addMetadataItem(layout, "Instructor" + (instructors.size() != 1 ? "s" : ""), TextUtils.join(", ", instructors));
        }

        if (course.enrollmentNumber > 0) {
            layoutBuilder.addMetadataItem(layout, "Average Enrollment", Integer.toString(course.enrollmentNumber));
        }

        layoutBuilder.addHeaderItem(layout, "Ratings");
        if (course.rating != 0.0) {
            layoutBuilder.addMetadataItem(layout, "Average Rating", Double.toString(course.rating) + " out of 7");
        }
        if (course.inClassHours != 0.0 || course.outOfClassHours != 0.0) {
            layoutBuilder.addMetadataItem(layout, "Hours", String.format(Locale.US, "%.2g in class\n%.2g out of class", course.inClassHours, course.outOfClassHours));
        }
        layoutBuilder.addRatingItem(layout, "My Rating", course);
        layoutBuilder.addFavoritesItem(layout, course);

        List<String> subjectList = course.getEquivalentSubjectsList();
        if (subjectList.size() > 0) {
            layoutBuilder.addHeaderItem(layout, "Equivalent Subjects");
            addCourseListItem(layout, subjectList);
        }

        subjectList = course.getJointSubjectsList();
        if (subjectList.size() > 0) {
            layoutBuilder.addHeaderItem(layout, "Joint Subjects");
            addCourseListItem(layout, subjectList);
        }

        subjectList = course.getMeetsWithSubjectsList();
        if (subjectList.size() > 0) {
            layoutBuilder.addHeaderItem(layout, "Meets With Subjects");
            addCourseListItem(layout, subjectList);
        }

        RequirementsListStatement prereqs = course.getPrerequisites();
        int courseSemester = RoadDocument.semesterNames.length;
        if (User.currentUser().getCurrentDocument() != null)
            courseSemester = User.currentUser().getCurrentDocument().firstSemesterForCourse(course);

        if (prereqs != null) {
            layoutBuilder.addHeaderItem(layout, "Prerequisites");
            if (course.getEitherPrereqOrCoreq()) {
                layoutBuilder.addDescriptionItem(layout, "Fulfill either the prerequisites or the corequisites.\n\nPrereqs: ");
            }
            prereqDisplay = addRequirementsStatementItem(layout, prereqs, courseSemester - 1);
        }
        RequirementsListStatement coreqs = course.getCorequisites();
        if (coreqs != null) {
            layoutBuilder.addHeaderItem(layout, "Corequisites");
            coreqDisplay = addRequirementsStatementItem(layout, coreqs, courseSemester);
        }

        List<String> related = course.getRelatedSubjectsList();
        if (related != null && related.size() > 0) {
            layoutBuilder.addHeaderItem(layout, "Related");
            addCourseListItem(layout, related);
        }

        layoutBuilder.addButtonItem(layout, "View Subjects with " + course.getSubjectID() + " as Prerequisite", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (delegate.get() != null) {
                    EnumSet<CourseSearchEngine.Filter> filter = CourseSearchEngine.Filter.noFilter();
                    CourseSearchEngine.Filter.filterSearchField(filter, CourseSearchEngine.Filter.SEARCH_PREREQS);
                    CourseSearchEngine.Filter.exactMatch(filter);

                    String searchText = course.getSubjectID();
                    if (course.getGIRAttribute() != null && course.getGIRAttribute() != Course.GIRAttribute.REST)
                        searchText = course.getGIRAttribute().toString();
                    delegate.get().courseNavigatorWantsSearchCourses(CourseDetailsFragment.this, searchText, filter);
                }
            }
        });
        if (course.url != null && course.url.length() > 0) {
            layoutBuilder.addButtonItem(layout, "View in Subject Catalog", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(course.url));
                    startActivity(i);
                }
            });
        }
        layoutBuilder.addButtonItem(layout, "View Subject Evaluations", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://edu-apps.mit.edu/ose-rpt/subjectEvaluationSearch.htm?search=Search&subjectCode=" + course.getSubjectID();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

        String notes = CourseManager.sharedInstance().getNotes(course);
        layoutBuilder.addHeaderItem(layout, "Notes");
        layoutBuilder.addEditTextItem(layout, notes, new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    CourseManager.sharedInstance().setNotes(course, ((TextView)view).getText().toString());
                }
            }
        });

        if (cacheScrollOffset != 0 && scrollView != null) {
            // Scroll after delay
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.scrollTo(0, cacheScrollOffset);
                }
            });
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
        layoutBuilder.addMetadataItem(layout, "Units", unitsString);
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
        layoutBuilder.addMetadataItem(layout, "Offered", offeredString);
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
            layoutBuilder.addMetadataItem(layout, "Fulfills", TextUtils.join(", ", reqs));
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
            layoutBuilder.addDescriptionItem(layout, command);
        }

        for (List<String> group : newCourses) {
            addCourseListItem(layout, group);
        }
    }

    private RequirementsListDisplay addRequirementsStatementItem(LinearLayout layout, RequirementsListStatement requirement, int maxSemester) {
        RequirementsListDisplay display = new RequirementsListDisplay(requirement, getContext());
        display.singleCard = true;
        display.delegate = new RequirementsListDisplay.Delegate() {
            @Override public void addCourse(Course course) {
                CourseDetailsFragment.this.addCourse(course);
            }
            @Override public void showDetails(Course course) {
                showCourseDetails(course);
            }
            @Override public void searchCourses(String searchTerm, EnumSet<CourseSearchEngine.Filter> filters) {
                if (delegate.get() != null) {
                    delegate.get().courseNavigatorWantsSearchCourses(CourseDetailsFragment.this, searchTerm, filters);
                }
            }
            @Override public void showManualProgressSelector(RequirementsListStatement req) {
                // Do nothing - manual progress is disabled for prereqs/coreqs
            }
        };
        if (User.currentUser().getCurrentDocument() != null)
            requirement.computeRequirementStatus(User.currentUser().getCurrentDocument().coursesTakenBeforeCourse(course, maxSemester, true));
        display.layoutInView(layout);
        return display;
    }

    // Layout

    private void addCourseListItem(LinearLayout layout, final List<String> subjectIDs) {
        final LinearLayout listLayout = layoutBuilder.addCourseListItem(layout);
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
                            layoutBuilder.addCourseCell(listLayout, course,
                                    realCourses.contains(course) || course.getGIRAttribute() != null ?
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    if (realCourses.contains(course))
                                                        showCourseDetails(course);
                                                    else if (delegate.get() != null) {
                                                        EnumSet<CourseSearchEngine.Filter> filters = CourseSearchEngine.Filter.noFilter();
                                                        CourseSearchEngine.Filter.filterGIR(filters, CourseSearchEngine.Filter.GIR);
                                                        CourseSearchEngine.Filter.filterSearchField(filters, CourseSearchEngine.Filter.SEARCH_REQUIREMENTS);
                                                        delegate.get().courseNavigatorWantsSearchCourses(CourseDetailsFragment.this, course.subjectTitle, filters);
                                                    }
                                                }
                                            } : null,
                                    realCourses.contains(course) ? new View.OnLongClickListener() {
                                        @Override
                                        public boolean onLongClick(View view) {
                                            addCourse(course);
                                            return true;
                                        }
                                    } : null);
                        }
                    }
                });
            }
        });
    }

    private void addCourse(Course myCourse) {
        addCourseDialog = new AddCourseDialog();
        addCourseDialog.course = myCourse;
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

    public void showCourseDetails(Course course) {
        if (delegate.get() != null) {
            delegate.get().courseNavigatorWantsCourseDetails(this, course);
        }
    }
}
