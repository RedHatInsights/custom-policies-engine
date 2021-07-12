package com.redhat.cloud.policies.engine.history.alert;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.redhat.cloud.policies.engine.history.alert.entities.AlertEntity;
import com.redhat.cloud.policies.engine.history.alert.entities.EventBaseEntity;
import com.redhat.cloud.policies.engine.history.alert.entities.EventEntity;
import com.redhat.cloud.policies.engine.history.alert.entities.TagEntity;
import com.redhat.cloud.policies.engine.history.alert.entities.TagValueEntity;
import com.redhat.cloud.policies.engine.history.alert.requests.AlertsRequest;
import com.redhat.cloud.policies.engine.history.alert.requests.AlertsRequestBuilder;
import com.redhat.cloud.policies.engine.history.alert.requests.EventsRequest;
import com.redhat.cloud.policies.engine.history.alert.requests.EventsRequestBuilder;
import com.redhat.cloud.policies.engine.history.alert.repositories.AlertsRepository;
import com.redhat.cloud.policies.engine.history.alert.repositories.EventsRepository;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hawkular.alerts.api.model.Note;
import org.hawkular.alerts.api.model.condition.ConditionEval;
import org.hawkular.alerts.api.model.data.Data;
import org.hawkular.alerts.api.model.event.Alert;
import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.api.model.paging.AlertComparator;
import org.hawkular.alerts.api.model.paging.EventComparator;
import org.hawkular.alerts.api.model.paging.Order;
import org.hawkular.alerts.api.model.paging.Page;
import org.hawkular.alerts.api.model.paging.Pager;
import org.hawkular.alerts.api.model.trigger.Mode;
import org.hawkular.alerts.api.model.trigger.Trigger;
import org.hawkular.alerts.api.services.ActionsService;
import org.hawkular.alerts.api.services.AlertsCriteria;
import org.hawkular.alerts.api.services.AlertsService;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.hawkular.alerts.api.services.EventsCriteria;
import org.hawkular.alerts.engine.impl.IncomingDataManagerImpl;
import org.hawkular.alerts.engine.service.AlertsEngine;
import org.hawkular.alerts.engine.service.IncomingDataManager;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.redhat.cloud.policies.engine.history.alert.PoliciesHistoryAlertsService.POLICIES_HISTORY_ALERTS_SERVICE_ENABLED_CONF_KEY;
import static org.hawkular.alerts.api.util.Util.isEmpty;

import static org.hawkular.alerts.api.model.event.Alert.Status;

@ApplicationScoped
@IfBuildProperty(name = POLICIES_HISTORY_ALERTS_SERVICE_ENABLED_CONF_KEY, stringValue = "true")
public class PoliciesHistoryAlertsService implements AlertsService {

    public static final String POLICIES_HISTORY_ALERTS_SERVICE_ENABLED_CONF_KEY = "policies-history.alerts-service.enabled";

    private static final Logger LOGGER = Logger.getLogger(PoliciesHistoryAlertsService.class);

    @ConfigProperty(name = "engine.backend.ispn.alerts-lifespan")
    long alertsLifespanInHours;

    @ConfigProperty(name = "engine.backend.ispn.events-lifespan")
    long eventsLifespanInHours;

    @ConfigProperty(name = "engine.backend.ispn.alerts-thin")
    boolean saveThinAlerts;

    @Inject
    AlertsEngine alertsEngine;

    @Inject
    DefinitionsService definitionsService;

    @Inject
    ActionsService actionsService;

    @Inject
    IncomingDataManager incomingDataManager;

    @Inject
    EventsRepository eventsRepository;

    @Inject
    AlertsRepository alertsRepository;

