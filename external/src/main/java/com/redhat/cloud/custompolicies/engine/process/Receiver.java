package com.redhat.cloud.custompolicies.engine.process;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.api.services.AlertsService;
import org.hawkular.commons.log.MsgLogger;
import org.hawkular.commons.log.MsgLogging;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * This is the main process for Custom Policies. It ingests data from Kafka, enriches it with information from
 * insights-host-inventory and then sends it for event processing in the engine.
 */
@ApplicationScoped
public class Receiver {
    private final MsgLogger log = MsgLogging.getMsgLogger(Receiver.class);

    public static final String INSIGHTS_REPORT_DATA_ID = "platform.inventory.host-egress";

    public static final String CATEGORY_NAME = "insight_report";
    public static final String INSIGHT_ID_FIELD = "insights_id";

    private static final String TENANT_ID_FIELD = "account";
    private static final String SYSTEM_PROFILE_FIELD = "system_profile";
    private static final String NETWORK_INTERFACES_FIELD = "network_interfaces";
    private static final String YUM_REPOS_FIELD = "yum_repos";
    private static final String DISPLAY_NAME_FIELD = "display_name";
    private static final String NAME_FIELD = "name";

    @ConfigProperty(name = "engine.receiver.store-events")
    boolean storeEvents;

    @Inject
    AlertsService alertsService;

    @Inject
    @Metric(absolute = true, name = "messages.incoming.host-egress.count")
    Counter incomingMessagesCount;

    @Incoming("kafka-hosts")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> processAsync(Message<JsonObject> input) {
        return CompletableFuture.supplyAsync(() -> {
            incomingMessagesCount.inc();
            JsonObject payload = input.getPayload();
            log.tracef("Received message, input payload: %s", payload);
            return payload;
        }).thenApplyAsync(json -> {
            String tenantId = json.getString(TENANT_ID_FIELD);
            String insightsId = json.getString(INSIGHT_ID_FIELD);
            String displayName = json.getString(DISPLAY_NAME_FIELD);

            String text = String.format("host-egress report %s for %s", insightsId, displayName);

            Event event = new Event(tenantId, UUID.randomUUID().toString(), INSIGHTS_REPORT_DATA_ID, CATEGORY_NAME, text);
            // Indexed searchable events
            Map<String, String> tagsMap = new HashMap<>();
            tagsMap.put(DISPLAY_NAME_FIELD, displayName);
            tagsMap.put(INSIGHT_ID_FIELD, insightsId);
            event.setTags(tagsMap);

            // Additional context for processing
            Map<String, String> contextMap = new HashMap<>();
            event.setContext(contextMap);

            JsonObject sp = json.getJsonObject(SYSTEM_PROFILE_FIELD);
            event.setFacts(parseSystemProfile(sp));
            return event;
        }).thenAcceptAsync(event -> {
            try {
                List<Event> eventList = new ArrayList<>(1);
                eventList.add(event);
                if (storeEvents) {
                    alertsService.addEvents(eventList);
                } else {
                    alertsService.sendEvents(eventList);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenApplyAsync(aVoid -> {
            input.ack();
            return null;
        });
    }

    /**
     * parseSystemProfile extracts certain parts of the input JSON and modifies them for easier use
     */
    static Map<String, Object> parseSystemProfile(JsonObject json) {
        if(json == null) {
            return new HashMap<>();
        }
        Map<String, Object> facts = json.getMap();

        JsonArray networkInterfaces = json.getJsonArray(NETWORK_INTERFACES_FIELD);
        if(networkInterfaces != null) {
            facts.put(NETWORK_INTERFACES_FIELD, namedObjectsToMap(networkInterfaces));
        }

        JsonArray yumRepos = json.getJsonArray(YUM_REPOS_FIELD);
        if(yumRepos != null) {
            facts.put(YUM_REPOS_FIELD, namedObjectsToMap(yumRepos));
        }

        return facts;
    }

    static Map<String, Object> namedObjectsToMap(JsonArray objectArray) {
        Map<String, Object> arrayObjectKey = new HashMap<>();
        for (Object o : objectArray) {
            JsonObject json = (JsonObject) o;
            String name = json.getString(NAME_FIELD);
            if (name == null || name.isEmpty()) {
                continue;
            }
            arrayObjectKey.put(name, json.getMap());
        }
        return arrayObjectKey;
    }
}
