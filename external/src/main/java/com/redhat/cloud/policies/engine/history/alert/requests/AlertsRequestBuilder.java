package com.redhat.cloud.policies.engine.history.alert.requests;

import org.hawkular.alerts.api.model.Severity;
import org.hawkular.alerts.api.model.paging.Order;
import org.hawkular.alerts.api.model.paging.Pager;
import org.hawkular.alerts.api.services.AlertsCriteria;

import java.util.Set;

import static org.hawkular.alerts.api.model.event.Alert.Status;

public class AlertsRequestBuilder {

    public static AlertsRequest build(String tenantId, AlertsCriteria criteria) {
        return build(tenantId, criteria, null);
    }

    public static AlertsRequest build(String tenantId, AlertsCriteria criteria, Pager pager) {
        AlertsRequest request = new AlertsRequest();
        request.setTenantId(tenantId);

        if (criteria != null && criteria.hasCriteria()) {

            if (criteria.hasCTimeCriteria()) {
                request.setStartTime(criteria.getStartTime());
                request.setEndTime(criteria.getEndTime());
            }

            if (criteria.hasResolvedTimeCriteria()) {
                request.setStartResolvedTime(criteria.getStartResolvedTime());
                request.setEndResolvedTime(criteria.getEndResolvedTime());
            }

            if (criteria.hasAckTimeCriteria()) {
                request.setStartAckTime(criteria.getStartAckTime());
                request.setEndAckTime(criteria.getEndAckTime());
            }

            if (criteria.hasStatusTimeCriteria()) {
                request.setStartStatusTime(criteria.getStartStatusTime());
                request.setEndStatusTime(criteria.getEndStatusTime());
            }

            if (criteria.hasAlertIdCriteria()) {
                Set<String> alertIds = MergeUtil.merge(criteria.getAlertId(), criteria.getAlertIds());
                request.setAlertIds(alertIds);
            }

            if (criteria.hasStatusCriteria()) {
                Set<Status> statuses = MergeUtil.merge(criteria.getStatus(), criteria.getStatusSet());
                request.setStatuses(statuses);
            }

            if (criteria.hasSeverityCriteria()) {
                Set<Severity> severities = MergeUtil.merge(criteria.getSeverity(), criteria.getSeverities());
                request.setSeverities(severities);
            }

            if (criteria.hasTriggerIdCriteria()) {
                Set<String> triggerIds = MergeUtil.merge(criteria.getTriggerId(), criteria.getTriggerIds());
                request.setTriggerIds(triggerIds);
            }

            if (criteria.getQuery() != null) {
                request.setQuery(criteria.getQuery());
            }

            if (criteria.getTagQuery() != null) {
                request.setTagQuery(criteria.getTagQuery());
            }

            request.setThin(Boolean.valueOf(criteria.isThin()));
        }

        if (pager != null) {

            if (pager.isLimited()) {
                request.setPageNumber(pager.getPageNumber());
                request.setPageSize(pager.getPageSize());
            }

            if (pager.getOrder() != null) {
                for (Order order : pager.getOrder()) {
                    if (order.getField() != null) {
                        request.getOrderBy().add(order.getField() + " " + order.getDirection().getShortString());
                    }
                }
            }

        }

        return request;
    }
}
