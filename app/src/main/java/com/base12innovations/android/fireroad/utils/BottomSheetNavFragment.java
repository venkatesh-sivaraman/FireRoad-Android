package com.base12innovations.android.fireroad.utils;

import com.base12innovations.android.fireroad.CourseNavigatorDelegate;

import java.lang.ref.WeakReference;

public interface BottomSheetNavFragment {
    interface Delegate extends CourseNavigatorDelegate {
        void navFragmentClickedToolbar(BottomSheetNavFragment fragment);
        void navFragmentWantsBack(BottomSheetNavFragment fragment);
    }

    WeakReference<Delegate> delegate = null;
}
