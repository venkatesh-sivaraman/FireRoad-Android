package com.base12innovations.android.fireroad;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.base12innovations.android.fireroad.adapter.RequirementsBrowserAdapter;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.course.CourseManager;
import com.base12innovations.android.fireroad.models.course.CourseSearchEngine;
import com.base12innovations.android.fireroad.models.req.RequirementsList;
import com.base12innovations.android.fireroad.models.req.RequirementsListManager;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RequirementsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RequirementsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RequirementsFragment extends Fragment implements RequirementsListFragment.OnFragmentInteractionListener {

    private OnFragmentInteractionListener mListener;

    private Spinner courseSelector;
    private View listEmbedView;
    private int currentSelection;
    private RequirementsListFragment currentListFragment;
    RequirementsBrowserAdapter spinnerAdapter;
    private ProgressBar loadingIndicator;

    private static final String PREFERENCES = "com.base12innovations.android.fireroad.requirementsFragmentPrefs";
    private static final String LAST_REQ_LIST_KEY = "lastRequirementsListID";

    public RequirementsListFragment getCurrentListFragment() {
        return currentListFragment;
    }

    public RequirementsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RequirementsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RequirementsFragment newInstance() {
        RequirementsFragment fragment = new RequirementsFragment();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
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
        View layout = inflater.inflate(R.layout.fragment_requirements, container, false);

        listEmbedView = layout.findViewById(R.id.requirementsList);
        courseSelector = layout.findViewById(R.id.courseSelector);
        loadingIndicator = layout.findViewById(R.id.loadingIndicator);
        if (CourseManager.sharedInstance().isLoaded()) {
            loadingIndicator.setVisibility(View.GONE);
            setupRequirementsListSelector();
        } else {
            loadingIndicator.setVisibility(View.VISIBLE);
            CourseManager.sharedInstance().waitForLoad(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    loadingIndicator.setVisibility(View.GONE);
                    setupRequirementsListSelector();
                    return null;
                }
            });
        }
        return layout;
    }

    private void setupRequirementsListSelector() {
        final List<RequirementsList> reqLists = RequirementsListManager.sharedInstance().getAllRequirementsLists();

        final RequirementsBrowserAdapter adapter = new RequirementsBrowserAdapter(getActivity(), reqLists);
        spinnerAdapter = adapter;

        courseSelector.setAdapter(adapter);
        courseSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentSelection != i) {
                    currentSelection = i;
                    RequirementsList list = (RequirementsList) adapter.getItem(i);
                    setLastShownRequirementsListID(list.listID);
                    showRequirementsList(list);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                currentSelection = 1;
            }
        });
        String lastShown = lastShownRequirementsListID();
        if (lastShown != null)
            courseSelector.setSelection(spinnerAdapter.indexOf(RequirementsListManager.sharedInstance().getRequirementsList(lastShown)));
        else
            courseSelector.setSelection(1);
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
        if (courseSelector != null && currentSelection != 0) {
            int oldSelection = currentSelection;
            currentSelection = 0;
            courseSelector.setSelection(oldSelection);
        }

        showHelpText();
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    public void showRequirementsList(RequirementsList list) {
        RequirementsListFragment fragment = RequirementsListFragment.newInstance(list.listID);
        fragment.delegate = this;
        currentListFragment = fragment;
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction().replace(listEmbedView.getId(), fragment).commit();
    }

    public void notifyRequirementsStatusChanged() {
        if (currentListFragment != null)
            currentListFragment.updateRequirementStatus();
        if (spinnerAdapter != null)
            spinnerAdapter.resetRequirementsCache();
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

    // List listener


    @Override
    public void courseNavigatorAddedCourse(Fragment source, Course course, String semesterID) {
        if (mListener != null) {
            mListener.courseNavigatorAddedCourse(this, course, semesterID);
        }
    }

    @Override
    public void courseNavigatorWantsCourseDetails(Fragment source, Course course) {
        if (mListener != null) {
            mListener.courseNavigatorWantsCourseDetails(this, course);
        }
    }

    @Override
    public void courseNavigatorWantsSearchCourses(Fragment source, String searchTerm, EnumSet<CourseSearchEngine.Filter> filters) {
        if (mListener != null) {
            mListener.courseNavigatorWantsSearchCourses(this, searchTerm, filters);
        }
    }

    @Override
    public void fragmentUpdatedCoursesOfStudy(RequirementsListFragment fragment) {
        int newSelection = spinnerAdapter.setRequirementsLists(RequirementsListManager.sharedInstance().getAllRequirementsLists(),
                courseSelector.getSelectedItemPosition());
        courseSelector.setSelection(newSelection);
        spinnerAdapter.notifyDataSetChanged();
    }

    // Recent list

    public String lastShownRequirementsListID() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getString(LAST_REQ_LIST_KEY, null);
    }

    public void setLastShownRequirementsListID(String id) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_REQ_LIST_KEY, id);
        editor.apply();
    }

    // Help text

    private static final String HAS_SHOWN_HELP = "hasShownHelp";

    private boolean hasShownHelpText() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getBoolean(HAS_SHOWN_HELP, false);
    }

    void showHelpText() {
        if (hasShownHelpText()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Requirements Tab");
        builder.setMessage("* Use the popup menu to select a course of study.\n* Tap a course to view details, or tap and hold to add directly.\n* Tap \"Add to My Courses\" to save as your major or minor.");

        builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                SharedPreferences prefs = getContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(HAS_SHOWN_HELP, true).apply();
            }
        });
        builder.show();
    }
}
