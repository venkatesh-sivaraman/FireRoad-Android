package com.base12innovations.android.fireroad.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.base12innovations.android.fireroad.TaskDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CourseSearchEngine {
    private static CourseSearchEngine _shared;

    private CourseSearchEngine() { }

    public static CourseSearchEngine sharedInstance() {
        if (_shared == null) {
            _shared = new CourseSearchEngine();
        }
        return _shared;
    }

    public void initialize(Context context) {
        preferences = context.getSharedPreferences(COURSE_SEARCH_PREFERENCES, Context.MODE_PRIVATE);
    }

    private class SearchItem {
        Course course;
        float relevance;

        SearchItem(Course course, float relevance) {
            this.course = course;
            this.relevance = relevance;
        }
    }

    public float searchProgress = 0.0f;

    public List<Course> searchSubjectsFast(String query) {
        List<Course> results = CourseManager.sharedInstance().courseDatabase.daoAccess().searchCoursesByIDOrTitle(query);
        // Sort results by if they match subject ID vs subject title
        List<SearchItem> searchItems = new ArrayList<>();
        for (Course course : results) {
            float relevance = 0.0f;
            if (course.getSubjectID().contains(query)) {
                if (course.getSubjectID().indexOf(query) == 0) {
                    relevance += 2.0f;
                } else {
                    relevance += 1.0f;
                }
            } else if (course.subjectTitle.contains(query)) {
                if (course.subjectTitle.indexOf(query) == 0) {
                    relevance += 3.0f;
                } else {
                    relevance += 0.5f;
                }
            }
            searchItems.add(new SearchItem(course, relevance));
        }

        return sortedSearchResults(searchItems);
    }

    public List<Course> searchSubjects(String query) {
        String[] queryComps = query.toLowerCase().split("[ ;:,]");
        searchProgress = 0.0f;
        List<Course> allCourses = CourseManager.sharedInstance().courseDatabase.daoAccess().allCourses();
        float interval = 1.0f / (float)allCourses.size();
        List<SearchItem> searchItems = new ArrayList<>();
        for (Course course : allCourses) {
            if (course.getSubjectID() == null)
                continue;
            searchProgress += interval;
            List<String> searchFields = new ArrayList<>();
            searchFields.add(course.getSubjectID().toLowerCase());
            if (course.subjectTitle != null)
                searchFields.add(course.subjectTitle.toLowerCase());
            if (course.subjectDescription!= null)
                searchFields.add(course.subjectDescription.toLowerCase());
            if (course.getInstructors() != null)
                searchFields.add(course.getInstructors().toLowerCase());
            if (course.prerequisites != null)
                searchFields.add(course.prerequisites.toLowerCase());
            if (course.corequisites != null)
                searchFields.add(course.corequisites.toLowerCase());
            if (course.relatedSubjects != null)
                searchFields.add(course.relatedSubjects.toLowerCase());
            float relevance = 0.0f;
            for (String comp : queryComps) {
                boolean found = false;
                for (int i = 0; i < searchFields.size(); i++) {
                    String field = searchFields.get(i);
                    if (field.contains(comp)) {
                        found = true;
                        if (field.indexOf(comp) == 0)
                            relevance += (float)(searchFields.size() - i) * 2.0f;
                        else
                            relevance += (float)(searchFields.size() - i);
                    }
                }
                if (!found) {
                    relevance = 0.0f;
                    break;
                }
            }

            if (relevance > 0.0f) {
                if (course.enrollmentNumber > 0) {
                    relevance *= Math.log(Math.min((double)course.enrollmentNumber, 2.0));
                }
                searchItems.add(new SearchItem(course, relevance));
            }
        }
        return sortedSearchResults(searchItems);
    }

    private List<Course> sortedSearchResults(List<SearchItem> searchItems) {
        Collections.sort(searchItems, new Comparator<SearchItem>() {
            @Override
            public int compare(SearchItem t1, SearchItem t2) {
                if (t1.relevance != t2.relevance) {
                    return (t1.relevance > t2.relevance ? -1 : 1);
                } else if (t1.course.getSubjectID().length() != t2.course.getSubjectID().length()) {
                    return (t1.course.getSubjectID().length() < t2.course.getSubjectID().length()) ? -1 : 1;
                }
                return t1.course.getSubjectID().compareTo(t2.course.getSubjectID());
            }
        });
        List<Course> finalResults = new ArrayList<>();
        for (SearchItem item : searchItems) {
            finalResults.add(item.course);
        }
        return finalResults;
    }

    // Recents

    private static String COURSE_SEARCH_PREFERENCES = "com.base12innovations.android.fireroad.courseSearchPreferences";
    private SharedPreferences preferences;
    private static String recentCoursesKey = "recentlySearchedCourses";
    private static int NUM_RECENT_COURSES = 5;
    private List<Course> recentCourses;

    public interface RecentCoursesCallback {
        void result(List<Course> courses);
    }

    public void getRecentCourses(final RecentCoursesCallback callback) {
        if (recentCourses == null) {
            TaskDispatcher.perform(new TaskDispatcher.Task<List<Course>>() {
                @Override
                public List<Course> perform() {
                    String rawRecents = preferences.getString(recentCoursesKey, "");
                    String[] subjectIDs = rawRecents.split(",");
                    List<Course> result = new ArrayList<>();
                    for (int i = 0; i < subjectIDs.length; i++) {
                        if (subjectIDs[i].length() == 0)
                            continue;
                        Course newCourse = CourseManager.sharedInstance().getSubjectByID(subjectIDs[i]);
                        if (newCourse != null)
                            result.add(newCourse);
                    }
                    return result;
                }
            }, new TaskDispatcher.CompletionBlock<List<Course>>() {
                @Override
                public void completed(List<Course> arg) {
                    recentCourses = arg;
                    callback.result(recentCourses);
                }
            });
        } else {
            callback.result(recentCourses);
        }
    }

    public void addRecentCourse(final Course course) {
        if (recentCourses == null) {
            getRecentCourses(new RecentCoursesCallback() {
                @Override
                public void result(List<Course> courses) {
                    recentCourses = courses;
                    addRecentCourse(course);
                }
            });
            return;
        }

        recentCourses.remove(course);
        recentCourses.add(0, course);
        if (recentCourses.size() > NUM_RECENT_COURSES) {
            recentCourses.remove(recentCourses.size() - 1);
        }
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                SharedPreferences.Editor editor = preferences.edit();
                List<String> subjectIDs = new ArrayList<>();
                for (Course course : recentCourses) {
                    subjectIDs.add(course.getSubjectID());
                }
                editor.putString(recentCoursesKey, TextUtils.join(",", subjectIDs));
                editor.apply();
            }
        });
    }
}
