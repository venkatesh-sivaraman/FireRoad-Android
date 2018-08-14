package com.base12innovations.android.fireroad;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.base12innovations.android.fireroad.models.Course;


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
            ((TextView)layout.findViewById(R.id.titleLabel)).setText(requirementsListID);
        }
        return layout;
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