    private void store(Event event) {
        EventEntity eventEntity = new EventEntity();
        if (eventsLifespanInHours > 0) {
            eventEntity.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(eventsLifespanInHours));
        }
        setEventBaseEntityFields(eventEntity, event);
        eventsRepository.save(eventEntity);
    }

    private void store(Alert alert) {
        AlertEntity alertEntity = new AlertEntity();
        if (alertsLifespanInHours > 0) {
            alertEntity.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(alertsLifespanInHours));
        }
        setEventBaseEntityFields(alertEntity, alert);
        alertEntity.setSeverity(alert.getSeverity());
        alertEntity.setStatus(alert.getStatus()); // TODO Is that really the good one or do we need two?
        alertEntity.setLifecycle(alert.getLifecycle());
        alertEntity.setResolvedEvalSets(alert.getResolvedEvalSets());
        alertEntity.setStime(alert.getCurrentLifecycle().getStime());
        alertsRepository.save(alertEntity);
    }

    private void setEventBaseEntityFields(EventBaseEntity eventBaseEntity, Event event) {
        eventBaseEntity.setTenantId(event.getTenantId());
        eventBaseEntity.setId(event.getId());
        eventBaseEntity.setEventType(event.getEventType());
        eventBaseEntity.setCtime(event.getCtime());
        eventBaseEntity.setDatasource(event.getDataSource());
        eventBaseEntity.setDataId(event.getDataId());
        eventBaseEntity.setCategory(event.getCategory());
        eventBaseEntity.setText(event.getText());
        eventBaseEntity.setContext(event.getContext());
        eventBaseEntity.setTrigger(event.getTrigger());
        eventBaseEntity.setDampening(event.getDampening());
        eventBaseEntity.setEvalSets(event.getEvalSets());
        eventBaseEntity.setFacts(event.getFacts());

        if (eventBaseEntity.getTrigger() != null) {
            eventBaseEntity.setTriggerId(eventBaseEntity.getTrigger().getId());
        }

        /*
         * Guava's Multimap can't be persisted and then queried with Hibernate ORM so we have to convert it to something
         * more Hibernate-friendly.
         */
        Set<TagEntity> tagEntities = event.getTags().asMap().entrySet().stream().map(tag -> {
            TagEntity tagEntity = new TagEntity();
            tagEntity.setUuid(UUID.randomUUID());
            tagEntity.setEvent(eventBaseEntity);
            tagEntity.setKey(tag.getKey());
            Set<TagValueEntity> tagValueEntities = tag.getValue().stream().map(tagValue -> {
                TagValueEntity tagValueEntity = new TagValueEntity();
                tagValueEntity.setUuid(UUID.randomUUID());
                tagValueEntity.setTag(tagEntity);
                tagValueEntity.setValue(tagValue);
                return tagValueEntity;
            }).collect(Collectors.toSet());
            tagEntity.setValues(tagValueEntities);
            return tagEntity;
        }).collect(Collectors.toSet());
        eventBaseEntity.setTags(tagEntities);
    }






















