package com.redhat.cloud.policies.engine.actions.plugins;

import com.redhat.cloud.notifications.ingress.Action;
import com.redhat.cloud.policies.engine.actions.plugins.notification.PoliciesPayloadBuilder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.hawkular.alerts.actions.api.ActionMessage;
import org.hawkular.alerts.actions.api.ActionPluginListener;
import org.hawkular.alerts.actions.api.Plugin;
import org.hawkular.alerts.api.model.condition.ConditionEval;
import org.hawkular.alerts.api.model.condition.EventConditionEval;
import org.hawkular.alerts.api.model.trigger.Trigger;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

/**
 * NotificationActionPluginListener sends a JSON encoded message in the format understood by the
 * notifications-backend in cloud.redhat.com. The encoding schema is defined in Avro and generated during
 * the compile.
 */
@Plugin(name = "notification")
@Dependent
public class NotificationActionPluginListener implements ActionPluginListener {
    public static final String BUNDLE_NAME = "rhel";
    public static final String APP_NAME = "policies";
    public static final String EVENT_TYPE_NAME = "policy-triggered";

    private final Logger log = Logger.getLogger(this.getClass().getName());
    private final ConcurrentSkipListMap<String, Action> notifyBuffer = new ConcurrentSkipListMap<>();

    @Inject
    @Channel("webhook")
    @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
    Emitter<String> channel;

    @Inject
    @Metric(absolute = true, name = "engine.actions.notifications.processed")
    Counter messagesCount;

    @Inject
    @Metric(absolute = true, name = "engine.actions.notifications.aggregated")
    Counter messagesAggregated;

    @Override
    public void process(ActionMessage actionMessage) throws Exception {
        messagesCount.inc();
        Action notificationAction = new Action();
        notificationAction.setEventType(EVENT_TYPE_NAME);
        notificationAction.setApplication(APP_NAME);
        notificationAction.setBundle(BUNDLE_NAME);
        notificationAction.setTimestamp(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(actionMessage.getAction().getCtime()), ZoneId.systemDefault())
        );

        PoliciesPayloadBuilder payloadBuilder = new PoliciesPayloadBuilder();

        for (Map.Entry<String, String> tagEntry : actionMessage.getAction().getEvent().getTags().entries()) {
            String value = tagEntry.getValue();
            if (value == null) {
                // Same behavior as previously with JsonObjectNoNullSerializer with old hooks
                value = "";
            }
            payloadBuilder.addTag(tagEntry.getKey(), value);
        }

        notificationAction.setAccountId(actionMessage.getAction().getTenantId());

        // Add the wanted properties here..
        Trigger trigger = actionMessage.getAction().getEvent().getTrigger();

        payloadBuilder.setPolicyId(trigger.getId())
                .setPolicyName(trigger.getName())
                .setPolicyDescription(trigger.getDescription());

        for (Set<ConditionEval> evalSet : actionMessage.getAction().getEvent().getEvalSets()) {
            for (ConditionEval conditionEval : evalSet) {
                if (conditionEval instanceof EventConditionEval) {
                    EventConditionEval eventEval = (EventConditionEval) conditionEval;
                    payloadBuilder.setPolicyCondition(eventEval.getCondition().getExpression())
                            .setSystemCheckIn(LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(eventEval.getContext().get("check_in"))))
                            .setInsightsId(eventEval.getContext().get("insights_id"))
                            .setDisplayName(eventEval.getValue().getTags().get("display_name").iterator().next());

                    String name = actionMessage.getAction().getEvent().getTrigger().getName();
                    String triggerId = actionMessage.getAction().getEvent().getTrigger().getId();

                    payloadBuilder.addTrigger(triggerId, name);
                }
            }
        }
        Map payload = payloadBuilder.build();
        notificationAction.setPayload(payload);

        String key = payload.get("insights_id").toString() + payload.get("system_check_in").toString();
        notifyBuffer.merge(key, notificationAction,(existing, addition) -> {
            ((List<PoliciesPayloadBuilder.Tag>)existing.getPayload().get("tags")).addAll((List<PoliciesPayloadBuilder.Tag>)addition.getPayload().get("tags"));
            ((Map<String, String>)existing.getPayload().get("triggers")).putAll((Map<String, String>)addition.getPayload().get("triggers"));
            return existing;
        });
        // channel.send(serializeAction(notificationAction));
    }

    @Override
    public void flush() {
        log.fine(() -> String.format("Starting flush of %d email messages", notifyBuffer.size()));

        while (true) {
            Map.Entry<String, Action> notificationEntry = notifyBuffer.pollFirstEntry();
            if (notificationEntry == null) {
                break;
            }

            Action action = notificationEntry.getValue();
            try {
                channel.send(serializeAction(action));
                messagesAggregated.inc();
            } catch (IOException ex) {
                log.warning(() -> "Failed to serialize action for accountId" + action.getAccountId());
            }

        }
    }

    @Override
    public Set<String> getProperties() {
        Set<String> properties = new HashSet<>();
        properties.add("endpoint_id");
        return properties;
    }

    @Override
    public Map<String, String> getDefaultProperties() {
        Map<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("_managed", "true");
        defaultProperties.put("endpoint_id", "");
        return defaultProperties;
    }

    public static String serializeAction(Action action) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(Action.getClassSchema(), baos);
        DatumWriter<Action> writer = new SpecificDatumWriter<>(Action.class);
        writer.write(action, jsonEncoder);
        jsonEncoder.flush();

        return baos.toString(StandardCharsets.UTF_8);
    }
}
