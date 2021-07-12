package com.redhat.cloud.policies.engine.history.alert.requests;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EventsRequest {

    @NotNull
    private String tenantId;

    private String eventType;

    private Long startTime;

    private Long endTime;

    private Set<String> eventIds;

    private Set<String> triggerIds;

    private Set<String> categories;

    private String tagQuery;

    private Boolean thin;

    private Integer criteriaNoQuerySize;

    private Integer pageNumber;

    private Integer pageSize;

    private List<String> orderBy = new ArrayList<>();

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Set<String> getEventIds() {
        return eventIds;
    }

    public void setEventIds(Set<String> eventIds) {
        this.eventIds = eventIds;
    }

    public Set<String> getTriggerIds() {
        return triggerIds;
    }

    public void setTriggerIds(Set<String> triggerIds) {
        this.triggerIds = triggerIds;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public String getTagQuery() {
        return tagQuery;
    }

    public void setTagQuery(String tagQuery) {
        this.tagQuery = tagQuery;
    }

    public Boolean getThin() {
        return thin;
    }

    public void setThin(Boolean thin) {
        this.thin = thin;
    }

    public Integer getCriteriaNoQuerySize() {
        return criteriaNoQuerySize;
    }

    public void setCriteriaNoQuerySize(Integer criteriaNoQuerySize) {
        this.criteriaNoQuerySize = criteriaNoQuerySize;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<String> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<String> orderBy) {
        this.orderBy = orderBy;
    }
}
