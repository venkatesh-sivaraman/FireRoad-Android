package com.base12innovations.android.fireroad.models.schedule;

import android.text.TextUtils;
import android.util.Log;

import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.utils.ListHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScheduleGenerator {

    Map<Course, Map<String, List<Integer>>> allowedSections;

    public boolean isSectionAllowed(Course course, String section, int sectionNumber) {
        if (allowedSections == null)
            return true;
        if (!allowedSections.containsKey(course))
            return true;
        if (!allowedSections.get(course).containsKey(section) ||
                allowedSections.get(course).get(section).size() == 0)
            return true;
        return allowedSections.get(course).get(section).contains(sectionNumber);
    }

    private List<List<Course.ScheduleItem>> allowedSectionsForCourse(Course course, String section, final List<List<Course.ScheduleItem>> allItems) {
        if (allowedSections == null ||
                !allowedSections.containsKey(course) ||
                !allowedSections.get(course).containsKey(section) ||
                allowedSections.get(course).get(section).size() == 0) {
            return allItems;
        }
        // If any of the section indexes are no longer available, pretend the course is unconstrained
        if (ListHelper.maximum(allowedSections.get(course).get(section), -1) >= allItems.size()) {
            return allItems;
        }
        return ListHelper.map(allowedSections.get(course).get(section),
                new ListHelper.Function<Integer, List<Course.ScheduleItem>>() {
                    @Override
                    public List<Course.ScheduleItem> apply(Integer integer) {
                        return allItems.get(integer);
                    }
                });
    }

    // Update the allowedSections list for the given course/section, resetting the sections if some of them are invalid
    private void updateAllowedSections(Course course, String section, List<List<Course.ScheduleItem>> allItems) {
        if (allowedSections == null ||
                !allowedSections.containsKey(course) ||
                !allowedSections.get(course).containsKey(section) ||
                allowedSections.get(course).get(section).size() == 0) {
            return;
        }
        // If any of the section indexes are no longer available, unconstrain the course
        if (ListHelper.maximum(allowedSections.get(course).get(section), -1) >= allItems.size()) {
            allowedSections.get(course).remove(section);
        }
    }

    public List<ScheduleConfiguration> generateSchedules(List<Course> courses, Map<Course, Map<String, List<Integer>>> sections) {
        allowedSections = sections;

        // Generate a list of ScheduleItem objects representing the possible schedule assignments
        // for each section of each course.
        List<List<ScheduleUnit>> schedConfigs = new ArrayList<>();
        List<ScheduleUnit> schedConfigsList = new ArrayList<>();

        for (Course course : courses) {
            Map<String, List<List<Course.ScheduleItem>>> schedule = course.getSchedule();
            if (schedule == null) continue;

            for (String section : schedule.keySet()) {
                updateAllowedSections(course, section, schedule.get(section));
                List<List<Course.ScheduleItem>> filteredOptions = allowedSectionsForCourse(course, section, schedule.get(section));

                // Filter out sections with the same exact days and times
                Map<String, List<Course.ScheduleItem>> uniqueTimeOptions = new HashMap<>();
                for (List<Course.ScheduleItem> option : filteredOptions) {
                    List<String> comps = new ArrayList<>();
                    for (Course.ScheduleItem item : option)
                        comps.add(item.toString(false));
                    String key = TextUtils.join(",", comps);
                    if (!uniqueTimeOptions.containsKey(key))
                        uniqueTimeOptions.put(key, option);
                }

                List<ScheduleUnit> allOptions = new ArrayList<>();
                for (String key : uniqueTimeOptions.keySet())
                    allOptions.add(new ScheduleUnit(course, section, uniqueTimeOptions.get(key)));
                if (allOptions.size() == 0)
                    Log.e("ScheduleGenerator", "No options for " + course.getSubjectID() + " " + section);

                if (section.equals(Course.ScheduleType.LECTURE))
                    schedConfigs.add(0, allOptions);
                else
                    schedConfigs.add(allOptions);
                schedConfigsList.addAll(allOptions);
            }
        }

        // Sort - performance improvement
        Collections.sort(schedConfigs, new Comparator<List<ScheduleUnit>>() {
            @Override
            public int compare(List<ScheduleUnit> t1, List<ScheduleUnit> t2) {
                return t1.size() != t2.size() ? (t1.size() < t2.size() ? -1 : 1) : 0;
            }
        });

        List<List<List<ScheduleUnit>>> conflictGroups = new ArrayList<>();
        for (int day : Course.ScheduleDay.ordering) {
            conflictGroups.add(new ArrayList<List<ScheduleUnit>>());
            for (int i = 0; i < ScheduleSlots.slots.size(); i++)
                conflictGroups.get(conflictGroups.size() - 1).add(new ArrayList<ScheduleUnit>());
        }
        Map<ScheduleUnit, List<Set<Integer>>> configConflictMapping = new HashMap<>();

        for (ScheduleUnit unit : schedConfigsList) {
            List<Set<Integer>> slotsOccupied = new ArrayList<>();
            for (int day : Course.ScheduleDay.ordering)
                slotsOccupied.add(new HashSet<Integer>());

            for (Course.ScheduleItem item : unit.scheduleItems) {
                int startSlot = ScheduleSlots.slotIndex(item.startTime);
                int endSlot = ScheduleSlots.slotIndex(item.endTime);
                for (int i = startSlot; i < endSlot; i++) {
                    if (i < 0 || i >= ScheduleSlots.slots.size()) continue;

                    for (int j = 0; j < Course.ScheduleDay.ordering.length; j++) {
                        if ((item.days & Course.ScheduleDay.ordering[j]) == 0) continue;
                        conflictGroups.get(j).get(i).add(unit);
                        slotsOccupied.get(j).add(i);
                    }
                }
            }

            configConflictMapping.put(unit, slotsOccupied);
        }

        List<ScheduleConfiguration> results = recursivelyGenerateScheduleConfigurations(
                schedConfigs, conflictGroups, configConflictMapping,
                new ArrayList<ScheduleUnit>(), 0, null
        );

        Collections.sort(results, new Comparator<ScheduleConfiguration>() {
            @Override
            public int compare(ScheduleConfiguration t1, ScheduleConfiguration t2) {
                return t1.conflictCount != t2.conflictCount ? (t1.conflictCount < t2.conflictCount ? -1 : 1) : 0;
            }
        });

        if (results.size() > 0) {
            int conflictThreshold = results.get(0).conflictCount;
            if (conflictThreshold != 0)
                conflictThreshold += 1;
            while (results.get(results.size() - 1).conflictCount > conflictThreshold)
                results.remove(results.size() - 1);
        }

        return results;
    }

    private class ScheduleConfigResult {
        int conflicts;
        int newConflicts;
        List<Set<Integer>> union;

        ScheduleConfigResult(int conflicts, int newConflicts, List<Set<Integer>> union) {
            this.conflicts = conflicts;
            this.newConflicts = newConflicts;
            this.union = union;
        }
    }

    private int intersectSize(Set<Integer> set1, Set<Integer> set2) {
        Set<Integer> cop = new HashSet<>(set1);
        cop.retainAll(set2);
        return cop.size();
    }

    private Set<Integer> getUnion(Set<Integer> set1, Set<Integer> set2) {
        Set<Integer> cop = new HashSet<>(set1);
        cop.addAll(set2);
        return cop;
    }

    /**
     - Parameters:
     * configurations: List of lists of schedule units. One schedule unit needs to
     * be chosen from each inner list.
     * conflictGroups: The schedule units occupied by each slot on each day.
     * conflictMapping: The numbers of the slots occupied by each schedule unit.
     * prefixSchedule: The list of schedule units generated so far.
     * conflictCount: The current number of conflicts in the schedule.
     * conflictingSlots: The slots which, if occupied by the next added configuration,
     will create an additional conflict.

     - Returns: A list of schedules generated.
     */
    private List<ScheduleConfiguration> recursivelyGenerateScheduleConfigurations(
        List<List<ScheduleUnit>> configurations,
        List<List<List<ScheduleUnit>>> conflictGroups,
        Map<ScheduleUnit, List<Set<Integer>>> conflictMapping,
        List<ScheduleUnit> prefixSchedule,
        int conflictCount,
        List<Set<Integer>> conflictingSlots
    ) {
        if (prefixSchedule.size() == configurations.size()) {
            // Done generating schedule
            return Arrays.asList(new ScheduleConfiguration(prefixSchedule, conflictCount));
        }

        List<Set<Integer>> mConflictingSlots = conflictingSlots;
        if (mConflictingSlots == null) {
            mConflictingSlots = new ArrayList<>();
            for (int day : Course.ScheduleDay.ordering)
                mConflictingSlots.add(new HashSet<Integer>());
        }

        // Vary the next configuration in the list
        List<ScheduleUnit> varyingConfigSet = configurations.get(prefixSchedule.size());
        List<ScheduleConfiguration> results = new ArrayList<>();

        List<ScheduleConfigResult> sets = new ArrayList<>();
        for (ScheduleUnit variation : varyingConfigSet) {
            if (!conflictMapping.containsKey(variation))
                sets.add(new ScheduleConfigResult(Integer.MAX_VALUE, Integer.MAX_VALUE, null));

            int allConflicts = 0, newConflictCount = 0;
            List<Set<Integer>> union = new ArrayList<>();
            List<Set<Integer>> slots = conflictMapping.get(variation);
            for (int i = 0; i < slots.size(); i++) {
                for (Integer val : slots.get(i)) {
                    allConflicts += conflictGroups.get(i).get(val).size();
                }
                newConflictCount += intersectSize(slots.get(i), mConflictingSlots.get(i));
                union.add(getUnion(slots.get(i), mConflictingSlots.get(i)));
            }

            sets.add(new ScheduleConfigResult(allConflicts, newConflictCount, union));
        }

        int minConflicts = 0;
        if (sets.size() != 0) {
            ScheduleConfigResult minConfig = ListHelper.minimum(sets, new Comparator<ScheduleConfigResult>() {
                @Override
                public int compare(ScheduleConfigResult t1, ScheduleConfigResult t2) {
                    return (t1.conflicts != t2.conflicts) ? (t1.conflicts < t2.conflicts ? -1 : 1) : 0;
                }
            }, null);
            if (minConfig != null)
                minConflicts = minConfig.conflicts;
        }

        // First, recurse on only the ones that have few conflicts
        for (int i = 0; i < varyingConfigSet.size(); i++) {
            ScheduleConfigResult tempResult = sets.get(i);
            if (tempResult.conflicts == minConflicts || tempResult.newConflicts == 0) {
                List<ScheduleUnit> newPrefix = new ArrayList<>(prefixSchedule);
                newPrefix.add(varyingConfigSet.get(i));
                results.addAll(recursivelyGenerateScheduleConfigurations(
                        configurations,
                        conflictGroups,
                        conflictMapping,
                        newPrefix,
                        conflictCount + tempResult.newConflicts,
                        tempResult.union
                ));
            }
        }

        if (results.size() == 0) {
            // If nothing was found, iterate again with the conflicting schedule items
            for (int i = 0; i < varyingConfigSet.size(); i++) {
                ScheduleConfigResult tempResult = sets.get(i);
                List<ScheduleUnit> newPrefix = new ArrayList<>(prefixSchedule);
                newPrefix.add(varyingConfigSet.get(i));
                results.addAll(recursivelyGenerateScheduleConfigurations(
                        configurations,
                        conflictGroups,
                        conflictMapping,
                        newPrefix,
                        conflictCount + tempResult.newConflicts,
                        tempResult.union
                ));
            }
        }

        return results;
    }
}
