package com.base12innovations.android.fireroad.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;

import java.util.HashMap;
import java.util.Map;

import static com.base12innovations.android.fireroad.models.doc.RoadDocument.SubjectMarker;

/**
 * Allows the user to select a subject marker for a given course.
 */
public class MarkerDialogFragment extends DialogFragment {

    public RoadDocument.SubjectMarker currentValue;
    public String subjectID;

    public OnCompleteListener listener;

    private static Map<Integer, RoadDocument.SubjectMarker> markerButtonIDs = new HashMap<>();
    static {
        markerButtonIDs.put(R.id.buttonNone, null);
        markerButtonIDs.put(R.id.buttonPNR, SubjectMarker.PNR);
        markerButtonIDs.put(R.id.buttonABCNR, SubjectMarker.ABCNR);
        markerButtonIDs.put(R.id.buttonPDF, SubjectMarker.PDF);
        markerButtonIDs.put(R.id.buttonExp, SubjectMarker.EXPLORATORY);
        markerButtonIDs.put(R.id.buttonEasy, SubjectMarker.EASY);
        markerButtonIDs.put(R.id.buttonDifficult, SubjectMarker.DIFFICULT);
        markerButtonIDs.put(R.id.buttonMaybe, SubjectMarker.MAYBE);
        markerButtonIDs.put(R.id.buttonListener, SubjectMarker.LISTENER);
    }

    private Integer idForMarker(SubjectMarker marker) {
        for (Integer key: markerButtonIDs.keySet()) {
            if (markerButtonIDs.get(key) == marker) {
                return key;
            }
        }
        return 0;
    }

    public MarkerDialogFragment() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.fragment_marker_dialog, null);

        // Initialize radio group
        RadioGroup radioGroup = layout.findViewById(R.id.markersRadioGroup);
        radioGroup.check(idForMarker(currentValue));
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                currentValue = markerButtonIDs.get(i);
            }
        });

        builder.setView(layout)
                // Add action buttons
                .setTitle("Set marker for " + subjectID)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (listener != null)
                            listener.selectedMarker(currentValue);
                    }
                });
        return builder.create();
    }

    public interface OnCompleteListener {
        void selectedMarker(RoadDocument.SubjectMarker newValue);
    }
}
