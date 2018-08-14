package com.base12innovations.android.fireroad;

import java.lang.ref.WeakReference;

public interface BottomSheetNavFragment {
    interface Delegate extends CourseNavigatorDelegate {
        void navFragmentClickedToolbar(BottomSheetNavFragment fragment);
        void navFragmentWantsBack(BottomSheetNavFragment fragment);
    }

    WeakReference<Delegate> delegate = null;
}
