package com.base12innovations.android.fireroad;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.CourseManager;
import com.base12innovations.android.fireroad.models.RoadDocument;
import com.base12innovations.android.fireroad.models.User;

import java.io.File;
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

    private Course currentlySelectedCourse;
    private int currentlySelectedSemester;
    private int currentlySelectedPosition;
    private RecyclerView recyclerView;
    private PopupMenu currentPopupMenu;

    private OnFragmentInteractionListener mListener;

    public MyRoadFragment() {
        // Required empty public constructor
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

        // Set up grid view

        gridAdapter = new MyRoadCoursesAdapter(null, 3);
        /*GridView grid = layout.findViewById(R.id.gridView);
        grid.setAdapter(gridAdapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                currentlySelectedCourse = gridAdapter.courseForGridPosition(i);
                currentlySelectedSemester = gridAdapter.semesterForGridPosition(i);
                PopupMenu menu = new PopupMenu(getActivity(), view);
                MenuInflater mInflater = menu.getMenuInflater();
                mInflater.inflate(R.menu.menu_course_cell, menu.getMenu());
                menu.setOnMenuItemClickListener(MyRoadFragment.this);
                menu.show();
            }
        });*/
        recyclerView = layout.findViewById(R.id.coursesRecyclerView);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 3);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(gridAdapter);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.course_cell_spacing);
        recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        gridAdapter.itemClickListener = new MyRoadCoursesAdapter.ClickListener() {
            @Override
            public void onClick(Course course, int position, View view) {
                showCourseDetails(course);
            }
        };
        gridAdapter.itemLongClickListener = new MyRoadCoursesAdapter.ClickListener() {
            @Override
            public void onClick(Course course, int position, View view) {
                currentlySelectedCourse = course;
                currentlySelectedSemester = gridAdapter.semesterForGridPosition(position);
                currentlySelectedPosition = position;
                PopupMenu menu = new PopupMenu(getActivity(), view);
                MenuInflater mInflater = menu.getMenuInflater();
                mInflater.inflate(R.menu.menu_course_cell, menu.getMenu());
                menu.setOnMenuItemClickListener(MyRoadFragment.this);
                menu.show();
                currentPopupMenu = menu;
            }
        };


        // Support drag and drop to move courses
        ItemTouchHelper.Callback _ithCallback = new ItemTouchHelper.Callback() {
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                // get the viewHolder's and target's positions in your adapter data, swap them
                if (currentPopupMenu != null) {
                    currentPopupMenu.dismiss();
                    currentPopupMenu = null;
                }
                return gridAdapter.moveCourse(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) { }

            //defines the enabled move directions in each state (idle, swiping, dragging).
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (gridAdapter.isSectionHeader(viewHolder.getAdapterPosition())) {
                    return 0;
                }
                if (currentPopupMenu != null) {
                    currentPopupMenu.dismiss();
                    currentPopupMenu = null;
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

        layoutManager.setSpanSizeLookup(gridAdapter.spanSizeLookup());

        User.currentUser().addRoadChangedListener(new User.RoadChangedListener() {
            @Override
            public void roadChanged(RoadDocument newDocument) {
                if (gridAdapter != null) {
                    gridAdapter.setDocument(newDocument);
                }
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

    public void finishLoadingView() {

        // Set up model
        Activity currentActivity = getActivity();
        if (currentActivity == null) {
            return;
        }
        if (User.currentUser().getCurrentDocument() == null) {
            final RoadDocument document = new RoadDocument(new File(currentActivity.getFilesDir(), "First Steps.road"));
            TaskDispatcher.perform(new TaskDispatcher.Task<Void>() {
                @Override
                public Void perform() {
                    if (document.file.exists()) {
                        document.read();
                    }
                    return null;
                }
            }, new TaskDispatcher.CompletionBlock<Void>() {
                @Override
                public void completed(Void arg) {
                    User.currentUser().setCurrentDocument(document);
                    loadingIndicator.setVisibility(ProgressBar.GONE);
                }
            });
        } else {
            if (gridAdapter != null) {
                gridAdapter.setDocument(User.currentUser().getCurrentDocument());
            }
            loadingIndicator.setVisibility(ProgressBar.GONE);
        }

        /*if (document.getAllCourses().size() == 0) {
            document.addCourse(new Course("8.02", "Physics"), 0);
            document.addCourse(new Course("18.03", "Differential Equations"), 1);
            document.addCourse(new Course("16.00", "Unified"), 1);
            document.addCourse(new Course("6.036", "Introduction to Machine Learning"), 3);
        }*/

    }

    public void roadAddedCourse(Course course, int semester) {
        if (gridAdapter != null) {
            int position = gridAdapter.lastPositionForSemester(semester);
            gridAdapter.notifyItemInserted(position);
            if (recyclerView != null) {
                recyclerView.scrollToPosition(position);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onShowCourseDetails(Course course);
    }

    public void showCourseDetails(Course course) {
        /*Intent intent = new Intent(getActivity(), CourseDetailsFragment.class);
        intent.putExtra(CourseDetailsFragment.COURSE_EXTRA, currentlySelectedCourse);
        startActivity(intent);*/
        if (mListener != null) {
            mListener.onShowCourseDetails(course);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
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
                    }
                }, 400);
                return true;
            case R.id.courseWarnings:
                return false;
            default:
                return false;
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
