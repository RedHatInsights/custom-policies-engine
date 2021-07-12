package com.redhat.cloud.policies.engine.history.alert.requests;

import org.hawkular.alerts.api.model.Severity;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hawkular.alerts.api.model.event.Alert.Status;

public class AlertsRequest {

    @NotNull
    private String tenantId;

    private Long startTime;

    private Long endTime;

    private Long startResolvedTime;

    private Long endResolvedTime;

    private Long startAckTime;

    private Long endAckTime;

    private Long startStatusTime;

    private Long endStatusTime;

    private Set<String> alertIds;

    private Set<Status> statuses;

    private Set<Severity> severities;

    private Set<String> triggerIds;

    private String tagQuery;

    private String query;

    private Boolean thin;

    private String eventType;

    private Integer pageNumber;

    private Integer pageSize;

    private List<String> orderBy = new ArrayList<>();

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public Long getStartResolvedTime() {
        return startResolvedTime;
    }

    public void setStartResolvedTime(Long startResolvedTime) {
        this.startResolvedTime = startResolvedTime;
    }

    public Long getEndResolvedTime() {
        return endResolvedTime;
    }

    public void setEndResolvedTime(Long endResolvedTime) {
        this.endResolvedTime = endResolvedTime;
    }

    public Long getStartAckTime() {
        return startAckTime;
    }

    public void setStartAckTime(Long startAckTime) {
        this.startAckTime = startAckTime;
    }

    public Long getEndAckTime() {
        return endAckTime;
    }

    public void setEndAckTime(Long endAckTime) {
        this.endAckTime = endAckTime;
    }

    public Long getStartStatusTime() {
        return startStatusTime;
    }

    public void setStartStatusTime(Long startStatusTime) {
        this.startStatusTime = startStatusTime;
    }

    public Long getEndStatusTime() {
        return endStatusTime;
    }

    public void setEndStatusTime(Long endStatusTime) {
        this.endStatusTime = endStatusTime;
    }

    public Set<String> getAlertIds() {
        return alertIds;
    }

    public void setAlertIds(Set<String> alertIds) {
        this.alertIds = alertIds;
    }

    public Set<Status> getStatuses() {
        return statuses;
    }

    public void setStatuses(Set<Status> statuses) {
        this.statuses = statuses;
    }

    public Set<Severity> getSeverities() {
        return severities;
    }

    public void setSeverities(Set<Severity> severities) {
        this.severities = severities;
    }

    public Set<String> getTriggerIds() {
        return triggerIds;
    }

    public void setTriggerIds(Set<String> triggerIds) {
        this.triggerIds = triggerIds;
    }

    public String getTagQuery() {
        return tagQuery;
    }

    public void setTagQuery(String tagQuery) {
        this.tagQuery = tagQuery;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Boolean getThin() {
        return thin;
    }

    public void setThin(Boolean thin) {
        this.thin = thin;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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
