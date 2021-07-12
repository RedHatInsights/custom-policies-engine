package com.redhat.cloud.policies.engine.history.alert.repositories;

import com.redhat.cloud.policies.engine.history.alert.requests.AlertsRequest;
import com.redhat.cloud.policies.engine.history.alert.entities.AlertEntity;
import com.redhat.cloud.policies.engine.history.alert.tags.TagQueryCondition;
import com.redhat.cloud.policies.engine.history.alert.tags.TagQueryParser;
import org.hibernate.Session;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.hawkular.alerts.api.model.event.Alert.Status;
import static org.hibernate.annotations.QueryHints.PASS_DISTINCT_THROUGH;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

@ApplicationScoped
public class AlertsRepository {

    private static final String BASE_SELECT_QUERY = "SELECT DISTINCT a FROM AlertEntity a LEFT JOIN FETCH a.tags t LEFT JOIN FETCH t.values";
    private static final String BASE_DELETE_QUERY = "DELETE FROM AlertEntity a";
    // FIXME The operator can vary. This only covers one case.
    private static final Pattern PREFIX_PATTERN = Pattern.compile("( |\\()(eventType|tenantId|id|triggerId|ctime|status|stime|severity|category) ?=", CASE_INSENSITIVE);

    @Inject
    Session session;

    public void save(AlertEntity alertEntity) {
        session.persist(alertEntity);
    }

