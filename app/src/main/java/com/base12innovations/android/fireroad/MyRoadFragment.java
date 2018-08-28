package com.base12innovations.android.fireroad;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.base12innovations.android.fireroad.adapter.MyRoadCoursesAdapter;
import com.base12innovations.android.fireroad.dialog.CourseWarningsDialogFragment;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.doc.Document;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MyRoadFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MyRoadFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyRoadFragment extends Fragment implements PopupMenu.OnMenuItemClickListener {
    private static final String ARG_TEST_PARAM = "param1";

    private MyRoadCoursesAdapter gridAdapter;
    private ProgressBar loadingIndicator;

    private int currentlySelectedSemester;
    private int currentlySelectedPosition;
    private RecyclerView recyclerView;
    private Course currentlySelectedCourse;
    private PopupMenu currentPopupMenu;
    private View noCoursesView;
    private GridLayoutManager gridLayoutManager;

    private Delegate mListener;

    public MyRoadFragment() {
        // Required empty public constructor
    }

    public interface Delegate extends CourseNavigatorDelegate {
        void myRoadFragmentAddedCoursesToSchedule(List<Course> courses, String fileName);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param testParam Parameter 1.
     * @return A new instance of fragment MyRoadFragment.
     */
    public static MyRoadFragment newInstance(int testParam) {
        MyRoadFragment fragment = new MyRoadFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TEST_PARAM, testParam);
        fragment.setArguments(args);
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
        final View layout = inflater.inflate(R.layout.fragment_my_road, container, false);

        // Get view elements
        loadingIndicator = layout.findViewById(R.id.loadingIndicator);
        loadingIndicator.setVisibility(ProgressBar.VISIBLE);
        noCoursesView = layout.findViewById(R.id.noCoursesView);

        // Set up grid view
        int numColumns = 3;
        if (!getActivity().getResources().getBoolean(R.bool.portrait_only))
            numColumns = 6;
        gridAdapter = new MyRoadCoursesAdapter(null, numColumns);

        recyclerView = layout.findViewById(R.id.coursesRecyclerView);
        recyclerView.setHasFixedSize(false);
        gridLayoutManager = new GridLayoutManager(getActivity(), numColumns);
        recyclerView.setAdapter(gridAdapter);
        recyclerView.setLayoutManager(gridLayoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.course_cell_spacing);
        recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        /*gridAdapter.itemClickListener = new MyRoadCoursesAdapter.ClickListener() {
            @Override
            public void onClick(Course course, int position, View view) {
                showCourseDetails(course);
            }
        };*/
        gridAdapter.itemClickListener = new MyRoadCoursesAdapter.ClickListener() {
            @Override
            public void onClick(Course course, int position, View view) {
                currentlySelectedCourse = course;
                currentlySelectedSemester = gridAdapter.semesterForGridPosition(position);
                currentlySelectedPosition = position;
                final PopupMenu menu = new PopupMenu(getActivity(), view);
                MenuInflater mInflater = menu.getMenuInflater();
                mInflater.inflate(R.menu.menu_course_cell, menu.getMenu());

                final MenuItem owOn = menu.getMenu().findItem(R.id.overrideWarningsOn);
                final MenuItem owOff = menu.getMenu().findItem(R.id.overrideWarningsOff);

                List<RoadDocument.Warning> warnings = User.currentUser().getCurrentDocument().warningsForCourseCached(currentlySelectedCourse, currentlySelectedSemester);
                if (warnings == null) {
                    TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                        @Override
                        public void perform() {
                            final List<RoadDocument.Warning> newWarnings = User.currentUser().getCurrentDocument().warningsForCourse(currentlySelectedCourse, currentlySelectedSemester);
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

                menu.setOnMenuItemClickListener(MyRoadFragment.this);
                menu.show();
                currentPopupMenu = menu;
            }
        };
        gridAdapter.onHeaderClickListener = new MyRoadCoursesAdapter.HeaderClickListener() {
            @Override
            public void onHeaderButtonClick(int semester, View view) {
                final PopupMenu menu = new PopupMenu(getActivity(), view);
                MenuInflater mInflater = menu.getMenuInflater();
                mInflater.inflate(R.menu.menu_myroad_header, menu.getMenu());
                menu.setOnMenuItemClickListener(MyRoadFragment.this);
                menu.show();
                currentlySelectedSemester = semester;
                currentPopupMenu = menu;
            }
        };

        // Support drag and drop to move courses
        ItemTouchHelper.Callback _ithCallback = new ItemTouchHelper.Callback() {
            public boolean onMove(final RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, final RecyclerView.ViewHolder target) {
                // get the viewHolder's and target's positions in your adapter data, swap them
                if (currentPopupMenu != null) {
                    currentPopupMenu.dismiss();
                    currentPopupMenu = null;
                }

                return gridAdapter.moveCourse(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            }

            @Override
            public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
                // Empty implementation overrides super and disables weird scrolling behavior!!
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (!recyclerView.isComputingLayout())
                    updateRecyclerView();
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) { }

            //defines the enabled move directions in each state (idle, swiping, dragging).
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (gridAdapter.isSectionHeader(viewHolder.getAdapterPosition())) {
                    return 0;
                }
                return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                        ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }
        };
        // Create an `ItemTouchHelper` and attach it to the `RecyclerView`
        ItemTouchHelper ith = new ItemTouchHelper(_ithCallback);
        ith.attachToRecyclerView(recyclerView);

        gridLayoutManager.setSpanSizeLookup(gridAdapter.spanSizeLookup());

        User.currentUser().addRoadChangedListener(new User.RoadChangedListener() {
            @Override
            public void roadChanged(RoadDocument newDocument) {
                if (gridAdapter != null) {
                    gridAdapter.setDocument(newDocument);
                }
                updateRecyclerView();
            }
        });

        CourseManager.sharedInstance().waitForLoad(new Callable<Void>() {
            @Override
            public Void call() {
                finishLoadingView();
                return null;
            }
        });

        return layout;
    }

    public static void createInitialDocument(Context context, final TaskDispatcher.TaskNoReturn completion) {
                final RoadDocument document = RoadDocument.newDocument(new File(context.getFilesDir(), Document.INITIAL_DOCUMENT_TITLE + ".road"));
        TaskDispatcher.perform(new TaskDispatcher.Task<Void>() {
            @Override
            public Void perform() {
                if (document.file.exists()) {
                    document.read();
                } else {
                    document.addCourseOfStudy("girs");
                    document.save();
                }
                return null;
            }
        }, new TaskDispatcher.CompletionBlock<Void>() {
            @Override
            public void completed(Void arg) {
                User.currentUser().setCurrentDocument(document);
                if (completion != null)
                    completion.perform();
            }
        });
    }

    public void finishLoadingView() {

        // Set up model
        Activity currentActivity = getActivity();
        if (currentActivity == null) {
            return;
        }
        if (User.currentUser().getCurrentDocument() == null) {
            MyRoadFragment.createInitialDocument(getActivity(), new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    loadingIndicator.setVisibility(ProgressBar.GONE);
                    updateRecyclerView();
                }
            });
        } else {
            if (gridAdapter != null) {
                gridAdapter.setDocument(User.currentUser().getCurrentDocument());
            }
            loadingIndicator.setVisibility(ProgressBar.GONE);
            updateRecyclerView();

        }
    }

    public void reloadView() {
        if (gridAdapter != null) {
            gridAdapter.setDocument(User.currentUser().getCurrentDocument());
        }
        updateRecyclerView();
    }

    public void roadAddedCourse(Course course, int semester) {
        if (gridAdapter != null) {
            int position = gridAdapter.lastPositionForSemester(semester);
            gridAdapter.notifyItemInserted(position);
            gridAdapter.notifyItemChanged(gridAdapter.headerPositionForSemester(semester));
            if (recyclerView != null) {
                recyclerView.scrollToPosition(position);
            }
        }
        updateRecyclerView();
    }

    private void updateRecyclerView() {
        if (getContext() == null)
            return;

        if (User.currentUser().getCurrentDocument() != null &&
                User.currentUser().getCurrentDocument().getAllCourses().size() > 0) {
            noCoursesView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            if (noCoursesView instanceof TextView)
                ((TextView)noCoursesView).setText(Html.fromHtml("<b>No subjects in your road yet!</b><br/>Add one by searching above or by browsing the Requirements page."));
            recyclerView.setVisibility(View.INVISIBLE);
            noCoursesView.setVisibility(View.VISIBLE);
        }

        for (int childCount = recyclerView.getChildCount(), i = 0; i < childCount; ++i) {
            MyRoadCoursesAdapter.ViewHolder holder = (MyRoadCoursesAdapter.ViewHolder)recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder == null) continue;
            int pos = holder.getAdapterPosition();
            if (pos >= 0 && gridAdapter.isSectionHeader(pos))
                gridAdapter.notifyItemChanged(pos);
            else
                gridAdapter.updateWarningsView(holder);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Delegate) {
            mListener = (Delegate) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement CourseNavigatorDelegate");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void showCourseDetails(Course course) {
        /*Intent intent = new Intent(getActivity(), CourseDetailsFragment.class);
        intent.putExtra(CourseDetailsFragment.COURSE_EXTRA, currentlySelectedCourse);
        startActivity(intent);*/
        if (mListener != null) {
            mListener.courseNavigatorWantsCourseDetails(this, course);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        currentPopupMenu.dismiss();
        currentPopupMenu = null;
        switch (menuItem.getItemId()) {
            case R.id.viewCourse:
                showCourseDetails(currentlySelectedCourse);
                return true;
            case R.id.deleteCourse:
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        User.currentUser().getCurrentDocument().removeCourse(currentlySelectedCourse, currentlySelectedSemester);
                        gridAdapter.notifyItemRemoved(currentlySelectedPosition);
                        gridAdapter.notifyItemChanged(gridAdapter.headerPositionForSemester(currentlySelectedSemester));
                        updateRecyclerView();
                    }
                }, 400);
                return true;
            case R.id.courseWarnings:
                presentWarningAlert(currentlySelectedCourse, currentlySelectedSemester, currentlySelectedPosition);
                return true;
            case R.id.overrideWarningsOn:
                User.currentUser().getCurrentDocument().setOverrideWarningsForCourse(currentlySelectedCourse, true);
                gridAdapter.notifyItemChanged(currentlySelectedPosition);
                return true;
            case R.id.overrideWarningsOff:
                User.currentUser().getCurrentDocument().setOverrideWarningsForCourse(currentlySelectedCourse, false);
                gridAdapter.notifyItemChanged(currentlySelectedPosition);
                return true;
            case R.id.createSchedule:
                if (mListener != null)
                    mListener.myRoadFragmentAddedCoursesToSchedule(User.currentUser().getCurrentDocument().coursesForSemester(currentlySelectedSemester), RoadDocument.semesterNames[currentlySelectedSemester]);
                return true;
            case R.id.clearCourses:
                /*final Handler handler2 = new Handler();
                handler2.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        User.currentUser().getCurrentDocument().removeAllCoursesFromSemester(currentlySelectedSemester);
                        gridAdapter.notifyDataSetChanged();
                        updateRecyclerView();
                    }
                }, 400);*/
                int size = User.currentUser().getCurrentDocument().coursesForSemester(currentlySelectedSemester).size();
                User.currentUser().getCurrentDocument().moveCourse(currentlySelectedSemester, 0,
                        currentlySelectedSemester,  size - 1);
                gridAdapter.notifyItemMoved(gridAdapter.lastPositionForSemester(currentlySelectedSemester) - size,
                        gridAdapter.lastPositionForSemester(currentlySelectedSemester));
                return true;
            default:
                return false;
        }
    }

    private void presentWarningAlert(Course course, int semester, final int position) {
        CourseWarningsDialogFragment dialogFragment = new CourseWarningsDialogFragment();
        dialogFragment.course = course;
        dialogFragment.warnings = User.currentUser().getCurrentDocument().warningsForCourse(course, semester);
        dialogFragment.override = User.currentUser().getCurrentDocument().overrideWarningsForCourse(course);
        dialogFragment.delegate = new CourseWarningsDialogFragment.Delegate() {
            @Override
            public void warningsDialogDismissed(CourseWarningsDialogFragment dialog) {
                dialog.dismiss();
            }

            @Override
            public void warningsDialogSetOverride(CourseWarningsDialogFragment dialog, Course course, boolean override) {
                User.currentUser().getCurrentDocument().setOverrideWarningsForCourse(course, override);
                gridAdapter.notifyItemChanged(position);
            }
        };
        FragmentActivity a = getActivity();
        if (a != null) {
            dialogFragment.show(a.getSupportFragmentManager(), "CourseWarningsFragment");
        }
    }

    /// Grid View Layout

    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;
            outRect.top = space;
        }
    }
}
