package com.base12innovations.android.fireroad.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.course.CourseSearchEngine;

import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class FilterDialogFragment extends DialogFragment {

    public interface Delegate {
        void filterDialogDismissed(FilterDialogFragment fragment);
    }

    public WeakReference<Delegate> delegate = new WeakReference<>(null);
    public EnumSet<CourseSearchEngine.Filter> filters;

    private View mLayout;

    public FilterDialogFragment() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_filter_dialog, null);
        mLayout = view;
        builder.setView(view);

        updateButtonsInLayout(view);
        builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (delegate.get() != null) {
                    delegate.get().filterDialogDismissed(FilterDialogFragment.this);
                }
            }
        });

        return builder.create();
    }

    private void updateButtonsInLayout(View view) {
        updateButton(view, R.id.levelNone, CourseSearchEngine.Filter.LEVEL_NONE);
        updateButton(view, R.id.levelUG, CourseSearchEngine.Filter.LEVEL_UG);
        updateButton(view, R.id.levelG, CourseSearchEngine.Filter.LEVEL_G);

        updateButton(view, R.id.offeredNone, CourseSearchEngine.Filter.OFFERED_NONE);
        updateButton(view, R.id.offeredFall, CourseSearchEngine.Filter.OFFERED_FALL);
        updateButton(view, R.id.offeredIAP, CourseSearchEngine.Filter.OFFERED_IAP);
        updateButton(view, R.id.offeredSpring, CourseSearchEngine.Filter.OFFERED_SPRING);

        updateButton(view, R.id.noHASS, CourseSearchEngine.Filter.HASS_NONE);
        updateButton(view, R.id.HASS, CourseSearchEngine.Filter.HASS);
        updateButton(view, R.id.HASS_A, CourseSearchEngine.Filter.HASS_A);
        updateButton(view, R.id.HASS_H, CourseSearchEngine.Filter.HASS_H);
        updateButton(view, R.id.HASS_S, CourseSearchEngine.Filter.HASS_S);

        updateButton(view, R.id.girNone, CourseSearchEngine.Filter.GIR_NONE);
        updateButton(view, R.id.girAny, CourseSearchEngine.Filter.GIR);
        updateButton(view, R.id.girLab, CourseSearchEngine.Filter.GIR_LAB);
        updateButton(view, R.id.girRest, CourseSearchEngine.Filter.GIR_REST);

        updateButton(view, R.id.noCI, CourseSearchEngine.Filter.CI_NONE);
        updateButton(view, R.id.CI_H, CourseSearchEngine.Filter.CI_H);
        updateButton(view, R.id.CI_HW, CourseSearchEngine.Filter.CI_HW);
        updateButton(view, R.id.notCI, CourseSearchEngine.Filter.NOT_CI);

        Button clearAll = (Button)view.findViewById(R.id.noFilter);
        clearAll.setEnabled(!filters.equals(CourseSearchEngine.Filter.noFilter));
        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filters = CourseSearchEngine.Filter.noFilter;
                if (delegate.get() != null) {
                    delegate.get().filterDialogDismissed(FilterDialogFragment.this);
                }
            }
        });
    }

    private Map<Integer, Integer> colorCache = new HashMap<>();

    private void updateButton(View view, int resId, final CourseSearchEngine.Filter option) {
        Button btn = (Button)view.findViewById(resId);
        if (filters.contains(option)) {
            if (colorCache.containsKey(resId)) {
                ColorStateList myColorStateList = new ColorStateList(
                        new int[][]{
                                new int[]{android.R.attr.state_enabled}
                        },
                        new int[] {
                                colorCache.get(resId)
                        }
                );
                btn.setBackgroundTintList(myColorStateList);
            }
            btn.setTextColor(0xFFFFFFFF);
            //btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            if (!colorCache.containsKey(resId)) {
                try {
                    int color = btn.getBackgroundTintList().getDefaultColor();
                    if (color != 0xFFFFFFFF) {
                        colorCache.put(resId, color);
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            ColorStateList myColorStateList = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_enabled}
                    },
                    new int[] {
                            0xFFFFFFFF
                    }
            );
            btn.setBackgroundTintList(myColorStateList);
            if (colorCache.containsKey(resId))
                btn.setTextColor(colorCache.get(resId));
            else
                btn.setTextColor(0xFF000000);
            //btn.setPaintFlags(btn.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (option) {
                    case LEVEL_NONE:
                    case LEVEL_UG:
                    case LEVEL_G:
                        CourseSearchEngine.Filter.filterLevel(filters, option);
                        break;
                    case OFFERED_NONE:
                    case OFFERED_FALL:
                    case OFFERED_IAP:
                    case OFFERED_SPRING:
                        CourseSearchEngine.Filter.filterOffered(filters, option);
                        break;
                    case GIR_NONE:
                    case GIR:
                    case GIR_LAB:
                    case GIR_REST:
                        CourseSearchEngine.Filter.filterGIR(filters, option);
                        break;
                    case HASS_NONE:
                    case HASS:
                    case HASS_A:
                    case HASS_S:
                    case HASS_H:
                        CourseSearchEngine.Filter.filterHASS(filters, option);
                        break;
                    case CI_NONE:
                    case CI_H:
                    case CI_HW:
                    case NOT_CI:
                        CourseSearchEngine.Filter.filterCI(filters, option);
                        break;
                    default:
                        break;
                }
                updateButtonsInLayout(mLayout);
            }
        });
    }
}
