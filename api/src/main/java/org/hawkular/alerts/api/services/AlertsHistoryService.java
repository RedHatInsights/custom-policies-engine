package org.hawkular.alerts.api.services;

import org.hawkular.alerts.api.model.event.Alert;

public interface AlertsHistoryService {

    void put(Alert alert);
}
