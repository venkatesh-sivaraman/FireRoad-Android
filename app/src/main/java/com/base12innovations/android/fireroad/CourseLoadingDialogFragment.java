package com.base12innovations.android.fireroad;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import com.base12innovations.android.fireroad.models.CourseManager;


public class CourseLoadingDialogFragment extends DialogFragment {

    public CourseLoadingDialogFragment() {
        // Required empty public constructor
    }

    private ProgressBar progressBar;
    private Handler progressHandler;

    private boolean shouldStop;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_course_loading_dialog, null);
        builder.setView(view);
        progressBar = view.findViewById(R.id.progressBar);
        progressHandler = new Handler(Looper.getMainLooper());
        progressHandler.postDelayed(updateTimerThread, 500);
        return builder.create();
    }

    private Runnable updateTimerThread = new Runnable()
    {
        public void run()
        {
            int progress = (int)(CourseManager.sharedInstance().getLoadingProgress() * 100.0f);
            progressBar.setProgress(progress);
            if (progress < 99 && !shouldStop) {
                progressHandler.postDelayed(this, 500);
            }
        }
    };

    @Override
    public void onDestroy() {
        shouldStop = true;
        super.onDestroy();
    }
}
