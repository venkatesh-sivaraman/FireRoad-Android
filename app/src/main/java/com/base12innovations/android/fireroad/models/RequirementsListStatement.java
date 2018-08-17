package com.base12innovations.android.fireroad.models;

import android.arch.persistence.room.util.StringUtil;
import android.text.TextUtils;
import android.util.Log;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RequirementsListStatement {
    public String title;
    public String contentDescription;

    public WeakReference<RequirementsListStatement> parent;

    public enum ConnectionType {
        ALL, ANY, NONE
    }

    public enum ThresholdType {
        LESS_THAN_OR_EQUAL, LESS_THAN, GREATER_THAN, GREATER_THAN_OR_EQUAL
    }

    public enum ThresholdCriterion {
        SUBJECTS, UNITS
    }

    protected static String allSeparator = ",";
    protected static String anySeparator = "/";
    protected static String commentCharacter = "%%";
    protected static String declarationCharacter = ":=";
    protected static String variableDeclarationSeparator = ",";
    protected static String headerSeparator = "#,#";

    protected static String thresholdParameter = "threshold=";
    protected static String urlParameter = "url=";

    static int DEFAULT_UNIT_COUNT = 12;

    public class Threshold {
        ThresholdType type;
        int cutoff;
        ThresholdCriterion criterion;

        Threshold(ThresholdType type, int number, ThresholdCriterion criterion) {
            this.type = type;
            this.cutoff = number;
            this.criterion = criterion;
        }

        int cutoffForCriterion(ThresholdCriterion criterion) {
            int co;
            if (this.criterion == criterion) {
                co = cutoff;
            } else if (this.criterion == ThresholdCriterion.SUBJECTS) {
                co = cutoff * DEFAULT_UNIT_COUNT;
            } else {
                co = cutoff / DEFAULT_UNIT_COUNT;
            }
            return co;
        }

        int getActualCutoff() {
            if (type == ThresholdType.GREATER_THAN) {
                return cutoff + 1;
            } else if (type == ThresholdType.LESS_THAN) {
                return cutoff - 1;
            }
            return cutoff;
        }
    }

    public ConnectionType connectionType = ConnectionType.ALL;

    private List<RequirementsListStatement> requirements;
    public List<RequirementsListStatement> getRequirements() {
        return requirements;
    }
    protected void setRequirements(List<RequirementsListStatement> newValue) {
        requirements = newValue;
        if (requirements != null) {
            for (RequirementsListStatement req : requirements) {
                req.parent = new WeakReference<>(this);
            }
        }
    }

    public String requirement;

    public Threshold threshold;

    public boolean isPlainString = false;

    /**
     Defines the bound on the number of distinct elements in the requirements list
     that courses must satisfy.
     */
    public Threshold distinctThreshold;

    public String getThresholdDescription() {
        String ret = "";
        if (threshold != null && threshold.cutoff != 1) {
            if (threshold.cutoff > 1) {
                switch (threshold.type) {
                    case LESS_THAN_OR_EQUAL:
                        ret = "select at most " + Integer.toString(threshold.cutoff);
                        break;
                    case LESS_THAN:
                        ret = "select at most " + Integer.toString(threshold.cutoff - 1);
                        break;
                    case GREATER_THAN_OR_EQUAL:
                        ret = "select any " + Integer.toString(threshold.cutoff);
                        break;
                    case GREATER_THAN:
                        ret = "select any " + Integer.toString(threshold.cutoff + 1);
                        break;
                }
                if (threshold.criterion == ThresholdCriterion.UNITS) {
                    ret += " units";
                } else if (threshold.criterion == ThresholdCriterion.SUBJECTS && connectionType == ConnectionType.ALL) {
                    ret += " subjects";
                }
            } else if (threshold.cutoff == 0 && connectionType == ConnectionType.ANY) {
                ret = "optional – select any";
            }
        } else if (connectionType == ConnectionType.ALL) {
            ret = "select all";
        } else if (connectionType == ConnectionType.ANY) {
            if (requirements != null && requirements.size() == 2) {
                ret = "select either";
            } else {
                ret = "select any";
            }
        }

        if (distinctThreshold != null && distinctThreshold.cutoff > 0) {
            String categoryText;
            switch (distinctThreshold.type) {
                case LESS_THAN_OR_EQUAL:
                    categoryText = (distinctThreshold.cutoff != 1) ? "categories" : "category";
                    ret += " from at most " + Integer.toString(distinctThreshold.cutoff) + " " + categoryText;
                    break;
                case LESS_THAN:
                    categoryText = (distinctThreshold.cutoff + 1 != 1) ? "categories" : "category";
                    ret += " from at most " + Integer.toString(distinctThreshold.cutoff - 1) + " " + categoryText;
                    break;
                case GREATER_THAN_OR_EQUAL:
                    categoryText = (distinctThreshold.cutoff != 1) ? "categories" : "category";
                    ret += " from at least " + Integer.toString(distinctThreshold.cutoff) + " " + categoryText;
                    break;
                case GREATER_THAN:
                    categoryText = (distinctThreshold.cutoff + 1 != 1) ? "categories" : "category";
                    ret += " from at least " + Integer.toString(distinctThreshold.cutoff + 1) + " " + categoryText;
                    break;
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        if (requirement != null) {
            String thresh = getThresholdDescription();
            return (title != null ? title + ": " : "") + requirement + (thresh.length() > 0 ? " (" + thresh + ")" : "");
        } else if (requirements != null) {
            String thresh = getThresholdDescription();
            StringBuilder builder = new StringBuilder();
            builder.append((title != null ? title + ": " : "") + (thresh.length() > 0 ? " (" + thresh + ")" : "") + "\n");
            for (RequirementsListStatement stmt : requirements) {
                builder.append(stmt.toString());
                builder.append("\n");
            }
            return builder.toString();
        }
        return super.toString();
    }

    public String getShortDescription() {
        String baseString = "";
        if (requirement != null) {
            baseString = requirement;
        } else if (requirements != null) {
            String connectionWord = connectionType == ConnectionType.ALL ? " and " : " or ";
            if (requirements.size() == 2) {
                baseString = requirements.get(0).getShortDescription() + connectionWord + requirements.get(1).getShortDescription();
            } else {
                baseString = requirements.get(0).getShortDescription() + connectionWord + Integer.toString(requirements.size() - 1) + " others";
            }
        }
        return baseString;
    }

    /// Gives the minimum number of steps needed to traverse the tree down to a leaf (an individual course).
    public int minimumNestDepth() {
        if (requirements != null) {
            OptionalInt result = requirements.stream().mapToInt(new ToIntFunction<RequirementsListStatement>() {
                @Override
                public int applyAsInt(RequirementsListStatement req) {
                    return req.minimumNestDepth();
                }
            }).min();
            return (result.orElse(-1)) + 1;
        }
        return 0;
    }

    /// Gives the maximum number of steps needed to traverse the tree down to a leaf (an individual course).
    public int maximumNestDepth() {
        if (requirements != null) {
            OptionalInt result = requirements.stream().mapToInt(new ToIntFunction<RequirementsListStatement>() {
                @Override
                public int applyAsInt(RequirementsListStatement req) {
                    return req.maximumNestDepth();
                }
            }).max();
            return (result.orElse(-1)) + 1;
        }
        return 0;
    }

    public RequirementsListStatement() { }

    public RequirementsListStatement(ConnectionType connection, List<RequirementsListStatement> items, String title) {
        this.title = title;
        this.connectionType = connection;
        this.setRequirements(items);
    }

    public RequirementsListStatement(String requirement, String title) {
        this.title = title;
        this.requirement = requirement;
    }

    public static RequirementsListStatement fromStatement(String statement, String title) {
        RequirementsListStatement req = new RequirementsListStatement();
        req.title = title;
        req.parseStatement(statement);
        return req;
    }

    private class TopLevelItemsResult {
        ConnectionType connectionType;
        List<String> items;

        TopLevelItemsResult(ConnectionType type, List<String> items) {
            this.connectionType = type;
            this.items = items;
        }
    }

    private TopLevelItemsResult separateTopLevelItems(String text) {
        String trimmed = text.trim();
        if (trimmed.length() >= 4 && trimmed.substring(0, 2).equals("\"\"") && trimmed.substring(trimmed.length() - 2).equals("\"\"")) {
            List<String> items = new ArrayList<>();
            items.add(undecoratedComponent(trimmed));
            return new TopLevelItemsResult(ConnectionType.NONE, items);
        }

        List<String> components = new ArrayList<>();
        ConnectionType connectionType = ConnectionType.ALL;
        int currentIndentLevel = 0;
        for (int i = 0; i < text.length(); i++) {
            String character = text.substring(i, i + 1);
            if (character.equals(allSeparator) && currentIndentLevel == 0) {
                connectionType = ConnectionType.ALL;
                components.add("");
            } else if (character.equals(anySeparator) && currentIndentLevel == 0) {
                connectionType = ConnectionType.ANY;
                components.add("");
            } else {
                if (character.equals("("))
                    currentIndentLevel += 1;
                else if (character.equals(")"))
                    currentIndentLevel -= 1;
                if (components.size() == 0)
                    components.add("");
                components.set(components.size() - 1, components.get(components.size() - 1) + character);
            }
        }

        for (int i = 0; i < components.size(); i++) {
            components.set(i, undecoratedComponent(components.get(i)));
        }

        return new TopLevelItemsResult(connectionType, components);
    }

    /*fileprivate func topLevelSeparatorRegex(for separator: String) -> NSRegularExpression {
        let sepPattern = NSRegularExpression.escapedPattern(for: separator)
        guard let regex = try? NSRegularExpression(pattern: "\(sepPattern)(?![^\\(]*\\))", options: []) else {
            fatalError("Couldn't initialize top level separator regex")
        }
        return regex
    }*/

    private Pattern getModifierRegex() {
        return Pattern.compile("\\{(.*?)\\}(?![^(]*\\))");
    }

    protected String undecoratedComponent(String component) {
        return component.replaceAll("^([\"' ]+)|([\"' ]+)$", "");
    }

    protected String unwrappedComponent(String component) {
        String unwrapping = component.trim();
        while (unwrapping.charAt(0) == '(' && unwrapping.charAt(unwrapping.length() - 1) == ')') {
            unwrapping = unwrapping.substring(1, unwrapping.length() - 1);
        }
        return unwrapping;
    }

    protected List<String> undecoratedSplit(String text, String regex) {
        String[] compsTemp = text.split(regex);
        List<String> comps = new ArrayList<>();
        for (String comp : compsTemp) {
            comps.add(undecoratedComponent(comp));
        }
        return comps;
    }

    private List<String> componentsSeparatedByRegex(String string, Pattern regex) {
        Matcher matcher = regex.matcher(string);
        List<String> comps = new ArrayList<>();
        int lastLocation = 0;
        while (matcher.find()) {
            comps.add(undecoratedComponent(matcher.group(0)));
            lastLocation = matcher.end();
        }
        if (lastLocation < string.length())
            comps.add(undecoratedComponent(string.substring(lastLocation)));
        return comps;
    }

    private Threshold parseModifierComponent(String modifier) {
        Threshold threshold = new Threshold(ThresholdType.GREATER_THAN_OR_EQUAL, 1, ThresholdCriterion.SUBJECTS);
        if (modifier.contains(">="))
            threshold.type = ThresholdType.GREATER_THAN_OR_EQUAL;
        else if (modifier.contains("<="))
            threshold.type = ThresholdType.LESS_THAN_OR_EQUAL;
        else if (modifier.contains(">"))
            threshold.type = ThresholdType.GREATER_THAN;
        else if (modifier.contains("<"))
            threshold.type = ThresholdType.LESS_THAN;

        String numberString = modifier.replaceAll("[><=]", "");
        if (numberString.contains("u")) {
            threshold.criterion = ThresholdCriterion.UNITS;
            numberString = numberString.replaceAll("u", "");
        }

        threshold.cutoff = Integer.parseInt(numberString);
        return threshold;
    }


    private void parseModifier(String modifier) {
        if (modifier.contains("|")) {
            String[] comps = modifier.split("\\|");
            if (comps.length != 2) {
                Log.e("RequirementsListStatement", "Unsupported number of components in modifier string: " + modifier);
                return;
            }
            if (comps[0].length() > 0) {
                threshold = parseModifierComponent(comps[0]);
            }
            if (comps[1].length() > 0) {
                distinctThreshold = parseModifierComponent(comps[1]);
            }
        } else if (modifier.length() > 0) {
            threshold = parseModifierComponent(modifier);
        }
    }

    public void parseStatement(String statement) {
        String filteredStatement = statement;
        Matcher modifierMatcher = getModifierRegex().matcher(filteredStatement);
        if (modifierMatcher.find()) {
            parseModifier(modifierMatcher.group(1));
            filteredStatement = filteredStatement.replaceAll(getModifierRegex().pattern(), "");
        }

        TopLevelItemsResult topLevelItems = separateTopLevelItems(filteredStatement);
        if (threshold != null && threshold.cutoff != 0 && threshold.type == ThresholdType.GREATER_THAN_OR_EQUAL) {
            // Force the connection type to be any (there's no way it can be all)
            connectionType = ConnectionType.ANY;
        } else {
            connectionType = topLevelItems.connectionType;
        }
        isPlainString = (connectionType == ConnectionType.NONE);

        if (topLevelItems.items.size() == 1) {
            requirement = topLevelItems.items.get(0);
        } else {
            List<RequirementsListStatement> reqs = new ArrayList<>();
            for (String item : topLevelItems.items) {
                reqs.add(RequirementsListStatement.fromStatement(unwrappedComponent(item), null));
            }
            setRequirements(reqs);
        }
    }

    public void substituteVariableDefinitions(Map<String, RequirementsListStatement> dictionary) {
        if (requirement != null) {
            if (dictionary.containsKey(requirement)) {
                // Turns out this requirement is a variable
                RequirementsListStatement subReq = dictionary.get(requirement);
                subReq.substituteVariableDefinitions(dictionary);
                requirements = new ArrayList<>(Arrays.asList(subReq));
            }
        } else if (requirements != null) {
            List<RequirementsListStatement> newRequirements = new ArrayList<>();
            for (RequirementsListStatement statement : requirements) {
                if (statement.requirement != null && dictionary.containsKey(statement.requirement)) {
                    RequirementsListStatement subReq = dictionary.get(statement.requirement);
                    subReq.substituteVariableDefinitions(dictionary);
                    newRequirements.add(subReq);
                } else {
                    statement.substituteVariableDefinitions(dictionary);
                    newRequirements.add(statement);
                }
            }
            setRequirements(newRequirements);
        }
    }

    public String keyPath() {
        if (parent.get() == null || parent.get().keyPath() == null) {
            return null;
        }
        RequirementsListStatement mParent = parent.get();
        return mParent.keyPath() + "." + Integer.toString(mParent.requirements.indexOf(this));
    }

    // Computing requirements status

    public class FulfillmentProgress {
        int progress;
        int max;
        FulfillmentProgress(int p, int m) {
            progress = p;
            max = m;
        }
        public int getProgress() { return progress; }
        public int getMax() { return max; }
    }

    private boolean isFulfilled = false;
    public boolean isFulfilled() { return isFulfilled; }

    private FulfillmentProgress fulfillmentProgress;
    private FulfillmentProgress subjectProgress;
    private FulfillmentProgress unitProgress;
    public FulfillmentProgress getFulfillmentProgress() { return fulfillmentProgress; }
    public FulfillmentProgress getSubjectProgress() { return subjectProgress; }
    public FulfillmentProgress getUnitProgress() { return unitProgress; }

    public List<Course> coursesSatisfyingRequirement(final List<Course> courses) {
        if (requirement != null) {
            return (List<Course>)(Object)(courses.stream().filter(new Predicate<Course>() {
                @Override
                public boolean test(Course course) {
                    return course.satisfiesRequirement(requirement, courses);
                }
            }).collect(Collectors.toList()));
        }
        return new ArrayList<>();
    }

    private boolean numberSatisfiesThreshold(int number, int units, Threshold threshold) {
        int criterion = threshold.criterion == ThresholdCriterion.UNITS ? units : number;
        switch (threshold.type) {
            case GREATER_THAN:
                return criterion > threshold.cutoff;
            case GREATER_THAN_OR_EQUAL:
                return criterion >= threshold.cutoff;
            case LESS_THAN:
                return criterion < threshold.cutoff;
            case LESS_THAN_OR_EQUAL:
                return criterion <= threshold.cutoff;
        }
        return false;
    }

    private FulfillmentProgress ceilingThreshold(int progress, int max) {
        return new FulfillmentProgress(Math.min(Math.max(0, progress), max), max);
    }

    private int totalUnitsInCourses(Collection<Course> courses) {
        return courses.stream().mapToInt(new ToIntFunction<Course>() {
            @Override
            public int applyAsInt(Course course) {
                return course.totalUnits;
            }
        }).sum();
    }

    private FulfillmentProgress sumFulfillmentProgresses(List<RequirementsListStatement> reqs, final ThresholdCriterion crit, final ToIntFunction<Integer> maxFunction) {
        return reqs.stream().map(new Function<RequirementsListStatement, FulfillmentProgress>() {
            @Override
            public FulfillmentProgress apply(RequirementsListStatement req) {
                if (crit == ThresholdCriterion.SUBJECTS)
                    return req.subjectProgress;
                return req.unitProgress;
            }
        }).reduce(new FulfillmentProgress(0, 0), new BinaryOperator<FulfillmentProgress>() {
            @Override
            public FulfillmentProgress apply(FulfillmentProgress p1, FulfillmentProgress p2) {
                if (p2 == null) return p1;
                if (maxFunction != null)
                    return new FulfillmentProgress(p1.progress + p2.progress, p1.max + maxFunction.applyAsInt(p2.max));
                return new FulfillmentProgress(p1.progress + p2.progress, p1.max + p2.max);
            }
        });
    }

    private int thresholdCutoff(Threshold t, ThresholdCriterion criterion) {
        if (criterion == ThresholdCriterion.SUBJECTS)
            return t.cutoff / (t.criterion == ThresholdCriterion.UNITS ? DEFAULT_UNIT_COUNT : 1);
        return t.cutoff * (t.criterion == ThresholdCriterion.SUBJECTS ? DEFAULT_UNIT_COUNT : 1);
    }

    /**
     * This is a large and complicated method that provides considerable information about the
     * requirement's fulfillment status. It sets the `isFulfilled` property, which indicates whether
     * the requirement is complete. It also sets the `subjectProgress` and `unitProgress` properties,
     * which indicate the progress toward completion. The percentageFulfilled() value is computed
     * using these quantities.
     *
     - Returns: The set of courses that satisfy this requirement.
     */
    public Set<Course> computeRequirementStatus(List<Course> courses) {
        if (requirement != null) {
            // It's a basic requirement
            Set<Course> satisfiedCourses = new HashSet<>();
            if (isPlainString && manualProgress != 0 && threshold != null) {
                // Use manual progress
                isFulfilled = manualProgress == threshold.cutoff;
                int subjects = 0, units = 0;
                if (threshold.criterion == ThresholdCriterion.UNITS) {
                    units = manualProgress;
                    subjects = manualProgress / DEFAULT_UNIT_COUNT;
                } else {
                    units = manualProgress * DEFAULT_UNIT_COUNT;
                    subjects = manualProgress;
                }
                subjectProgress = ceilingThreshold(subjects, thresholdCutoff(threshold, ThresholdCriterion.SUBJECTS));
                unitProgress = ceilingThreshold(units, thresholdCutoff(threshold, ThresholdCriterion.UNITS));

                // Fill with dummy courses
                Random rng = new Random();
                for (int i = 0; i < subjectProgress.progress; i++) {
                    int id = rng.nextInt();
                    Course dummy = new Course();
                    dummy.setSubjectID("generatedCourse" + Integer.toString(id));
                    dummy.subjectTitle = "generatedCourse" + Integer.toString(id);
                    satisfiedCourses.add(dummy);
                }
            } else {
                // Example: requirement CI-H, we want to show how many have been fulfilled
                satisfiedCourses.addAll(coursesSatisfyingRequirement(courses));
                if (threshold != null) {
                    // A specific number of courses is required
                    subjectProgress = ceilingThreshold(satisfiedCourses.size(), thresholdCutoff(threshold, ThresholdCriterion.SUBJECTS));
                    unitProgress = ceilingThreshold(totalUnitsInCourses(satisfiedCourses), thresholdCutoff(threshold, ThresholdCriterion.UNITS));
                    isFulfilled = numberSatisfiesThreshold(subjectProgress.progress, unitProgress.progress, threshold);
                } else {
                    // Only one is needed
                    int progress = Math.min(satisfiedCourses.size(), 1);
                    isFulfilled = satisfiedCourses.size() > 0;
                    subjectProgress = ceilingThreshold(progress, 1);
                    if (satisfiedCourses.size() > 0)
                        unitProgress = ceilingThreshold(satisfiedCourses.iterator().next().totalUnits, DEFAULT_UNIT_COUNT);
                    else
                        unitProgress = ceilingThreshold(0, DEFAULT_UNIT_COUNT);
                }
            }
            fulfillmentProgress = (threshold != null && threshold.criterion == ThresholdCriterion.UNITS) ? unitProgress : subjectProgress;
            return satisfiedCourses;
        }

        if (requirements == null) return new HashSet<>();
        // It's a compound requirement

        Map<RequirementsListStatement, Set<Course>> satByCategory = new HashMap<>();
        Set<Course> totalSat = new HashSet<>();
        int numReqsSatisfied = 0;
        for (RequirementsListStatement req : requirements) {
            Set<Course> sat = req.computeRequirementStatus(courses);
            if (req.isFulfilled)
                numReqsSatisfied += 1;
            satByCategory.put(req, sat);
            totalSat.addAll(sat);
        }
        List<RequirementsListStatement> sortedProgresses = new ArrayList<>(requirements);
        sortedProgresses.sort(new Comparator<RequirementsListStatement>() {
            @Override
            public int compare(RequirementsListStatement t1, RequirementsListStatement t2) {
                float p1 = t1.percentageFulfilled(), p2 = t2.percentageFulfilled();
                return -Float.compare(p1, p2);
            }
        });

        if (threshold == null && distinctThreshold == null) {
            isFulfilled = (numReqsSatisfied > 0);
            if (connectionType == ConnectionType.ANY) {
                // Simple "any" statement
                if (sortedProgresses.size() > 0) {
                    subjectProgress = sortedProgresses.get(0).subjectProgress;
                    unitProgress = sortedProgresses.get(0).unitProgress;
                } else {
                    subjectProgress = new FulfillmentProgress(0, 0);
                    unitProgress = new FulfillmentProgress(0, 0);
                }
            } else {
                // "All" statement, will be finalized later
                subjectProgress = sumFulfillmentProgresses(sortedProgresses, ThresholdCriterion.SUBJECTS, null);
                unitProgress = sumFulfillmentProgresses(sortedProgresses, ThresholdCriterion.UNITS, null);
            }
        } else {
            if (distinctThreshold != null) {
                // Clip the progresses to the ones which the user is closest to completing
                sortedProgresses = sortedProgresses.subList(0, Math.min(distinctThreshold.getActualCutoff(), sortedProgresses.size()));
                totalSat = new HashSet<>();
                for (RequirementsListStatement req : sortedProgresses) {
                    totalSat.addAll(satByCategory.get(req));
                }
            }

            if (threshold == null && distinctThreshold != null) {
                // required number of statements
                if (distinctThreshold.type == ThresholdType.GREATER_THAN_OR_EQUAL ||
                        distinctThreshold.type == ThresholdType.GREATER_THAN)
                    isFulfilled = numReqsSatisfied >= distinctThreshold.getActualCutoff();
                else
                    isFulfilled = true;
                subjectProgress = sumFulfillmentProgresses(sortedProgresses, ThresholdCriterion.SUBJECTS, new ToIntFunction<Integer>() {
                    @Override
                    public int applyAsInt(Integer integer) {
                        return Math.max(integer, 1);
                    }
                });
                unitProgress = sumFulfillmentProgresses(sortedProgresses, ThresholdCriterion.UNITS, new ToIntFunction<Integer>() {
                    @Override
                    public int applyAsInt(Integer integer) {
                        return integer == 0 ? DEFAULT_UNIT_COUNT : integer;
                    }
                });
            } else if (threshold != null) {
                // required number of subjects or units
                subjectProgress = new FulfillmentProgress(totalSat.size(), thresholdCutoff(threshold, ThresholdCriterion.SUBJECTS));
                unitProgress = new FulfillmentProgress(totalUnitsInCourses(totalSat), thresholdCutoff(threshold, ThresholdCriterion.UNITS));

                if (distinctThreshold != null &&
                        (distinctThreshold.type == ThresholdType.GREATER_THAN ||
                                distinctThreshold.type == ThresholdType.GREATER_THAN_OR_EQUAL)) {
                    isFulfilled = numberSatisfiesThreshold(subjectProgress.progress, unitProgress.progress, threshold) && numReqsSatisfied >= distinctThreshold.getActualCutoff();
                    if (numReqsSatisfied < distinctThreshold.getActualCutoff()) {
                        subjectProgress = sumFulfillmentProgresses(sortedProgresses, ThresholdCriterion.SUBJECTS, new ToIntFunction<Integer>() {
                            @Override
                            public int applyAsInt(Integer integer) {
                                return Math.max(integer, 1);
                            }
                        });
                        unitProgress = sumFulfillmentProgresses(sortedProgresses, ThresholdCriterion.UNITS, new ToIntFunction<Integer>() {
                            @Override
                            public int applyAsInt(Integer integer) {
                                return integer == 0 ? DEFAULT_UNIT_COUNT : integer;
                            }
                        });
                    }
                } else {
                    isFulfilled = numberSatisfiesThreshold(subjectProgress.progress, unitProgress.progress, threshold);
                }
            }
        }

        if (connectionType == ConnectionType.ALL) {
            // "all" statement - make above progresses more stringent
            isFulfilled = isFulfilled && (numReqsSatisfied == requirements.size());
            if (subjectProgress.progress == subjectProgress.max && requirements.size() > numReqsSatisfied) {
                // Not satisfied, but subject progress makes it look satisfied, so lower the progress
                subjectProgress.max += requirements.size() - numReqsSatisfied;
                unitProgress.max += (requirements.size() - numReqsSatisfied) * DEFAULT_UNIT_COUNT;
            }
        }

        // Polish up values
        subjectProgress = ceilingThreshold(subjectProgress.progress, subjectProgress.max);
        unitProgress = ceilingThreshold(unitProgress.progress, unitProgress.max);
        fulfillmentProgress = (threshold != null && threshold.criterion == ThresholdCriterion.UNITS) ? unitProgress : subjectProgress;

        return totalSat;
    }

    public float percentageFulfilled() {
        if ((connectionType == ConnectionType.NONE && getManualProgress() == 0) || fulfillmentProgress == null)
            return 0.0f;
        if (fulfillmentProgress.progress == 0 && fulfillmentProgress.max == 0)
            return 0.0f;
        return Math.min(1.0f, (float)fulfillmentProgress.progress / (float)fulfillmentProgress.max) * 100.0f;
    }

    private int manualProgress = 0;
    public int getManualProgress() {
        return manualProgress;
    }
    public void setManualProgress(int newValue) {
        manualProgress = newValue;
    }
}
