package com.base12innovations.android.fireroad.activity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.models.course.ColorManager;

public class IntroActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private View colorView;
    private int currentColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        colorView = findViewById(R.id.backgroundColorView);
        currentColor = ContextCompat.getColor(this, R.color.intro_1);
        Window window = IntroActivity.this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ColorManager.darkenColor(currentColor, 0xFF));

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mSectionsPagerAdapter.onNextListener = new PlaceholderFragment.OnNextListener() {
            @Override
            public void nextTapped(int currentIndex) {
                if (currentIndex < 3) {
                    mViewPager.setCurrentItem(currentIndex + 1, true);
                } else {
                    // Show dialog, then dismiss
                    promptYearAndDismiss();
                }
            }
        };

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int pageIndex) {
                int colorFrom = currentColor;
                int resId = 0;
                switch (pageIndex % 4) {
                    case 0: resId = R.color.intro_1; break;
                    case 1: resId = R.color.intro_2; break;
                    case 2: resId = R.color.intro_3; break;
                    case 3:
                    default: resId = R.color.intro_4; break;
                }
                int colorTo = ContextCompat.getColor(IntroActivity.this, resId);
                if (colorFrom != colorTo) {
                    final Window window = IntroActivity.this.getWindow();
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(400); // milliseconds
                    colorAnimation.start();
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            colorView.setBackgroundColor((int) valueAnimator.getAnimatedValue());
                            window.setStatusBarColor(ColorManager.darkenColor((int) valueAnimator.getAnimatedValue(), 0xFF));
                        }
                    });
                    currentColor = colorTo;
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    public void promptYearAndDismiss() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Please provide your current (upcoming) class year:");
        b.setSingleChoiceItems(R.array.class_year_titles,
                AppSettings.shared().getInt(AppSettings.CLASS_YEAR, 1) - 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AppSettings.shared().edit().putInt(AppSettings.CLASS_YEAR, i + 1).apply();
                    }
                });
        b.setPositiveButton("Start Using FireRoad", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                setResult(RESULT_OK);
                finish();
            }
        });
        b.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_intro, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_intro, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.descriptionLabel);
            ImageView imageView = (ImageView)rootView.findViewById(R.id.imageView);
            AppCompatButton nextButton = rootView.findViewById(R.id.nextButton);
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onNextListener != null)
                        onNextListener.nextTapped(getArguments().getInt(ARG_SECTION_NUMBER));
                }
            });
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 0:
                    textView.setText(R.string.intro_page_1);
                    imageView.setImageResource(R.drawable.demo_for_you);
                    break;
                case 1:
                    textView.setText(R.string.intro_page_2);
                    imageView.setImageResource(R.drawable.demo_my_road);
                    break;
                case 2:
                    textView.setText(R.string.intro_page_3);
                    imageView.setImageResource(R.drawable.demo_requirements);
                    break;
                case 3:
                    textView.setText(R.string.intro_page_4);
                    imageView.setImageResource(R.drawable.demo_schedule);
                    nextButton.setText("Continue");
                    break;
            }
            return rootView;
        }

        public interface OnNextListener {
            void nextTapped(int currentIndex);
        }

        public OnNextListener onNextListener;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public PlaceholderFragment.OnNextListener onNextListener;

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            PlaceholderFragment frag = PlaceholderFragment.newInstance(position);
            frag.onNextListener = onNextListener;
            return frag;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }
    }
}
