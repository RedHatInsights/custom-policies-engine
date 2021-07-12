package com.redhat.cloud.policies.engine.history.alert.tags;

import java.util.Optional;

public enum TagQueryOperator {

    EQUAL("="),
    NOT_EQUAL("!="),
    LIKE("MATCHES");

    private String operator;

    TagQueryOperator(String operator) {
        this.operator = operator;
    }

    public static Optional<TagQueryOperator> fromString(String operator) {
        for (TagQueryOperator tagQueryOperator : TagQueryOperator.values()) {
            if (tagQueryOperator.operator.equals(operator)) {
                return Optional.of(tagQueryOperator);
            }
        }
        return Optional.empty();
    }
}
