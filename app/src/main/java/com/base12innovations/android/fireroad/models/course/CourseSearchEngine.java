package com.base12innovations.android.fireroad.models.course;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

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

    // Filter options

    public enum Filter {
        GIR_NONE, GIR, GIR_LAB, GIR_REST,
        HASS_NONE, HASS, HASS_A, HASS_S, HASS_H,
        CI_NONE, CI_H, CI_HW, NOT_CI,
        OFFERED_NONE, OFFERED_FALL, OFFERED_SPRING, OFFERED_IAP,
        LEVEL_NONE, LEVEL_UG, LEVEL_G,
        ATTENDANCE_NONE, ATTENDANCE_VIRTUAL, ATTENDANCE_INPERSON,
        SEARCH_ID, SEARCH_TITLE, SEARCH_PREREQS, SEARCH_COREQS, SEARCH_INSTRUCTORS, SEARCH_REQUIREMENTS,
        CONFLICTS_ANY, CONFLICTS_NO_LECTURE, CONFLICTS_NOT_ALLOWED,
        CONTAINS, MATCHES;

        public static EnumSet<Filter> allGIRFilters = EnumSet.of(GIR_NONE, GIR, GIR_LAB, GIR_REST);
        public static EnumSet<Filter> allHASSFilters = EnumSet.of(HASS_NONE, HASS, HASS_A, HASS_S, HASS_H);
        public static EnumSet<Filter> allCIFilters = EnumSet.of(CI_NONE, CI_H, CI_HW, NOT_CI);
        public static EnumSet<Filter> allOfferedFilters = EnumSet.of(OFFERED_NONE, OFFERED_FALL, OFFERED_IAP, OFFERED_SPRING);
        public static EnumSet<Filter> allLevelFilters = EnumSet.of(LEVEL_NONE, LEVEL_UG, LEVEL_G);
        public static EnumSet<Filter> allAttendanceFilters = EnumSet.of(ATTENDANCE_NONE, ATTENDANCE_VIRTUAL, ATTENDANCE_INPERSON);
        public static EnumSet<Filter> allConflictsFilters = EnumSet.of(CONFLICTS_ANY, CONFLICTS_NO_LECTURE, CONFLICTS_NOT_ALLOWED);
        public static EnumSet<Filter> searchAllFields = EnumSet.of(SEARCH_ID, SEARCH_TITLE, SEARCH_PREREQS, SEARCH_COREQS, SEARCH_INSTRUCTORS, SEARCH_REQUIREMENTS);

        private static EnumSet<Filter> noFilter = union(searchAllFields, EnumSet.of(GIR_NONE, HASS_NONE, CI_NONE, LEVEL_NONE, ATTENDANCE_NONE, OFFERED_NONE, CONFLICTS_ANY, CONTAINS));
        public static EnumSet<Filter> noFilter() {
            return EnumSet.copyOf(noFilter);
        }

        @SafeVarargs
        private static EnumSet<Filter> union(EnumSet<Filter> ... enums) {
            if (enums.length == 0)
                return EnumSet.noneOf(Filter.class);
            EnumSet<Filter> result = EnumSet.copyOf(enums[0]);
            for (int i = 1; i < enums.length; i++)
                result.addAll(enums[i]);
            return result;
        }

        // Convenience functions to replace certain axes of filter options

        public static void filterGIR(EnumSet<Filter> filter, Filter girOption) {
            filter.removeAll(allGIRFilters);
            filter.add(girOption);
        }
        public static void filterHASS(EnumSet<Filter> filter, Filter hassOption) {
            filter.removeAll(allHASSFilters);
            filter.add(hassOption);
        }
        public static void filterCI(EnumSet<Filter> filter, Filter ciOption) {
            filter.removeAll(allCIFilters);
            filter.add(ciOption);
        }
        public static void filterOffered(EnumSet<Filter> filter, Filter offeredOption) {
            filter.removeAll(allOfferedFilters);
            filter.add(offeredOption);
        }
        public static void filterLevel(EnumSet<Filter> filter, Filter levelOption) {
            filter.removeAll(allLevelFilters);
            filter.add(levelOption);
        }
        public static void filterConflict(EnumSet<Filter> filter, Filter conflictOption) {
            filter.removeAll(allConflictsFilters);
            filter.add(conflictOption);
        }
        public static void filterAttendance(EnumSet<Filter> filter, Filter attendanceOption) {
            filter.removeAll(allAttendanceFilters);
            filter.add(attendanceOption);
        }
        public static void filterSearchField(EnumSet<Filter> filter, Filter searchField) {
            filter.removeAll(searchAllFields);
            filter.add(searchField);
        }
        public static void exactMatch(EnumSet<Filter> filter) {
            filter.remove(CONTAINS);
            filter.add(MATCHES);
        }
    }

    // Searching

    private class SearchItem {
        Course course;
        float relevance;

        SearchItem(Course course, float relevance) {
            this.course = course;
            this.relevance = relevance;
        }
    }

    public float searchProgress = 0.0f;

    /**
     * Searches the course database using an SQL select query.
     * @param query - the search string.
     * @param filters - the search filters to apply.
     * @return a list of courses whose subject IDs or titles match the query exactly.
     */
    public List<Course> searchSubjectsFast(String query, EnumSet<Filter> filters) {
        List<Course> results = CourseManager.sharedInstance().courseDatabase.daoAccess().searchCoursesByIDOrTitle(query);
        // Sort results by if they match subject ID vs subject title
        List<SearchItem> searchItems = new ArrayList<>();
        for (Course course : results) {
            if (!courseSatisfiesSearchFilters(course, filters))
                continue;
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

    /**
     * Searches the course database one by one, using the search engine's filters if applicable.
     * @param query - the search string.
     * @param filters - filters to apply
     * @return a list of courses which match the query according to the given filters.
     */
    public List<Course> searchSubjects(String query, EnumSet<Filter> filters) {
        String[] queryComps = query.toLowerCase().split("[ ;:,]");
        searchProgress = 0.0f;
        List<Course> allCourses = CourseManager.sharedInstance().courseDatabase.daoAccess().publicCourses();
        allCourses.addAll(CourseManager.genericCourses.values());
        float interval = 1.0f / (float)allCourses.size();
        List<SearchItem> searchItems = new ArrayList<>();
        for (Course course : allCourses) {
            searchProgress += interval;
            if (course.getSubjectID() == null || !courseSatisfiesSearchFilters(course, filters))
                continue;
            List<String> searchFields = searchFieldsForCourse(course, filters);
            float relevance = 0.0f;
            if (query.length() == 0)
                relevance = 1.0f;
            else {
                for (String comp : queryComps) {
                    boolean found = false;
                    String regex = filters.contains(Filter.MATCHES) ? "^.*[^A-Za-z0-9]*" + Pattern.quote(comp) + "[^A-Za-z0-9]*.*$" : "";
                    for (int i = 0; i < searchFields.size(); i++) {
                        String field = searchFields.get(i);
                        if ((filters.contains(Filter.MATCHES) && field.matches(regex)) ||
                                (!filters.contains(Filter.MATCHES) && field.contains(comp))) {
                            found = true;
                            if (field.indexOf(comp) == 0)
                                relevance += (float) (searchFields.size() - i) * 2.0f;
                            else
                                relevance += (float) (searchFields.size() - i);
                        }
                    }
                    if (!found) {
                        relevance = 0.0f;
                        break;
                    }
                }
            }

            if (relevance > 0.0f) {
                if (course.isGeneric)
                    relevance = 1000.0f;
                if (course.enrollmentNumber > 0) {
                    relevance *= Math.log(Math.min((double)course.enrollmentNumber, 2.0));
                }
                searchItems.add(new SearchItem(course, relevance));
            }
        }
        return sortedSearchResults(searchItems);
    }

    private boolean courseSatisfiesSearchFilters(Course course, EnumSet<Filter> filters) {
        if (filters == null) return true;

        boolean fulfillsGIR = false;
        if (filters.contains(Filter.GIR_NONE)) {
            fulfillsGIR = true;
        } else if (filters.contains(Filter.GIR) && course.getGIRAttribute() != null) {
            fulfillsGIR = true;
        } else if (filters.contains(Filter.GIR_LAB) && course.getGIRAttribute() == Course.GIRAttribute.LAB) {
            fulfillsGIR = true;
        } else if (filters.contains(Filter.GIR_REST) && course.getGIRAttribute() == Course.GIRAttribute.REST) {
            fulfillsGIR = true;
        }
        if (!fulfillsGIR) return false;

        boolean fulfillsHASS = false;
        if (filters.contains(Filter.HASS_NONE)) {
            fulfillsHASS = true;
        } else if (course.getHASSAttribute() != null) {
            List<Course.HASSAttribute> hasses = course.getHASSAttribute();
            if (filters.contains(Filter.HASS) && hasses.size() > 0) {
                fulfillsHASS = true;
            } else if (filters.contains(Filter.HASS_A) && hasses.contains(Course.HASSAttribute.ARTS)) {
                fulfillsHASS = true;
            } else if (filters.contains(Filter.HASS_S) && hasses.contains(Course.HASSAttribute.SOCIAL_SCIENCES)) {
                fulfillsHASS = true;
            } else if (filters.contains(Filter.HASS_H) && hasses.contains(Course.HASSAttribute.HUMANITIES)) {
                fulfillsHASS = true;
            }
        }
        if (!fulfillsHASS) return false;

        boolean fulfillsCI = false;
        if (filters.contains(Filter.CI_NONE)) {
            fulfillsCI = true;
        } else if (filters.contains(Filter.CI_H) && course.getCommunicationRequirement() == Course.CommunicationAttribute.CI_H) {
            fulfillsCI = true;
        } else if (filters.contains(Filter.NOT_CI) && course.getCommunicationRequirement() == null) {
            fulfillsCI = true;
        } else if (filters.contains(Filter.CI_HW) && course.getCommunicationRequirement() == Course.CommunicationAttribute.CI_HW) {
            fulfillsCI = true;
        }
        if (!fulfillsCI) return false;

        boolean fulfillsOffered = false;
        if (filters.contains(Filter.OFFERED_NONE)) {
            fulfillsOffered = true;
        } else if (filters.contains(Filter.OFFERED_FALL) && course.isOfferedFall) {
            fulfillsOffered = true;
        } else if (filters.contains(Filter.OFFERED_SPRING) && course.isOfferedSpring) {
            fulfillsOffered = true;
        } else if (filters.contains(Filter.OFFERED_IAP) && course.isOfferedIAP) {
            fulfillsOffered = true;
        }
        if (!fulfillsOffered) return false;

        boolean fulfillsLevel = false;
        if (filters.contains(Filter.LEVEL_NONE)) {
            fulfillsLevel = true;
        } else if (filters.contains(Filter.LEVEL_UG) && course.subjectLevel.equals("U")) {
            fulfillsLevel = true;
        } else if (filters.contains(Filter.LEVEL_G) && course.subjectLevel.equals("G")) {
            fulfillsLevel = true;
        }
        if (!fulfillsLevel) return false;

        boolean fulfillsAttendance = false;
        if (filters.contains(Filter.ATTENDANCE_NONE)) {
            fulfillsAttendance = true;
        } else if (filters.contains(Filter.ATTENDANCE_INPERSON) && (course.getVirtualStatus() == Course.VirtualStatus.INPERSON ||
                course.getVirtualStatus() == Course.VirtualStatus.HYBRID)) {
            fulfillsAttendance = true;
        } else if (filters.contains(Filter.ATTENDANCE_VIRTUAL) && (course.getVirtualStatus() == Course.VirtualStatus.VIRTUAL || course.getVirtualStatus() == Course.VirtualStatus.HYBRID)) {
            fulfillsAttendance = true;
        }
        if (!fulfillsAttendance) return false;

        return true;

    }

    private List<String> searchFieldsForCourse(Course course, EnumSet<Filter> mFilters) {
        List<String> searchFields = new ArrayList<>();

        EnumSet<Filter> filters = mFilters == null ? Filter.noFilter() : mFilters;

        if (filters.contains(Filter.SEARCH_ID))
            searchFields.add(course.getSubjectID().toLowerCase());
        if (course.subjectTitle != null && filters.contains(Filter.SEARCH_TITLE))
            searchFields.add(course.subjectTitle.toLowerCase());
        if (course.getInstructors() != null && filters.contains(Filter.SEARCH_INSTRUCTORS))
            searchFields.add(course.getInstructors().toLowerCase());
        if (course.prerequisites != null && filters.contains(Filter.SEARCH_PREREQS))
            searchFields.add(course.prerequisites.toLowerCase());
        if (course.corequisites != null && filters.contains(Filter.SEARCH_COREQS))
            searchFields.add(course.corequisites.toLowerCase());

        if (filters.contains(Filter.SEARCH_REQUIREMENTS)) {
            if (course.getGIRAttribute() != null) {
                searchFields.add(course.getGIRAttribute().toString().toLowerCase());
                searchFields.add(course.getGIRAttribute().rawValue.toLowerCase());
            }
            if (course.getCommunicationRequirement() != null) {
                searchFields.add(course.getCommunicationRequirement().descriptionText().toLowerCase());
                searchFields.add(course.getCommunicationRequirement().toString().toLowerCase());
            }
            List<Course.HASSAttribute> hasses = course.getHASSAttribute();
            if (hasses != null) {
                for (Course.HASSAttribute hass: hasses) {
                    searchFields.add(hass.descriptionText().toLowerCase());
                    searchFields.add(hass.toString().toLowerCase());
                }
            }
        }
        return searchFields;
    }

    private List<Course> sortedSearchResults(List<SearchItem> searchItems) {
        Collections.sort(searchItems, new Comparator<SearchItem>() {
            @Override
            public int compare(SearchItem t1, SearchItem t2) {
                if (t1.course.isHistorical != t2.course.isHistorical) {
                    return t1.course.isHistorical ? 1 : -1;
                } else if (t1.relevance != t2.relevance) {
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
    private static final int NUM_RECENT_COURSES = 15;
    private List<Course> recentCourses;

    public interface RecentCoursesCallback {
        void result(List<Course> courses);
    }

    public interface RecentCoursesListener {
        void changed(List<Course> courses);
    }

    private RecentCoursesListener recentsListener;
    public void setRecentCoursesChangedListener(RecentCoursesListener listener) {
        recentsListener = listener;
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
        if (recentsListener != null) {
            TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    recentsListener.changed(recentCourses);
                }
            });
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
