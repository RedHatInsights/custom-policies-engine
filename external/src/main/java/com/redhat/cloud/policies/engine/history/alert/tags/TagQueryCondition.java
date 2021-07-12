package com.redhat.cloud.policies.engine.history.alert.tags;

public class TagQueryCondition {

    private String key;

    private TagQueryOperator operator;

    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public TagQueryOperator getOperator() {
        return operator;
    }

    public void setOperator(TagQueryOperator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
