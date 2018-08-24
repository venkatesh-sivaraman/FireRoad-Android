package com.base12innovations.android.fireroad;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.base12innovations.android.fireroad.dialog.ConstrainDialogFragment;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.doc.Document;
import com.base12innovations.android.fireroad.models.doc.DocumentManager;
import com.base12innovations.android.fireroad.models.doc.NetworkManager;
import com.base12innovations.android.fireroad.models.schedule.ScheduleConfiguration;
import com.base12innovations.android.fireroad.models.doc.ScheduleDocument;
import com.base12innovations.android.fireroad.models.schedule.ScheduleGenerator;
import com.base12innovations.android.fireroad.models.schedule.ScheduleSlots;
import com.base12innovations.android.fireroad.models.schedule.ScheduleUnit;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScheduleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScheduleFragment extends Fragment implements PopupMenu.OnMenuItemClickListener {


    private static String PREFERENCES = "com.base12innovations.android.fireroad.ScheduleFragmentPreferences";
    private View mView;
    private View configView, noResultsView, noCoursesView;
    private LinearLayout columnView;
    private ProgressBar loadingIndicator;
    private AppCompatImageButton nextButton, previousButton;
    private TextView configurationCountLabel;

    private ScheduleDocument document;
    private List<ScheduleConfiguration> scheduleConfigurations;
    private ScheduleConfiguration currentConfiguration;

    private CourseNavigatorDelegate mListener;

    private boolean needsDisplay = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CourseNavigatorDelegate) {
            mListener = (CourseNavigatorDelegate) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        if (needsDisplay) {
            loadScheduleDisplay(false);
            needsDisplay = false;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public ScheduleFragment() {
        // Required empty public constructor
    }

    public static ScheduleFragment newInstance() {
        ScheduleFragment fragment = new ScheduleFragment();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        mView = view;
        configView = view.findViewById(R.id.scheduleConfiguration);
        columnView = configView.findViewById(R.id.scheduleColumnView);
        noResultsView = mView.findViewById(R.id.noResultsView);
        noCoursesView = mView.findViewById(R.id.noCoursesView);

        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(View.VISIBLE);

        User.currentUser().addScheduleChangedListener(new User.ScheduleChangedListener() {
            @Override
            public void scheduleChanged(ScheduleDocument newDocument) {
                // Update schedule view
                document = newDocument;
                loadSchedules(true);
            }
        });

        CourseManager.sharedInstance().waitForLoad(new Callable<Void>() {
            @Override
            public Void call() {
                finishLoadingView();
                return null;
            }
        });

        nextButton = mView.findViewById(R.id.nextButton);
        previousButton = mView.findViewById(R.id.previousButton);
        configurationCountLabel = (TextView)mView.findViewById(R.id.configurationCountLabel);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDisplayedScheduleIndex(document.getDisplayedScheduleIndex() + 1);
            }
        });
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDisplayedScheduleIndex(document.getDisplayedScheduleIndex() - 1);
            }
        });
        return view;
    }

    public static void createInitialDocument(Context context, final TaskDispatcher.TaskNoReturn completion) {
        Log.d("ScheduleFragment", "Creating initial document");
        final ScheduleDocument document = new ScheduleDocument(new File(context.getFilesDir(), Document.INITIAL_DOCUMENT_TITLE + ".sched"));
        TaskDispatcher.perform(new TaskDispatcher.Task<Void>() {
            @Override
            public Void perform() {
                if (document.file.exists()) {
                    document.read();
                } else {
                    document.save();
                }
                return null;
            }
        }, new TaskDispatcher.CompletionBlock<Void>() {
            @Override
            public void completed(Void arg) {
                User.currentUser().setCurrentSchedule(document);
                if (completion != null)
                    completion.perform();
            }
        });
    }
    private void finishLoadingView() {
        setupScheduleDisplay();

        Activity currentActivity = getActivity();
        if (currentActivity == null)
            return;

        if (User.currentUser().getCurrentSchedule() == null) {
            ScheduleFragment.createInitialDocument(currentActivity, new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    ScheduleFragment.this.document = document;
                    loadSchedules(true);
                }
            });
        } else {
            // Update schedule view
            document = User.currentUser().getCurrentSchedule();
            loadSchedules(true);
        }
    }

    private void setupScheduleDisplay() {
        Map<View, String> dayMapping = new HashMap<>();
        dayMapping.put(columnView.findViewById(R.id.columnLayoutMonday), "MON");
        dayMapping.put(columnView.findViewById(R.id.columnLayoutTuesday), "TUE");
        dayMapping.put(columnView.findViewById(R.id.columnLayoutWednesday), "WED");
        dayMapping.put(columnView.findViewById(R.id.columnLayoutThursday), "THU");
        dayMapping.put(columnView.findViewById(R.id.columnLayoutFriday), "FRI");

        for (View column : dayMapping.keySet()) {
            ((TextView)column.findViewById(R.id.dayLabel)).setText(dayMapping.get(column));
        }
    }

    private boolean isLoading = false;

    public void loadSchedules(boolean hideView) {
        if (configView == null || loadingIndicator == null)
            return;
        if (hideView) {
            configView.setVisibility(View.INVISIBLE);
        }
        loadingIndicator.setVisibility(View.VISIBLE);
        configView.setClickable(false);
        if (isLoading)
            return;
        isLoading = true;

        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                if (document == null) {
                    isLoading = false;
                    return;
                }

                // Check that all courses have schedules available
                Iterator<Course> it = document.getCourses().iterator();
                final List<String> incompatibleCourses = new ArrayList<>();
                while (it.hasNext()) {
                    Course course = it.next();
                    if (course.getSchedule() == null) {
                        it.remove();
                        incompatibleCourses.add(course.getSubjectID());
                    }
                }
                if (incompatibleCourses.size() > 0) {
                    TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                        @Override
                        public void perform() {
                            alertIncompatibleCourses(incompatibleCourses);
                        }
                    });
                }

                Log.d("ScheduleFragment", "Courses: " + document.getCourses().toString());
                ScheduleGenerator gen = new ScheduleGenerator();
                scheduleConfigurations = gen.generateSchedules(document.getCourses(), document.allowedSections);
                if (scheduleConfigurations.size() == 0) {
                    // Show no-results label
                    TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                        @Override
                        public void perform() {
                            isLoading = false;
                            configView.setClickable(false);
                            loadingIndicator.setVisibility(View.INVISIBLE);
                            formatViewForNoResults();
                        }
                    });
                } else {
                    isLoading = false;
                    inferScheduleIndex();

                    TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                        @Override
                        public void perform() {
                            setDisplayedScheduleIndex(document.getDisplayedScheduleIndex());
                            configView.setClickable(false);
                            configView.setVisibility(View.VISIBLE);
                            loadingIndicator.setVisibility(View.INVISIBLE);
                            loadScheduleDisplay();
                        }
                    });
                }
            }
        });
    }

    private void alertIncompatibleCourses(List<String> alertSubjectIDs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage("The following subjects do not have schedule information available: " + TextUtils.join(", ", alertSubjectIDs) + ".")
                .setTitle("Subjects Unavailable");
        builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void formatViewForNoResults() {
        if (noResultsView != null)
            noResultsView.setVisibility(View.VISIBLE);
        updateConfigurationButtons();
    }

    private void setDisplayedScheduleIndex(int newValue) {
        if (newValue < 0 || newValue >= scheduleConfigurations.size())
            return;
        document.selectedSchedule = scheduleConfigurations.get(newValue);
        document.setDisplayedScheduleIndex(newValue);
        loadScheduleDisplay(true);
    }

    private void updateConfigurationButtons() {
        if (document == null) {
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);
            configurationCountLabel.setText("--");
        }
        nextButton.setEnabled(document.getDisplayedScheduleIndex() < scheduleConfigurations.size() - 1);
        previousButton.setEnabled(document.getDisplayedScheduleIndex() > 0);
        configurationCountLabel.setText(String.format(Locale.US, "%d of %d", document.getDisplayedScheduleIndex() + 1, scheduleConfigurations.size()));
    }

    public void scheduleAddedCourse(Course course) {
        loadSchedules(false);
    }

    public void addAllCourses(final List<Course> courses, final String fileName) {
        if (User.currentUser().getCurrentSchedule() != null) {
            String currentName = User.currentUser().getCurrentSchedule().getFileName().replaceAll(fileName, "").trim();
            try {
                // If it's a version of the same semester, replace the current document
                if (currentName.length() == 0 || Integer.parseInt(currentName) != 0) {
                    User.currentUser().getCurrentSchedule().setCourses(courses);
                    loadSchedules(true);
                }
            } catch (NumberFormatException e) {
                    // Create a new document
                    final DocumentManager manager = NetworkManager.sharedInstance().getScheduleManager();
                    TaskDispatcher.perform(new TaskDispatcher.Task<ScheduleDocument>() {
                        @Override
                        public ScheduleDocument perform() {
                            try {
                                ScheduleDocument newDoc = (ScheduleDocument) manager.getNewDocument(manager.noConflictName(fileName));
                                newDoc.setCourses(courses);
                                return newDoc;
                            } catch (IOException e2) {
                                e2.printStackTrace();
                                return null;
                            }
                        }
                    }, new TaskDispatcher.CompletionBlock<ScheduleDocument>() {
                        @Override
                        public void completed(ScheduleDocument newDoc) {
                            if (newDoc != null) {
                                manager.setAsCurrent(newDoc);
                                showNewFileHelpText();
                            }
                        }
                    });
            }
        }
    }

    private static String HAS_SHOWN_NEW_FILE_HELP = "hasShownNewFileHelp";

    private boolean hasShownNewFileHelpText() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getBoolean(HAS_SHOWN_NEW_FILE_HELP, false);
    }

    void showNewFileHelpText() {
        if (hasShownNewFileHelpText()) return;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
        builder.setTitle("New Schedule Created");
        builder.setMessage(Html.fromHtml("To access previous schedules, tap the <b>&#8942;</b> button, then <b>Open other schedule</b>."));

        builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                SharedPreferences prefs = getContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(HAS_SHOWN_NEW_FILE_HELP, true).apply();
            }
        });
        builder.show();
    }

    // Schedule display

    private void inferScheduleIndex() {

        Log.d("ScheduleFragment", "Inferring");
        if (document == null)
            return;
        if (document.preloadSections != null) {
            Map<Course, Map<String, Integer>> preload = document.preloadSections;
            Log.d("ScheduleFragment", "Preload " + preload.toString() + ", configs " + scheduleConfigurations.toString());
            int index = 0;
            for (int i = 0; i < scheduleConfigurations.size(); i++) {
                boolean allMatch = true;
                for (Course course : preload.keySet()) {
                    if (course.getSchedule() == null) continue;
                    for (String section : preload.get(course).keySet()) {
                        boolean match = false;
                        for (ScheduleUnit unit : scheduleConfigurations.get(i).scheduleItems) {
                            if (unit.course.equals(course) &&
                                    unit.sectionType.equals(section) &&
                                    unit.scheduleItems.equals(course.getSchedule().get(section).get(preload.get(course).get(section)))) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            allMatch = false;
                            break;
                        }
                    }
                }
                if (allMatch) {
                    Log.d("ScheduleFragment", "Setting to index " + Integer.toString(i));
                    index = i;
                    break;
                }
            }

            document.setDisplayedScheduleIndex(index);
            document.selectedSchedule = scheduleConfigurations.get(index);
            document.preloadSections = null;
        } else if (scheduleConfigurations.size() > 0) {
            document.setDisplayedScheduleIndex(0);
            document.selectedSchedule = scheduleConfigurations.get(0);
            document.preloadSections = null;
        }

    }

    private Map<ScheduleUnit, List<View>> courseCells;

    private Map<Integer, Integer> borderMappings;

    private void loadBorderMappings(List<Course.ScheduleTime> times) {
        borderMappings = new HashMap<>();
        for (Course.ScheduleTime time : times) {
            if (time.minute != 0) continue;

            int borderId = 0;
            switch (time.hour24()) {
                case 8: borderId = R.id.horizontal8;
                    break;
                case 9: borderId = R.id.horizontal9;
                    break;
                case 10: borderId = R.id.horizontal10;
                    break;
                case 11: borderId = R.id.horizontal11;
                    break;
                case 12: borderId = R.id.horizontal12;
                    break;
                case 13: borderId = R.id.horizontal1;
                    break;
                case 14: borderId = R.id.horizontal2;
                    break;
                case 15: borderId = R.id.horizontal3;
                    break;
                case 16: borderId = R.id.horizontal4;
                    break;
                case 17: borderId = R.id.horizontal5;
                    break;
                case 18: borderId = R.id.horizontal6;
                    break;
                case 19: borderId = R.id.horizontal7;
                    break;
                case 20: borderId = R.id.horizontal8P;
                    break;
                case 21: borderId = R.id.horizontal9P;
                    break;
                case 22: borderId = R.id.horizontal10P;
                    break;
                default:
                    break;
            }

            borderMappings.put(time.hour24(), borderId);
        }
    }

    private View createCell(RelativeLayout parentView, final Course course, Course.ScheduleItem item, String sectionType) {
        View courseThumbnail = LayoutInflater.from(getContext()).inflate(R.layout.linearlayout_course, null);
        parentView.addView(courseThumbnail);

        //Log.d("ScheduleFragment", "Creating thumbnail for " + course.getSubjectID());
        int color = ColorManager.colorForCourse(course);

        ((GradientDrawable)courseThumbnail.getBackground()).setColor(color);
        TextView subjectIDLabel = courseThumbnail.findViewById(R.id.subjectIDLabel);
        TextView subjectTitleLabel = courseThumbnail.findViewById(R.id.subjectTitleLabel);
        subjectIDLabel.setText(course.getSubjectID());
        subjectIDLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f);
        subjectTitleLabel.setText(sectionType + " (" + item.location + ")");
        subjectTitleLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);

        /*courseThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCourseDetails(course);
            }
        });*/
        courseThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentlySelectedCourse = course;
                PopupMenu menu = new PopupMenu(getActivity(), view);
                MenuInflater mInflater = menu.getMenuInflater();
                mInflater.inflate(R.menu.menu_schedule_cell, menu.getMenu());
                menu.setOnMenuItemClickListener(ScheduleFragment.this);
                menu.show();
                currentPopupMenu = menu;
            }
        });
        return courseThumbnail;
    }

    private static int MINS_PER_HOUR = 60;

    private int cellHeight(Course.ScheduleTime startTime, Course.ScheduleTime duration, int rowHeight, int lineWidth) {
        int minute = startTime.minute;
        int minuteDelta = duration.hour * MINS_PER_HOUR + duration.minute;
        int height = 0;
        while (minuteDelta >= MINS_PER_HOUR - minute) {
            if (height != 0)
                height += lineWidth;
            minuteDelta -= MINS_PER_HOUR - minute;
            height += (int)((float)rowHeight * (float)(MINS_PER_HOUR - minute) / 60.0f);
            minute = 0;
        }
        if (minuteDelta > 0) {
            height += (int)((float)rowHeight * (float)minuteDelta / 60.0f);
        }

        return height;
    }

    private int cellMarginTop(Course.ScheduleTime startTime, int rowHeight) {
        return (int)((float)rowHeight * (float)startTime.minute / 60.0f);
    }

    private int getAnchorBorder(RelativeLayout dayLayout, Course.ScheduleTime startTime) {
        return borderMappings.get(startTime.hour24());
    }

    private void resetAndLoadScheduleDisplay() {
        if (noResultsView != null)
            noResultsView.setVisibility(View.INVISIBLE);
        updateNoCoursesView();
        updateConfigurationButtons();

        // Present the schedule
        int columnWidth = (int) getResources().getDimension(R.dimen.schedule_day_width);
        int rowHeight = (int) getResources().getDimension(R.dimen.schedule_hour_height);
        int lineWidth = (int) getResources().getDimension(R.dimen.schedule_line_width);
        if (courseCells != null) {
            for (ScheduleUnit unit : courseCells.keySet()) {
                for (View view : courseCells.get(unit)) {
                    ((ViewGroup)view.getParent()).removeView(view);
                }
            }
        }
        courseCells = new HashMap<>();
        List<Course.ScheduleTime> times = ScheduleSlots.slots;
        if (borderMappings == null)
            loadBorderMappings(times);

        int[] days = new int[] {Course.ScheduleDay.MON, Course.ScheduleDay.TUES, Course.ScheduleDay.WED, Course.ScheduleDay.THURS, Course.ScheduleDay.FRI };
        List<RelativeLayout> dayLayouts = new ArrayList<>();
        dayLayouts.add((RelativeLayout)columnView.findViewById(R.id.columnLayoutMonday));
        dayLayouts.add((RelativeLayout)columnView.findViewById(R.id.columnLayoutTuesday));
        dayLayouts.add((RelativeLayout)columnView.findViewById(R.id.columnLayoutWednesday));
        dayLayouts.add((RelativeLayout)columnView.findViewById(R.id.columnLayoutThursday));
        dayLayouts.add((RelativeLayout)columnView.findViewById(R.id.columnLayoutFriday));

        for (int i = 0; i < days.length; i++) {

            int day = days[i];
            List<ScheduleConfiguration.ChronologicalElement> sortedItems = currentConfiguration.chronologicalItemsForDay(day);

            List<List<ScheduleConfiguration.ChronologicalElement>> timeSlots = new ArrayList<>();
            for (Course.ScheduleTime time : times)
                timeSlots.add(new ArrayList<ScheduleConfiguration.ChronologicalElement>());

            List<List<Integer>> allTimeSlots = new ArrayList<>();
            for (Course.ScheduleTime time : times) allTimeSlots.add(new ArrayList<Integer>());

            for (int j = 0; j < sortedItems.size(); j++) {
                ScheduleConfiguration.ChronologicalElement element = sortedItems.get(j);
                int startIndex = ScheduleSlots.slotIndex(element.item.startTime);
                int endIndex = ScheduleSlots.slotIndex(element.item.endTime);
                if (startIndex < 0 || startIndex >= timeSlots.size() ||
                        endIndex < 0 || endIndex >= timeSlots.size()) continue;

                timeSlots.get(startIndex).add(element);
                for (int idx = startIndex; idx < endIndex; idx++)
                    allTimeSlots.get(idx).add(j);
            }

            // Cluster the time slots so we know how wide to make each cell
            List<Integer> slotOccupancyCounts = new ArrayList<>(); // Number of occupancies in each cluster
            List<Integer> slotSizes = new ArrayList<>(); // Number of time slots occupied by each cluster
            Map<Integer, Integer> slotClusterMapping = new HashMap<>(); // Mapping of slot index to cluster in slotOccupiedCounts
            Set<Integer> currentElements = new HashSet<>(); // Indices in sortedItems

            for (int j = 0; j < allTimeSlots.size(); j++) {
                List<Integer> slot = allTimeSlots.get(j);

                // Remove elements that end before this time
                Iterator<Integer> iterator = currentElements.iterator();
                while (iterator.hasNext()) {
                    Integer element = iterator.next();
                    if (sortedItems.get(element).item.endTime.compareTo(times.get(j)) <= 0) {
                        iterator.remove();
                    }
                }

                if (currentElements.size() == 0 || slotOccupancyCounts.size() == 0) {
                    slotOccupancyCounts.add(currentElements.size());
                    slotSizes.add(0);
                }
                if (slot.size() == 0 && currentElements.size() != 0) {
                    Log.e("ScheduleFragment", "Inconsistency between allTimeSlots and current element list");
                    return;
                }

                currentElements.addAll(new HashSet<>(slot));
                if (slotOccupancyCounts.size() > 0) {
                    slotOccupancyCounts.set(slotOccupancyCounts.size() - 1, Math.max(currentElements.size(), slotOccupancyCounts.get(slotOccupancyCounts.size() - 1)));
                    slotSizes.set(slotSizes.size() - 1, slotSizes.get(slotSizes.size() - 1) + 1);
                }
                slotClusterMapping.put(j, slotOccupancyCounts.size() - 1);
            }

            // Layout the cells

            // Mapping of column numbers to the indexes at which those columns will end
            Map<Integer, Integer> occupiedColumns = new HashMap<>();
            for (int j = 0; j < timeSlots.size(); j++) {
                List<ScheduleConfiguration.ChronologicalElement> slot = timeSlots.get(j);
                if (!slotClusterMapping.containsKey(j)) {
                    Log.e("ScheduleFragment", "Couldn't find current cluster");
                    continue;
                }
                int cluster = slotClusterMapping.get(j);

                // Remove elements that end before this time
                Map<Integer, Integer> newOccupiedCols = new HashMap<>();
                for (Integer colKey: occupiedColumns.keySet())
                    if (occupiedColumns.get(colKey) > j)
                        newOccupiedCols.put(colKey, occupiedColumns.get(colKey));
                occupiedColumns = newOccupiedCols;

                int occupancy = slotOccupancyCounts.get(cluster);
                int duration = slotSizes.get(cluster);
                float widthFraction = 1.0f / (float) occupancy;
                if (slot.size() == 0)
                    continue;

                for (ScheduleConfiguration.ChronologicalElement element : slot) {
                    Course.ScheduleTime classDuration = element.item.startTime.deltaTo(element.item.endTime);

                    View cell = createCell(dayLayouts.get(i), element.course, element.item, Course.ScheduleType.abbreviationFor(element.type).toLowerCase());

                    // Positioning
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            (int)Math.round((float)columnWidth * widthFraction),
                            cellHeight(element.item.startTime, classDuration, rowHeight, lineWidth));
                    int marginTop = cellMarginTop(element.item.startTime, rowHeight);
                    int marginLeft = 0;
                    int marginRight = 0;
                    params.addRule(RelativeLayout.BELOW, getAnchorBorder(dayLayouts.get(i), element.item.startTime));
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);

                    // Find the first unoccupied column and position its margins to situate it there
                    for (int subcolumn = 0; subcolumn < occupancy; subcolumn++) {
                        if (occupiedColumns.containsKey(subcolumn)) continue;

                        occupiedColumns.put(subcolumn, ScheduleSlots.slotIndex(element.item.endTime));
                        marginLeft = (int)((float)subcolumn * (float)columnWidth * widthFraction);
                        marginRight = (int)((float)(occupancy - subcolumn - 1) * (float)columnWidth * widthFraction);
                        break;
                    }
                    params.setMargins(marginLeft, marginTop, marginRight, 0);

                    cell.setLayoutParams(params);

                    if (!courseCells.containsKey(element.unit))
                        courseCells.put(element.unit, new ArrayList<View>());
                    courseCells.get(element.unit).add(cell);
                }

            }
        }
    }

    // Delta between schedules

    private void loadScheduleDisplay(boolean withDelta) {
        if (getContext() == null) {
            needsDisplay = true;
            return;
        }

        final ScheduleConfiguration newConfig = scheduleConfigurations.get(document.getDisplayedScheduleIndex());
        if (withDelta) {
            List<ScheduleUnit> removedUnits = (currentConfiguration != null) ? scheduleUnitsNotPresentInOtherSchedule(currentConfiguration, newConfig) : new ArrayList<ScheduleUnit>();
            final List<ScheduleUnit> addedUnits = (currentConfiguration != null) ? scheduleUnitsNotPresentInOtherSchedule(newConfig, currentConfiguration) : newConfig.scheduleItems;

            // Animate out removed units
            AnimatorSet animateOut = new AnimatorSet();
            List<Animator> animations = new ArrayList<>();
            for (ScheduleUnit unit : removedUnits) {
                if (!courseCells.containsKey(unit)) continue;
                for (View view : courseCells.get(unit)) {
                    animations.add(ObjectAnimator.ofFloat(view, "scaleX", 0.001f));
                    animations.add(ObjectAnimator.ofFloat(view, "scaleY", 0.001f));
                    animations.add(ObjectAnimator.ofFloat(view, "alpha", 0.001f));
                }
            }
            animateOut.playTogether(animations);
            animateOut.setDuration(300);
            animateOut.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) { }

                @Override
                public void onAnimationEnd(Animator animator) {
                    finishLoadingScheduleAnimation(newConfig, addedUnits);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    finishLoadingScheduleAnimation(newConfig, addedUnits);
                }

                @Override
                public void onAnimationRepeat(Animator animator) { }
            });
            animateOut.start();
        } else {
            resetAndLoadScheduleDisplay();
        }
    }

    private void finishLoadingScheduleAnimation(ScheduleConfiguration newConfig, List<ScheduleUnit> addedUnits) {
        // Quickly scale down added units, then animate them up
        currentConfiguration = newConfig;
        resetAndLoadScheduleDisplay();

        AnimatorSet animateIn = new AnimatorSet();
        List<Animator> animations = new ArrayList<>();
        for (ScheduleUnit unit : addedUnits) {
            if (!courseCells.containsKey(unit)) continue;
            for (View view : courseCells.get(unit)) {
                view.setScaleX(0.001f);
                view.setScaleY(0.001f);
                view.setAlpha(0.001f);
                animations.add(ObjectAnimator.ofFloat(view, "scaleX", 1.0f));
                animations.add(ObjectAnimator.ofFloat(view, "scaleY", 1.0f));
                animations.add(ObjectAnimator.ofFloat(view, "alpha", 1.0f));
            }
        }
        animateIn.playTogether(animations);
        animateIn.setDuration(300);
        animateIn.start();
    }

    private void loadScheduleDisplay() {
        loadScheduleDisplay(false);
    }

    private List<ScheduleUnit> scheduleUnitsNotPresentInOtherSchedule(ScheduleConfiguration oldConfig, ScheduleConfiguration newConfig) {
        List<ScheduleUnit> ret = new ArrayList<>();
        for (ScheduleUnit unit : oldConfig.scheduleItems) {
            if (!newConfig.scheduleItems.contains(unit)) {
                ret.add(unit);
            }
        }
        return ret;
    }

    private void updateNoCoursesView() {
        if (User.currentUser().getCurrentSchedule() != null &&
                User.currentUser().getCurrentSchedule().getAllCourses().size() > 0) {
            noCoursesView.setVisibility(View.GONE);
            configView.setVisibility(View.VISIBLE);
        } else {
            if (noCoursesView instanceof TextView)
                ((TextView)noCoursesView).setText(Html.fromHtml("<b>No subjects in your schedule yet!</b><br/>Add one by searching above or by browsing the Requirements page."));
            configView.setVisibility(View.INVISIBLE);
            noCoursesView.setVisibility(View.VISIBLE);
        }
    }


    // Handling clicks

    private Course currentlySelectedCourse;
    private PopupMenu currentPopupMenu;

    private void showCourseDetails(Course course) {
        mListener.courseNavigatorWantsCourseDetails(this, course);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        currentPopupMenu = null;
        final Course course = currentlySelectedCourse;
        currentlySelectedCourse = null;
        switch (menuItem.getItemId()) {
            case R.id.viewCourse:
                showCourseDetails(course);
                return true;
            case R.id.deleteCourse:
                document.removeCourse(course);
                loadSchedules(false);
                return true;
            case R.id.constrainCourse:
                // Show constrain sections view
                final ConstrainDialogFragment fragment = new ConstrainDialogFragment();
                fragment.course = course;
                if (document.allowedSections == null || !document.allowedSections.containsKey(course))
                    fragment.sections = new HashMap<>();
                else
                    fragment.sections = document.allowedSections.get(course);
                fragment.delegate = new ConstrainDialogFragment.Delegate() {
                    @Override
                    public void constrainDialogDismissed(ConstrainDialogFragment dialog) {
                        fragment.dismiss();
                    }

                    @Override
                    public void constrainDialogFinished(ConstrainDialogFragment dialog, Map<String, List<Integer>> sections) {
                        fragment.dismiss();
                        if (document.allowedSections == null)
                            document.allowedSections = new HashMap<>();
                        document.allowedSections.put(course, sections);
                        document.save();
                        loadSchedules(true);
                    }
                };
                FragmentActivity a = getActivity();
                if (a != null) {
                    fragment.show(a.getSupportFragmentManager(), "ConstrainDialogFragment");
                }
                break;
            default:
                break;
        }
        return false;
    }
}
