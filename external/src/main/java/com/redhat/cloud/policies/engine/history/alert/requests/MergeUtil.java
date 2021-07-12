package com.redhat.cloud.policies.engine.history.alert.requests;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MergeUtil {

    public static <T> Set<T> merge(T singleElement, Collection<T> collection) {
        if (singleElement == null && (collection == null || collection.isEmpty())) {
            return Collections.emptySet();
        } else {
            Set<T> result = new HashSet<>();
            if (singleElement != null) {
                result.add(singleElement);
            }
            if (collection != null) {
                result.addAll(collection);
            }
            return result;
        }
    }
}
