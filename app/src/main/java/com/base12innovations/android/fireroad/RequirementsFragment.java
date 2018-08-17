package com.base12innovations.android.fireroad;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.base12innovations.android.fireroad.models.Course;
import com.base12innovations.android.fireroad.models.RequirementsList;
import com.base12innovations.android.fireroad.models.RequirementsListManager;

import java.util.ArrayList;
import java.util.List;


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
        setupRequirementsListSelector();
        if (savedInstanceState == null) {
            courseSelector.setSelection(1);
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
                    showRequirementsList((RequirementsList) adapter.getItem(i));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                currentSelection = 1;
            }
        });
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
    public void courseNavigatorAddedCourse(Fragment source, Course course, int semester) {
        if (mListener != null) {
            mListener.courseNavigatorAddedCourse(this, course, semester);
        }
    }

    @Override
    public void courseNavigatorWantsCourseDetails(Fragment source, Course course) {
        if (mListener != null) {
            mListener.courseNavigatorWantsCourseDetails(this, course);
        }
    }

    @Override
    public void courseNavigatorWantsSearchCourses(Fragment source, String searchTerm) {
        if (mListener != null) {
            mListener.courseNavigatorWantsSearchCourses(this, searchTerm);
        }
    }
}
