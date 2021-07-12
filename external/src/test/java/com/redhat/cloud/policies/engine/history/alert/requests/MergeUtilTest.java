package com.redhat.cloud.policies.engine.history.alert.requests;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MergeUtilTest {

    @Test
    public void testMergeNullArgs() {
        Set<String> result = MergeUtil.merge(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeNonNullArgs() {
        Object singleElement = new Object();
        Set<Object> collection = Set.of(new Object(), new Object());
        Set<Object> result = MergeUtil.merge(singleElement, collection);
        assertEquals(collection.size() + 1, result.size());
        assertTrue(result.contains(singleElement));
        assertTrue(result.containsAll(collection));
    }

    @Test
    public void testMergeNullSingleElementNonNullCollection() {
        List<String> collection = List.of("hello", "world");
        Set<String> result = MergeUtil.merge(null, collection);
        assertEquals(collection.size(), result.size());
        assertTrue(result.containsAll(collection));
    }

    @Test
    public void testMergeNonNullSingleElementNullCollection() {
        Integer singleElement = Integer.valueOf("123");
        Set<Integer> result = MergeUtil.merge(singleElement, null);
        assertEquals(1, result.size());
        assertTrue(result.contains(singleElement));
    }
}