// FIXME
// persist est probablement appelé lors d'updates...















    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void ackAlerts(String tenantId, Collection<String> alertIds, String ackBy, String ackNotes) throws Exception {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(alertIds)) {
            return;
        }

        AlertsCriteria criteria = new AlertsCriteria();
        criteria.setAlertIds(alertIds);

        // TODO Update without prior select may be possible
        List<Alert> alertsToAck = getAlerts(tenantId, criteria, null);

        long timestamp = System.currentTimeMillis();
        for (Alert alert : alertsToAck) {
            List<Note> notes = null;
            if(!isEmpty(ackBy)) {
                notes = List.of(new Note(ackBy, ackNotes));
            }
            alert.addLifecycle(Status.ACKNOWLEDGED, timestamp, notes);
            store(alert);
            sendAction(alert);
        }
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void addAlerts(Collection<Alert> alerts) {
        if (alerts == null) {
            throw new IllegalArgumentException("Alerts must be not null");
        }
        if (alerts.isEmpty()) {
            return;
        }
        LOGGER.debugf("Adding %s alerts", alerts.size());
        for (Alert alert : alerts) {
            if(saveThinAlerts) {
                // This reduces the storage requirements by not storing runtime evaluation information
                alert.setDampening(null);
                alert.setEvalSets(null);
                alert.setResolvedEvalSets(null);
            }
            store(alert);
        }
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void addAlertTags(String tenantId, Collection<String> alertIds, Map<String, String> tags) throws Exception {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(alertIds)) {
            throw new IllegalArgumentException("AlertIds must be not null");
        }
        if (isEmpty(tags)) {
            throw new IllegalArgumentException("Tags must be not null");
        }

        AlertsCriteria criteria = new AlertsCriteria();
        criteria.setAlertIds(alertIds);
        // TODO Update without prior select is possible
        Page<Alert> existingAlerts = getAlerts(tenantId, criteria, null);

        for (Alert alert : existingAlerts) {
            tags.entrySet().stream().forEach(tag -> alert.addTag(tag.getKey(), tag.getValue()));
            store(alert);
        }
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Uni<Void> addEvents(Collection<Event> events) throws Exception {
        if (null == events || events.isEmpty()) {
            return Uni.createFrom().nullItem();
        }
        return persistEvents(events)
                .replaceWith(sendEvents(events));
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void addEventTags(String tenantId, Collection<String> eventIds, Map<String, String> tags) throws Exception {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(eventIds)) {
            throw new IllegalArgumentException("AlertIds must be not null");
        }
        if (isEmpty(tags)) {
            throw new IllegalArgumentException("Tags must be not null");
        }

        EventsCriteria criteria = new EventsCriteria();
        criteria.setEventIds(eventIds);
        // TODO Update without prior select is possible
        Page<Event> existingEvents = getEvents(tenantId, criteria, null);

        for (Event event : existingEvents) {
            tags.entrySet().stream().forEach(tag -> event.addTag(tag.getKey(), tag.getValue()));
            store(event);
        }
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Uni<Void> persistEvents(Collection<Event> events) {
        if (events == null) {
            throw new IllegalArgumentException("Events must be not null");
        }
        if (events.isEmpty()) {
            return Uni.createFrom().nullItem();
        }
        LOGGER.debugf("Adding %s events", events.size());
        return Uni.createFrom().item(() -> {
            for (Event event : events) {
                store(event);
            }
            return null;
        });
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void addNote(String tenantId, String alertId, String user, String text) {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(alertId)) {
            throw new IllegalArgumentException("AlertId must be not null");
        }
        if (isEmpty(user) || isEmpty(text)) {
            throw new IllegalArgumentException("user or text must be not null");
        }

        // TODO Update without prior select is possible
        Alert alert = getAlert(tenantId, alertId, false);
        if (alert == null) {
            return;
        }

        alert.addNote(new Note(user, System.currentTimeMillis(), text));

        store(alert);
    }

    // Modified code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public int deleteAlerts(String tenantId, AlertsCriteria criteria) {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (null == criteria) {
            throw new IllegalArgumentException("Criteria must be not null");
        }
        // no need to fetch the evalSets to perform the necessary deletes
        criteria.setThin(true);

        AlertsRequest alertsRequest = AlertsRequestBuilder.build(tenantId, criteria);
        return alertsRepository.delete(alertsRequest);
    }

    // Modified code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public int deleteEvents(String tenantId, EventsCriteria criteria) {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (null == criteria) {
            throw new IllegalArgumentException("Criteria must be not null");
        }
        // no need to fetch the evalSets to perform the necessary deletes
        criteria.setThin(true);

        EventsRequest eventsRequest = EventsRequestBuilder.build(tenantId, criteria);
        return eventsRepository.delete(eventsRequest);
    }

    // Modified code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Alert getAlert(String tenantId, String alertId, boolean thin) {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(alertId)) {
            throw new IllegalArgumentException("AlertId must be not null");
        }

        AlertEntity alertEntity = alertsRepository.find(tenantId, alertId);
        return convert(alertEntity, thin);
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Page<Alert> getAlerts(String tenantId, AlertsCriteria criteria, Pager pager) throws Exception {
        return getAlerts(Collections.singleton(tenantId), criteria, pager);
    }

    // Modified code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Page<Alert> getAlerts(Set<String> tenantIds, AlertsCriteria criteria, Pager pager) {
        if (isEmpty(tenantIds)) {
            throw new IllegalArgumentException("TenantIds must not be null or empty");
        }

        if (criteria == null) {
            criteria = new AlertsCriteria();
        }

        // Set the query starting point to the earliest retention time to prevent incorrect
        // return of the Query maxResults
        long earliestRetentionTime = Instant.now().minus(alertsLifespanInHours, ChronoUnit.HOURS).toEpochMilli();

        if (criteria.getStartTime() == null || criteria.getStartTime() < earliestRetentionTime) {
            criteria.setStartTime(earliestRetentionTime);
        }

        String tenantId = tenantIds.iterator().next();
        AlertsRequest alertsRequest = AlertsRequestBuilder.build(tenantId, criteria, pager);

        boolean thin = criteria.isThin();
        List<Alert> alerts = alertsRepository.findAll(alertsRequest).stream()
                .map(alertEntity -> convert(alertEntity, thin))
                .collect(Collectors.toList());
        return preparePage(alerts, pager, alerts.size());
    }

    // Modified code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Event getEvent(String tenantId, String eventId, boolean thin) {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(eventId)) {
            throw new IllegalArgumentException("EventId must be not null");
        }

        EventEntity eventEntity = eventsRepository.find(tenantId, eventId);
        return convert(eventEntity, thin);
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Page<Event> getEvents(String tenantId, EventsCriteria criteria, Pager pager) throws Exception {
        return getEvents(Collections.singleton(tenantId), criteria, pager);
    }

    // Modified code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Page<Event> getEvents(Set<String> tenantIds, EventsCriteria criteria, Pager pager) throws Exception {
        if (isEmpty(tenantIds)) {
            throw new IllegalArgumentException("TenantIds must be not null");
        }

        // Set the query starting point to the earliest retention time to prevent incorrect
        // return of the Query maxResults
        long earliestRetentionTime = Instant.now().minus(alertsLifespanInHours, ChronoUnit.HOURS).toEpochMilli();

        String tenantId = tenantIds.iterator().next();
        EventsRequest eventsRequest = EventsRequestBuilder.build(tenantId, criteria, pager);

        List<Event> events = eventsRepository.findAll(eventsRequest).stream()
                .map(eventEntity -> convert(eventEntity, criteria.isThin()))
                .collect(Collectors.toList());
        return prepareEventsPage(events, pager, events.size());
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void removeAlertTags(String tenantId, Collection<String> alertIds, Collection<String> tags) throws Exception {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(alertIds)) {
            throw new IllegalArgumentException("AlertIds must be not null");
        }
        if (isEmpty(tags)) {
            throw new IllegalArgumentException("Tags must be not null");
        }

        // Only untag existing alerts
        AlertsCriteria criteria = new AlertsCriteria();
        criteria.setAlertIds(alertIds);
        // TODO Update without prior select is possible
        Page<Alert> existingAlerts = getAlerts(tenantId, criteria, null);

        for (Alert alert : existingAlerts) {
            boolean modified = false;
            for (String tag : tags) {
                if (alert.getTags().containsKey(tag)) {
                    alert.removeTag(tag);
                    modified = true;
                }
            }
            if (modified) {
                store(alert);
            }
        }
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void removeEventTags(String tenantId, Collection<String> eventIds, Collection<String> tags) throws Exception {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(eventIds)) {
            throw new IllegalArgumentException("EventIds must be not null");
        }
        if (isEmpty(tags)) {
            throw new IllegalArgumentException("Tags must be not null");
        }

        // Only untag existing events
        EventsCriteria criteria = new EventsCriteria();
        criteria.setEventIds(eventIds);
        // TODO Update without prior select is possible
        Page<Event> existingEvents = getEvents(tenantId, criteria, null);

        for (Event event : existingEvents) {
            boolean modified = false;
            for (String tag : tags) {
                if (event.getTags().containsKey(tag)) {
                    event.removeTag(tag);
                    modified = true;
                }
            }
            if (modified) {
                store(event);
            }
        }
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void resolveAlerts(String tenantId, Collection<String> alertIds, String resolvedBy, String resolvedNotes, List<Set<ConditionEval>> resolvedEvalSets) throws Exception {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(alertIds)) {
            return;
        }

        if (isEmpty(resolvedBy)) {
            resolvedBy = "unknown";
        }
        if (isEmpty(resolvedNotes)) {
            resolvedNotes = "none";
        }

        AlertsCriteria criteria = new AlertsCriteria();
        criteria.setAlertIds(alertIds);
        List<Alert> alertsToResolve = getAlerts(tenantId, criteria, null);

        // resolve the alerts
        long timestamp = System.currentTimeMillis();
        for (Alert alert : alertsToResolve) {
            List<Note> notes = List.of(new Note(resolvedBy, timestamp, resolvedNotes));
            alert.setResolvedEvalSets(resolvedEvalSets);
            alert.addLifecycle(Alert.Status.RESOLVED, timestamp, notes);
            store(alert);
            sendAction(alert);
        }

        // gather the triggerIds of the triggers we need to check for resolve options
        Set<String> triggerIds = alertsToResolve.stream().map(Alert::getTriggerId).collect(Collectors.toSet());

        // handle resolve options
        triggerIds.forEach(tid -> handleResolveOptions(tenantId, tid, true));
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void resolveAlertsForTrigger(String tenantId, String triggerId, String resolvedBy, String resolvedNotes, List<Set<ConditionEval>> resolvedEvalSets) throws Exception {
        if (isEmpty(tenantId)) {
            throw new IllegalArgumentException("TenantId must be not null");
        }
        if (isEmpty(triggerId)) {
            throw new IllegalArgumentException("TriggerId must be not null");
        }

        AlertsCriteria criteria = new AlertsCriteria();
        criteria.setTriggerId(triggerId);
        criteria.setStatusSet(EnumSet.complementOf(EnumSet.of(Alert.Status.RESOLVED)));
        List<Alert> alertsToResolve = getAlerts(tenantId, criteria, null);

        long timestamp = System.currentTimeMillis();
        for (Alert alert : alertsToResolve) {
            List<Note> notes = null;
            if(!isEmpty(resolvedBy)) {
                notes = List.of(new Note(resolvedBy, timestamp, resolvedNotes));
            }
            alert.setResolvedEvalSets(resolvedEvalSets);
            alert.addLifecycle(Alert.Status.RESOLVED, timestamp, notes);
            store(alert);
            sendAction(alert);
        }

        handleResolveOptions(tenantId, triggerId, false);
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void sendData(Collection<Data> data) throws Exception {
        sendData(data, false);
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public void sendData(Collection<Data> data, boolean ignoreFiltering) throws Exception {
        if (isEmpty(data)) {
            return;
        }

        if (incomingDataManager == null) {
            LOGGER.info("incomingDataManager is not defined. Only valid for testing.");
            return;
        }

        incomingDataManager.bufferData(new IncomingDataManagerImpl.IncomingData(data, !ignoreFiltering));
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Uni<Void> sendEvents(Collection<Event> events) throws Exception {
        return sendEvents(events, false);
    }

    // Unchanged code copied from IspnAlertsServiceImpl.
    @Override
    @Transactional
    public Uni<Void> sendEvents(Collection<Event> events, boolean ignoreFiltering) throws Exception {
        if (isEmpty(events)) {
            return Uni.createFrom().nullItem();
        }

        if (incomingDataManager == null) {
            LOGGER.debug("incomingDataManager is not defined. Only valid for testing.");
            return Uni.createFrom().nullItem();
        }

        return Uni.createFrom().item(() -> {
            incomingDataManager.bufferEvents(new IncomingDataManagerImpl.IncomingEvents(events, !ignoreFiltering));
            return null;
        });
    }

    private boolean isServerSideSorted(Pager pager) {
        if(pager == null || pager.getOrder() == null || pager.getOrder().isEmpty()) {
            return true;
        }
        String field = pager.getOrder().get(0).getField();
        return field == null || AlertComparator.Field.ALERT_ID.getText().equals(field)
                || AlertComparator.Field.CTIME.getText().equals(field);
    }

    // Private methods
    // TODO Merge preparePage and prepareEventsPage, EventComparator and AlertsComparator

    private Page<Alert> preparePage(List<Alert> alerts, Pager pager, long totalSize) {
        if (pager != null) {
            if (pager.getOrder() != null
                    && !pager.getOrder().isEmpty()
                    && pager.getOrder().get(0).getField() == null) {
                pager = Pager.builder()
                        .withPageSize(pager.getPageSize())
                        .withStartPage(pager.getPageNumber())
                        .orderBy(AlertComparator.Field.ALERT_ID.getText(), Order.Direction.ASCENDING).build();
            }
            if (pager.getOrder() != null) {
                pager.getOrder().stream()
                        .filter(o -> o.getField() != null && o.getDirection() != null)
                        .forEach(o -> {
                            AlertComparator comparator = new AlertComparator(o.getField(), o.getDirection());
                            alerts.sort(comparator);
                        });
            }
            if(!isServerSideSorted(pager) && pager.isLimited()) {
                // We need to filter the amounts here
                if(pager.getEnd() >= alerts.size()) {
                    return new Page<>(alerts.subList(pager.getStart(), alerts.size()), pager, alerts.size());
                }
                return new Page<>(alerts.subList(pager.getStart(), pager.getEnd()), pager, alerts.size());
            }
        } else {
            AlertComparator.Field defaultField = AlertComparator.Field.ALERT_ID;
            Order.Direction defaultDirection = Order.Direction.ASCENDING;
            AlertComparator comparator = new AlertComparator(defaultField.getText(), defaultDirection);
            pager = Pager.builder().withPageSize(alerts.size()).orderBy(defaultField.getText(), defaultDirection)
                    .build();
            alerts.sort(comparator);
        }

        return new Page<>(alerts, pager, totalSize);
    }

    private void sendAction(Alert a) {
        if (actionsService != null && a != null && a.getTrigger() != null) {
            actionsService.send(a.getTrigger(), a);
        }
    }

    private void handleResolveOptions(String tenantId, String triggerId, boolean checkIfAllResolved) {

        if (definitionsService == null || alertsEngine == null) {
            LOGGER.debug("definitionsService or alertsEngine are not defined. Only valid for testing.");
            return;
        }

        try {
            Trigger trigger = definitionsService.getTrigger(tenantId, triggerId);
            if (null == trigger) {
                return;
            }

            boolean setEnabled = trigger.isAutoEnable() && !trigger.isEnabled();
            boolean setFiring = trigger.isAutoResolve();

            // Only reload the trigger if it is not already in firing mode, otherwise we could lose partial matching.
            // This is a rare case because a trigger with autoResolve=true will not be in firing mode with an
            // unresolved trigger. But it is possible, either by mistake, or timing,  for a client to try and
            // resolve an already-resolved alert.
            if (setFiring) {
                Trigger loadedTrigger = alertsEngine.getLoadedTrigger(trigger);
                if (null != loadedTrigger && Mode.FIRING == loadedTrigger.getMode()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Ignoring setFiring, loaded Trigger already in firing mode " +
                                loadedTrigger.toString());
                    }
                    setFiring = false;
                }
            }

            if (!(setEnabled || setFiring)) {
                return;
            }

            boolean allResolved = true;
            if (checkIfAllResolved) {
                AlertsCriteria ac = new AlertsCriteria();
                ac.setTriggerId(triggerId);
                ac.setStatusSet(EnumSet.complementOf(EnumSet.of(Status.RESOLVED)));
                Page<Alert> unresolvedAlerts = getAlerts(tenantId, ac, new Pager(0, 1, Order.unspecified()));
                allResolved = unresolvedAlerts.isEmpty();
            }

            if (!allResolved) {
                LOGGER.debugf("Ignoring resolveOptions, not all Alerts for Trigger %s are resolved", trigger.toString());
                return;
            }

            // Either update the trigger, which implicitly reloads the trigger (and as such resets to firing mode)
            // or perform an explicit reload to reset to firing mode.
            if (setEnabled) {
                trigger.setEnabled(true);
                definitionsService.updateTrigger(tenantId, trigger, true);
            } else {
                alertsEngine.reloadTrigger(tenantId, triggerId);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private Page<Event> prepareEventsPage(List<Event> events, Pager pager, long totalSize) {
        if (pager != null) {
            if (pager.getOrder() != null
                    && !pager.getOrder().isEmpty()
                    && pager.getOrder().get(0).getField() == null) {
                pager = Pager.builder()
                        .withPageSize(pager.getPageSize())
                        .withStartPage(pager.getPageNumber())
                        .orderBy(EventComparator.Field.ID.getName(), Order.Direction.ASCENDING).build();
            }
            if (pager.getOrder() != null) {
                pager.getOrder()
                        .stream()
                        .filter(o -> o.getField() != null && o.getDirection() != null)
                        .forEach(o -> {
                            EventComparator comparator = new EventComparator(o.getField(), o.getDirection());
                            events.sort(comparator);
                        });
            }
            if(!isServerSideSorted(pager) && pager.isLimited()) {
                /*
            if (!pager.isLimited() || ordered.size() < pager.getStart()) {
                pager = new Pager(0, ordered.size(), pager.getOrder());
                return new Page<>(ordered, pager, ordered.size());
            }
            if (pager.getEnd() >= ordered.size()) {
                return new Page<>(ordered.subList(pager.getStart(), ordered.size()), pager, ordered.size());
            }
            return new Page<>(ordered.subList(pager.getStart(), pager.getEnd()), pager, ordered.size());
                 */

                // TODO What if the start is larger than the amount? Will it crash?

                // We need to filter the amounts here
                if(pager.getEnd() >= events.size()) {
                    return new Page<>(events.subList(pager.getStart(), events.size()), pager, events.size());
                }
                return new Page<>(events.subList(pager.getStart(), pager.getEnd()), pager, events.size());
            }
        } else {
            EventComparator.Field defaultField = EventComparator.Field.ID;
            Order.Direction defaultDirection = Order.Direction.ASCENDING;
            pager = Pager.builder().withPageSize(events.size()).orderBy(defaultField.getName(),
                    defaultDirection).build();
            EventComparator comparator = new EventComparator(defaultField.getName(), defaultDirection);
            Collections.sort(events, comparator);
        }
        return new Page<>(events, pager, totalSize);
    }

    private Alert convert(AlertEntity alertEntity, boolean thin) {
        if (alertEntity == null) {
            return null;
        }

        Alert alert = new Alert();
        setEventFields(alert, alertEntity);
        alert.setSeverity(alertEntity.getSeverity());
        alert.setStatus(alertEntity.getStatus());
        alert.setLifecycle(alertEntity.getLifecycle());
        alert.setResolvedEvalSets(alertEntity.getResolvedEvalSets());

        Multimap<String, String> tags = MultimapBuilder.hashKeys(123).hashSetValues(456).build();
        for (TagEntity tagEntity : alertEntity.getTags()) {
            tags.putAll(tagEntity.getKey(), tagEntity.getValues().stream().map(TagValueEntity::getValue).collect(Collectors.toSet()));
        }
        alert.setTags(tags);

        // TODO Needs more work.
        if (thin) {
            alert.setDampening(null);
            alert.setEvalSets(null);
            alert.setResolvedEvalSets(null);
            alert.getTrigger().setActions(null);
            alert.getTrigger().setLifecycle(null);
        }

        return alert;
    }

    private Event convert(EventEntity eventEntity, boolean thin) {
        if (eventEntity == null) {
            return null;
        }

        Event event = new Event();
        setEventFields(event, eventEntity);

        // TODO Needs more work.
        if (thin) {
        }
        return event;
    }

    private void setEventFields(Event event, EventBaseEntity eventBaseEntity) {
        event.setId(eventBaseEntity.getId());
        event.setEventType(eventBaseEntity.getEventType());
        event.setTenantId(eventBaseEntity.getTenantId());
        event.setCtime(eventBaseEntity.getCtime());
        event.setDataSource(eventBaseEntity.getDatasource());
        event.setDataId(eventBaseEntity.getDataId());
        event.setCategory(eventBaseEntity.getCategory());
        event.setText(eventBaseEntity.getText());
        event.setContext(eventBaseEntity.getContext());
        event.setTrigger(eventBaseEntity.getTrigger());
        event.setDampening(eventBaseEntity.getDampening());
        event.setEvalSets(eventBaseEntity.getEvalSets());
        event.setFacts(eventBaseEntity.getFacts());
    }
}
