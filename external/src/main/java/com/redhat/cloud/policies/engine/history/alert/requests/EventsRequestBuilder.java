package com.redhat.cloud.policies.engine.history.alert.requests;

import org.hawkular.alerts.api.model.paging.Order;
import org.hawkular.alerts.api.model.paging.Pager;
import org.hawkular.alerts.api.services.EventsCriteria;

import java.util.Set;

public class EventsRequestBuilder {

    public static EventsRequest build(String tenantId, EventsCriteria criteria) {
        return build(tenantId, criteria, null);
    }

    public static EventsRequest build(String tenantId, EventsCriteria criteria, Pager pager) {
        EventsRequest request = new EventsRequest();
        request.setTenantId(tenantId);

        if (criteria != null && criteria.hasCriteria()) {

            if (criteria.hasCTimeCriteria()) {
                request.setStartTime(criteria.getStartTime());
                request.setEndTime(criteria.getEndTime());
            }

            if (criteria.hasEventIdCriteria()) {
                Set<String> eventIds = MergeUtil.merge(criteria.getEventId(), criteria.getEventIds());
                request.setEventIds(eventIds);
            }

            if (criteria.hasTriggerIdCriteria()) {
                Set<String> triggerIds = MergeUtil.merge(criteria.getTriggerId(), criteria.getTriggerIds());
                request.setTriggerIds(triggerIds);
            }

            if (criteria.hasCategoryCriteria()) {
                Set<String> categories = MergeUtil.merge(criteria.getCategory(), criteria.getCategories());
                request.setCategories(categories);
            }

            if (criteria.getEventType() != null) {
                request.setEventType(criteria.getEventType());
            }

            if (criteria.getTagQuery() != null) {
                request.setTagQuery(criteria.getTagQuery());
            }

            request.setThin(Boolean.valueOf(criteria.isThin()));

            if (criteria.getCriteriaNoQuerySize() != null) {
                request.setCriteriaNoQuerySize(criteria.getCriteriaNoQuerySize());
            }
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
