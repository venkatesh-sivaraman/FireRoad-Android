package com.base12innovations.android.fireroad.utils;

import com.base12innovations.android.fireroad.CourseNavigatorDelegate;
import com.base12innovations.android.fireroad.SearchCoursesFragment;

import java.lang.ref.WeakReference;

public interface BottomSheetNavFragment {
    interface Delegate extends CourseNavigatorDelegate {
        void navFragmentClickedToolbar(BottomSheetNavFragment fragment);
        void navFragmentWantsBack(BottomSheetNavFragment fragment);
        void sortTypeUpdate(SearchCoursesFragment.SortType sortType);
    }

    WeakReference<Delegate> delegate = null;
}
