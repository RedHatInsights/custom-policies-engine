package com.redhat.cloud.policies.engine.history.alert.repositories;

import com.redhat.cloud.policies.engine.history.alert.requests.EventsRequest;
import com.redhat.cloud.policies.engine.history.alert.entities.EventEntity;
import com.redhat.cloud.policies.engine.history.alert.tags.TagQueryCondition;
import com.redhat.cloud.policies.engine.history.alert.tags.TagQueryParser;
import org.hibernate.Session;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.annotations.QueryHints.PASS_DISTINCT_THROUGH;

@ApplicationScoped
public class EventsRepository {

    private static final String BASE_SELECT_QUERY = "SELECT DISTINCT e FROM EventEntity e LEFT JOIN FETCH e.tags t LEFT JOIN FETCH t.values";
    private static final String BASE_DELETE_QUERY = "DELETE FROM EventEntity e";

    @Inject
    Session session;

    public void save(EventEntity eventEntity) {
        session.persist(eventEntity);
    }

    public EventEntity find(String tenantId, String eventId) {
        String hqlQuery = "FROM EventEntity WHERE tenantId = :tenantId AND id = :eventId";
        return session.createQuery(hqlQuery, EventEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("eventId", eventId)
                .uniqueResult();
    }

    public List<EventEntity> findAll(EventsRequest eventsRequest) {
        List<TagQueryCondition> tagQueryConditions = TagQueryParser.parse(eventsRequest.getTagQuery());
        String hqlQuery = buildHqlQuery(BASE_SELECT_QUERY, eventsRequest, tagQueryConditions);
        TypedQuery<EventEntity> typedQuery = session.createQuery(hqlQuery, EventEntity.class)
                .setHint(PASS_DISTINCT_THROUGH, false)
                .setParameter("tenantId", eventsRequest.getTenantId());
        setQueryParameters(typedQuery, eventsRequest, tagQueryConditions);
        return typedQuery.getResultList();
    }

    public int delete(EventsRequest eventsRequest) {
        List<TagQueryCondition> tagQueryConditions = TagQueryParser.parse(eventsRequest.getTagQuery());
        String hqlQuery = buildHqlQuery(BASE_DELETE_QUERY, eventsRequest, tagQueryConditions);
        Query query = session.createQuery(hqlQuery)
                .setParameter("tenantId", eventsRequest.getTenantId());
        setQueryParameters(query, eventsRequest, tagQueryConditions);
        return query.executeUpdate();
    }

    private static String buildHqlQuery(String baseHqlQuery, EventsRequest eventsRequest, List<TagQueryCondition> tagQueryConditions) {
        List<String> conditions = new ArrayList<>();
        conditions.add("e.tenantId = :tenantId");

        if (eventsRequest.getEventType() != null) {
            conditions.add("e.eventType = :eventType");
        }

        if (eventsRequest.getStartTime() != null && eventsRequest.getEndTime() != null) {
            conditions.add("e.ctime BETWEEN :startTime AND :endTime");
        } else if (eventsRequest.getStartTime() != null) {
            conditions.add("e.ctime >= :startTime");
        } else if (eventsRequest.getEndTime() != null) {
            conditions.add("e.ctime <= :endTime");
        }

        if (eventsRequest.getEventIds() != null && !eventsRequest.getEventIds().isEmpty()) {
            conditions.add("e.id IN (:eventIds)");
        }

        if (eventsRequest.getTriggerIds() != null && !eventsRequest.getTriggerIds().isEmpty()) {
            conditions.add("e.triggerId IN (:triggerIds)");
        }

        if (eventsRequest.getCategories() != null && !eventsRequest.getCategories().isEmpty()) {
            conditions.add("e.category IN (:categories)");
        }

        for (int i = 0; i < tagQueryConditions.size(); i++) {
            switch (tagQueryConditions.get(i).getOperator()) {
                case EQUAL:
                    conditions.add("EXISTS (SELECT 1 FROM TagEntity t JOIN t.values v WHERE t.event = e AND t.key = :tagQueryKey" + i + " AND LOWER(v.value) = LOWER(:tagQueryValue" + i + "))");
                    break;
                case NOT_EQUAL:
                    conditions.add("NOT EXISTS (SELECT 1 FROM TagEntity t JOIN t.values v WHERE t.event = e AND t.key = :tagQueryKey" + i + " AND LOWER(v.value) = LOWER(:tagQueryValue" + i + "))");
                    break;
                case LIKE:
                    conditions.add("EXISTS (SELECT 1 FROM TagEntity t JOIN t.values v WHERE t.event = e AND t.key = :tagQueryKey" + i + " AND LOWER(v.value) LIKE LOWER(:tagQueryValue" + i + "))");
                    break;
                default:
                    // This line should never be reached.
                    break;
            }
        }

        if (!conditions.isEmpty()) {
            baseHqlQuery += " WHERE " + String.join(" AND ", conditions);
        }

        return baseHqlQuery;
    }

    private static void setQueryParameters(Query query, EventsRequest eventsRequest, List<TagQueryCondition> tagQueryConditions) {

        if (eventsRequest.getEventType() != null) {
            query.setParameter("eventType", eventsRequest.getEventType());
        }

        if (eventsRequest.getStartTime() != null && eventsRequest.getEndTime() != null) {
            query.setParameter("startTime", eventsRequest.getStartTime()).setParameter("endTime", eventsRequest.getEndTime());
        } else if (eventsRequest.getStartTime() != null) {
            query.setParameter("startTime", eventsRequest.getStartTime());
        } else if (eventsRequest.getEndTime() != null) {
            query.setParameter("endTime", eventsRequest.getEndTime());
        }

        if (eventsRequest.getEventIds() != null && !eventsRequest.getEventIds().isEmpty()) {
            query.setParameter("eventIds", eventsRequest.getEventIds());
        }

        if (eventsRequest.getTriggerIds() != null && !eventsRequest.getTriggerIds().isEmpty()) {
            query.setParameter("triggerIds", eventsRequest.getTriggerIds());
        }

        if (eventsRequest.getCategories() != null && !eventsRequest.getCategories().isEmpty()) {
            query.setParameter("categories", eventsRequest.getCategories());
        }

        for (int i = 0; i < tagQueryConditions.size(); i++) {
            query.setParameter("tagQueryKey" + i, tagQueryConditions.get(i).getKey());
            query.setParameter("tagQueryValue" + i, tagQueryConditions.get(i).getValue());
        }
    }
}