    public AlertEntity find(String tenantId, String alertId) {
        String hqlQuery = "FROM AlertEntity WHERE tenantId = :tenantId AND id = :alertId";
        return session.createQuery(hqlQuery, AlertEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("alertId", alertId)
                .uniqueResult();
    }

    public List<AlertEntity> findAll(AlertsRequest alertsRequest) {
        List<TagQueryCondition> tagQueryConditions = TagQueryParser.parse(alertsRequest.getTagQuery());
        String hqlQuery = buildHqlQuery(BASE_SELECT_QUERY, alertsRequest, tagQueryConditions);
        TypedQuery<AlertEntity> typedQuery = session.createQuery(hqlQuery, AlertEntity.class)
                .setHint(PASS_DISTINCT_THROUGH, false)
                .setParameter("tenantId", alertsRequest.getTenantId());
        setQueryParameters(typedQuery, alertsRequest, tagQueryConditions);
        return typedQuery.getResultList();
    }

    public int delete(AlertsRequest alertsRequest) {
        List<TagQueryCondition> tagQueryConditions = TagQueryParser.parse(alertsRequest.getTagQuery());
        String hqlQuery = buildHqlQuery(BASE_DELETE_QUERY, alertsRequest, tagQueryConditions);
        Query query = session.createQuery(hqlQuery)
                .setParameter("tenantId", alertsRequest.getTenantId());
        setQueryParameters(query, alertsRequest, tagQueryConditions);
        return query.executeUpdate();
    }

    private static String buildHqlQuery(String baseHqlQuery, AlertsRequest alertsRequest, List<TagQueryCondition> tagQueryConditions) {
        List<String> conditions = new ArrayList<>();
        conditions.add("a.tenantId = :tenantId");

        if (alertsRequest.getQuery() != null && !alertsRequest.getQuery().isBlank()) {
            // AlertEntity fields need to be prefixed with "a.".
            String modifiedQuery = PREFIX_PATTERN.matcher(alertsRequest.getQuery()).replaceAll(matcher -> matcher.group(1) + " a." + matcher.group(2) + " =");
            conditions.add(modifiedQuery);
            // TODO Prevent SQL injections: parser?
        }

        if (alertsRequest.getStartTime() != null && alertsRequest.getEndTime() != null) {
            conditions.add("a.ctime BETWEEN :startTime AND :endTime");
        } else if (alertsRequest.getStartTime() != null) {
            conditions.add("a.ctime >= :startTime");
        } else if (alertsRequest.getEndTime() != null) {
            conditions.add("a.ctime <= :endTime");
        }

        if (alertsRequest.getStartResolvedTime() != null || alertsRequest.getEndResolvedTime() != null) {
            conditions.add("a.status = :status");
            if (alertsRequest.getStartResolvedTime() != null && alertsRequest.getEndResolvedTime() != null) {
                conditions.add("a.stime BETWEEN :startResolvedTime AND :endResolvedTime");
            } else if (alertsRequest.getStartResolvedTime() != null) {
                conditions.add("a.stime >= :startResolvedTime");
            } else if (alertsRequest.getEndResolvedTime() != null) {
                conditions.add("a.stime <= :endResolvedTime");
            }
        }

        if (alertsRequest.getStartAckTime() != null || alertsRequest.getEndAckTime() != null) {
            conditions.add("a.status = :status");
            if (alertsRequest.getStartAckTime() != null && alertsRequest.getEndAckTime() != null) {
                conditions.add("a.stime BETWEEN :startAckTime AND :endAckTime");
            } else if (alertsRequest.getStartAckTime() != null) {
                conditions.add("a.stime >= :startAckTime");
            } else if (alertsRequest.getEndAckTime() != null) {
                conditions.add("a.stime <= :endAckTime");
            }
        }

        if (alertsRequest.getStartStatusTime() != null && alertsRequest.getEndStatusTime() != null) {
            conditions.add("a.stime BETWEEN :startStatusTime AND :endStatusTime");
        } else if (alertsRequest.getStartStatusTime() != null) {
            conditions.add("a.stime >= :startStatusTime");
        } else if (alertsRequest.getEndStatusTime() != null) {
            conditions.add("a.stime <= :endStatusTime");
        }

        if (alertsRequest.getAlertIds() != null && !alertsRequest.getAlertIds().isEmpty()) {
            conditions.add("a.id IN (:alertIds)");
        }

        if (alertsRequest.getStatuses() != null && !alertsRequest.getStatuses().isEmpty()) {
            conditions.add("a.status IN (:statuses)");
        }

        if (alertsRequest.getSeverities() != null && !alertsRequest.getSeverities().isEmpty()) {
            conditions.add("a.severity IN (:severities)");
        }

        if (alertsRequest.getTriggerIds() != null && !alertsRequest.getTriggerIds().isEmpty()) {
            conditions.add("a.triggerId IN (:triggerIds)");
        }

        for (int i = 0; i < tagQueryConditions.size(); i++) {
            switch (tagQueryConditions.get(i).getOperator()) {
                case EQUAL:
                    conditions.add("EXISTS (SELECT 1 FROM TagEntity t JOIN t.values v WHERE t.event = a AND t.key = :tagQueryKey" + i + " AND LOWER(v.value) = LOWER(:tagQueryValue" + i + "))");
                    break;
                case NOT_EQUAL:
                    conditions.add("NOT EXISTS (SELECT 1 FROM TagEntity t JOIN t.values v WHERE t.event = a AND t.key = :tagQueryKey" + i + " AND LOWER(v.value) = LOWER(:tagQueryValue" + i + "))");
                    break;
                case LIKE:
                    conditions.add("EXISTS (SELECT 1 FROM TagEntity t JOIN t.values v WHERE t.event = a AND t.key = :tagQueryKey" + i + " AND LOWER(v.value) LIKE LOWER(:tagQueryValue" + i + "))");
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

    private static void setQueryParameters(Query query, AlertsRequest alertsRequest, List<TagQueryCondition> tagQueryConditions) {

        if (alertsRequest.getStartTime() != null && alertsRequest.getEndTime() != null) {
            query.setParameter("startTime", alertsRequest.getStartTime()).setParameter("endTime", alertsRequest.getEndTime());
        } else if (alertsRequest.getStartTime() != null) {
            query.setParameter("startTime", alertsRequest.getStartTime());
        } else if (alertsRequest.getEndTime() != null) {
            query.setParameter("endTime", alertsRequest.getEndTime());
        }

        if (alertsRequest.getStartResolvedTime() != null || alertsRequest.getEndResolvedTime() != null) {
            query.setParameter("status", Status.RESOLVED);
            if (alertsRequest.getStartResolvedTime() != null && alertsRequest.getEndResolvedTime() != null) {
                query.setParameter("startResolvedTime", alertsRequest.getStartResolvedTime()).setParameter("endResolvedTime", alertsRequest.getEndResolvedTime());
            } else if (alertsRequest.getStartResolvedTime() != null) {
                query.setParameter("startResolvedTime", alertsRequest.getStartResolvedTime());
            } else if (alertsRequest.getEndResolvedTime() != null) {
                query.setParameter("endResolvedTime", alertsRequest.getEndResolvedTime());
            }
        }

        if (alertsRequest.getStartAckTime() != null || alertsRequest.getEndAckTime() != null) {
            query.setParameter("status", Status.ACKNOWLEDGED);
            if (alertsRequest.getStartAckTime() != null && alertsRequest.getEndAckTime() != null) {
                query.setParameter("startAckTime", alertsRequest.getStartAckTime()).setParameter("endAckTime", alertsRequest.getEndAckTime());
            } else if (alertsRequest.getStartAckTime() != null) {
                query.setParameter("startAckTime", alertsRequest.getStartAckTime());
            } else if (alertsRequest.getEndAckTime() != null) {
                query.setParameter("endAckTime", alertsRequest.getEndAckTime());
            }
        }

        if (alertsRequest.getStartStatusTime() != null && alertsRequest.getEndStatusTime() != null) {
            query.setParameter("startStatusTime", alertsRequest.getStartStatusTime()).setParameter("endStatusTime", alertsRequest.getEndStatusTime());
        } else if (alertsRequest.getStartStatusTime() != null) {
            query.setParameter("startStatusTime", alertsRequest.getStartStatusTime());
        } else if (alertsRequest.getEndStatusTime() != null) {
            query.setParameter("endStatusTime", alertsRequest.getEndStatusTime());
        }

        if (alertsRequest.getAlertIds() != null && !alertsRequest.getAlertIds().isEmpty()) {
            query.setParameter("alertIds", alertsRequest.getAlertIds());
        }

        if (alertsRequest.getStatuses() != null && !alertsRequest.getStatuses().isEmpty()) {
            query.setParameter("statuses", alertsRequest.getStatuses());
        }

        if (alertsRequest.getSeverities() != null && !alertsRequest.getSeverities().isEmpty()) {
            query.setParameter("severities", alertsRequest.getSeverities());
        }

        if (alertsRequest.getTriggerIds() != null && !alertsRequest.getTriggerIds().isEmpty()) {
            query.setParameter("triggerIds", alertsRequest.getTriggerIds());
        }

        for (int i = 0; i < tagQueryConditions.size(); i++) {
            query.setParameter("tagQueryKey" + i, tagQueryConditions.get(i).getKey());
            query.setParameter("tagQueryValue" + i, tagQueryConditions.get(i).getValue());
        }
    }
}
