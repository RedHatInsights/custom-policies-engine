package com.redhat.cloud.policies.engine.history;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hawkular.alerts.api.model.event.Alert;
import org.hawkular.alerts.api.services.AlertsHistoryService;
import org.hibernate.Session;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Iterator;
import java.util.Optional;

@ApplicationScoped
public class AlertsHistoryServiceImpl implements AlertsHistoryService {

    private static final Logger LOGGER = Logger.getLogger(AlertsHistoryServiceImpl.class);
    private static final String INVENTORY_ID_TAG_KEY = "inventory_id";
    private static final String DISPLAY_NAME_TAG_KEY = "display_name";

    @ConfigProperty(name = "engine.alerts-history-service.enabled", defaultValue = "false")
    boolean enabled;

    @Inject
    Session session;

    void logAtStartup(@Observes StartupEvent event) {
        if (enabled) {
            LOGGER.info("The alerts history service is enabled. The history will be persisted into the database.");
        } else {
            LOGGER.info("The alerts history service is disabled. The history won't be persisted into the database.");
        }
    }

    @Override
    @Transactional
    @ActivateRequestContext
    public void put(Alert alert) {
        if (enabled) {
            try {
                LOGGER.debugf("Creating %s from %s", AlertsHistoryEntry.class.getSimpleName(), alert);
                AlertsHistoryEntry historyEntry = new AlertsHistoryEntry();
                historyEntry.setTenantId(alert.getTenantId());
                historyEntry.setPolicyId(alert.getTriggerId());
                historyEntry.setCtime(alert.getCtime());
                getSingleTagValue(alert, INVENTORY_ID_TAG_KEY).ifPresent(historyEntry::setHostId);
                getSingleTagValue(alert, DISPLAY_NAME_TAG_KEY).ifPresent(historyEntry::setHostName);
                session.persist(historyEntry);
                LOGGER.debugf("Persisted %s", historyEntry);
            } catch (Exception e) {
                LOGGER.error("Alerts history entry persist failed", e);
            }
        }
    }

    private static Optional<String> getSingleTagValue(Alert alert, String tagKey) {
        Iterator<String> iterator = alert.getTags().get(tagKey).iterator();
        if (iterator.hasNext()) {
            return Optional.ofNullable(iterator.next());
        } else {
            return Optional.empty();
        }
    }
}
