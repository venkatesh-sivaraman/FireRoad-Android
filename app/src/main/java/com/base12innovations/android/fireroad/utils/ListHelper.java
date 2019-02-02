package com.base12innovations.android.fireroad.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ListHelper {

    public interface Function <T, E> {
        E apply(T elem);
    }

    public static <T, E> List<E> map(Collection<T> input, Function<T, E> function) {
        List<E> output = new ArrayList<>();
        for (T obj : input) {
            output.add(function.apply(obj));
        }
        return output;
    }

    public static <T, E> List<E> compactMap(Collection<T> input, Function<T, E> function) {
        List<E> output = new ArrayList<>();
        for (T obj : input) {
            E newObj = function.apply(obj);
            if (newObj != null)
                output.add(newObj);
        }
        return output;
    }

    public interface Predicate<T> {
        boolean test(T element);
    }

    public static final int NOT_FOUND = Integer.MAX_VALUE;

    public static <T> void filterInPlace(Collection<T> input, Predicate<T> predicate) {
        Iterator<T> it = input.iterator();
        while (it.hasNext()) {
            T elem = it.next();
            if (!predicate.test(elem))
                it.remove();
        }
    }

    public static <T> List<T> filter(Collection<T> input, Predicate<T> predicate) {
        List<T> output = new ArrayList<>();
        for (T elem : input) {
            if (predicate.test(elem))
                output.add(elem);
        }
        return output;
    }

    public static <T> boolean containsElement(Collection<T> input, Predicate<T> predicate) {
        for (T elem : input) {
            if (predicate.test(elem))
                return true;
        }
        return false;
    }

    public static <T> int indexOfElement(List<T> input, Predicate<T> predicate) {
        for (int i = 0; i < input.size(); i++) {
            if (predicate.test(input.get(i)))
                return i;
        }
        return NOT_FOUND;
    }

    public interface Reducer<T, E> {
        E concat(E running, T elem);
    }

    public static <T, E> E reduce(Collection<T> input, E initialValue, Reducer<T, E> reducer) {
        E runningResult = initialValue;
        for (T elem : input) {
            runningResult = reducer.concat(runningResult, elem);
        }
        return runningResult;
    }

    public static <T> T minimum(Collection<T> input, Comparator<T> comparator, T defaultValue) {
        T currentMin = null;
        for (T elem : input) {
            if (currentMin == null || comparator.compare(elem, currentMin) < 0)
                currentMin = elem;
        }
        if (currentMin == null && defaultValue != null)
            return defaultValue;
        return currentMin;
    }

    public static <T> T maximum(Collection<T> input, Comparator<T> comparator, T defaultValue) {
        T currentMax = null;
        for (T elem : input) {
            if (currentMax == null || comparator.compare(elem, currentMax) > 0)
                currentMax = elem;
        }
        if (currentMax == null && defaultValue != null)
            return defaultValue;
        return currentMax;
    }

    public static <T extends Comparable<T>> T minimum(Collection<T> input, T defaultValue) {
        T currentMin = null;
        for (T elem : input) {
            if (currentMin == null || elem.compareTo(currentMin) < 0)
                currentMin = elem;
        }
        if (currentMin == null && defaultValue != null)
            return defaultValue;
        return currentMin;
    }

    public static <T extends Comparable<T>> T maximum(Collection<T> input, T defaultValue) {
        T currentMax = null;
        for (T elem : input) {
            if (currentMax == null || elem.compareTo(currentMax) > 0)
                currentMax = elem;
        }
        if (currentMax == null && defaultValue != null)
            return defaultValue;
        return currentMax;
    }

    // Reducers

    public static class IntegerSum implements Reducer<Integer, Integer> {
        @Override
        public Integer concat(Integer running, Integer elem) { return running + elem; }
    }

    public static class FloatSum implements Reducer<Float, Float> {
        @Override
        public Float concat(Float running, Float elem) { return running + elem; }
    }

    public static class DoubleSum implements Reducer<Double, Double> {
        @Override
        public Double concat(Double running, Double elem) { return running + elem; }
    }
}
