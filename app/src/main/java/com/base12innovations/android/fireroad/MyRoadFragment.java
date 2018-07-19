package com.base12innovations.android.fireroad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

    private int testParam;

    private Course currentlySelectedCourse;
    private int currentlySelectedSemester;

    private RoadDocument _document;

    public void setDocument(RoadDocument document) {
        _document = document;
        if (gridAdapter != null) {
            gridAdapter.setDocument(document);
        }
    }

    public RoadDocument getDocument() {
        return _document;
    }

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
        if (getArguments() != null) {
            testParam = getArguments().getInt(ARG_TEST_PARAM);
        }
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
        GridView grid = layout.findViewById(R.id.gridView);
        gridAdapter = new MyRoadCoursesAdapter(getActivity(), null, 3);
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
                gridAdapter.setDocument(document);
                setDocument(document);
                loadingIndicator.setVisibility(ProgressBar.GONE);
            }
        });

        /*if (document.getAllCourses().size() == 0) {
            document.addCourse(new Course("8.02", "Physics"), 0);
            document.addCourse(new Course("18.03", "Differential Equations"), 1);
            document.addCourse(new Course("16.00", "Unified"), 1);
            document.addCourse(new Course("6.036", "Introduction to Machine Learning"), 3);
        }*/

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
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
        void onFragmentInteraction(Uri uri);
    }

    public void showCourseDetails(Course course) {
        Intent intent = new Intent(getActivity(), CourseDetailsActivity.class);
        intent.putExtra(CourseDetailsActivity.COURSE_EXTRA, currentlySelectedCourse);
        startActivity(intent);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.viewCourse:
                showCourseDetails(currentlySelectedCourse);
                return true;
            case R.id.deleteCourse:
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getDocument().removeCourse(currentlySelectedCourse, currentlySelectedSemester);
                        gridAdapter.notifyDataSetChanged();
                    }
                }, 400);
                return true;
            case R.id.courseWarnings:
                return false;
            default:
                return false;
        }
    }
}
