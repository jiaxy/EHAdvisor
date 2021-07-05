package com.tcl.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class CollectionUtils {

    @Nonnull
    public static <T> T[] toArray(@Nonnull Iterable<T> iterable, @Nonnull Class<T> type) {
        var list = new ArrayList<T>();
        iterable.forEach(list::add);
        //noinspection unchecked
        return list.toArray((T[]) Array.newInstance(type, 0));
    }

    public static <T> T getFirst(@Nonnull Iterable<T> iterable) {
        for (var x : iterable) {
            return x;
        }
        throw new IllegalArgumentException("The iterable object is empty.");
    }

    public static <T> boolean notContainsNull(@Nullable Iterable<T> iterable) {
        if (iterable == null) {
            return false;
        }
        for (var x : iterable) {
            if (x == null) {
                return false;
            }
        }
        return true;
    }

    private CollectionUtils() {
    }
}
