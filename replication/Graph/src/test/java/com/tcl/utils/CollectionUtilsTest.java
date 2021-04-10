package com.tcl.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CollectionUtilsTest {

    @Test
    public void toArray1() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = CollectionUtils.toArray(list, String.class);
        assertArrayEquals(list.toArray(new String[0]), array);
    }

    @Test
    public void toArray2() {
        List<String> list = new ArrayList<>();
        String[] array = CollectionUtils.toArray(list, String.class);
        assertArrayEquals(list.toArray(new String[0]), array);
    }

    @Test
    public void getFirst() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CollectionUtils.getFirst(list));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFirst2() {
        var list = new ArrayList<>();
        CollectionUtils.getFirst(list);
    }
}