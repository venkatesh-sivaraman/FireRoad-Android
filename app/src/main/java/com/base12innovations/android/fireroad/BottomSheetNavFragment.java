package com.base12innovations.android.fireroad;

import java.lang.ref.WeakReference;

public interface BottomSheetNavFragment {
    interface Delegate {
        void navFragmentClickedToolbar(BottomSheetNavFragment fragment);
        void navFragmentWantsCourseDetails(BottomSheetNavFragment fragment, Course course);
        void navFragmentWantsBack(BottomSheetNavFragment fragment);
        void navFragmentAddedCourse(BottomSheetNavFragment fragment, Course course, int semester);
    }

    WeakReference<Delegate> delegate = null;
}
