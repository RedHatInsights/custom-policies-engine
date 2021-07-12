package com.redhat.cloud.policies.engine;

import io.quarkus.arc.properties.IfBuildProperty;
import org.hawkular.alerts.api.services.AlertsService;
import org.hawkular.alerts.engine.impl.ispn.IspnAlertsServiceImpl;

import javax.inject.Singleton;

import static com.redhat.cloud.policies.engine.history.alert.PoliciesHistoryAlertsService.POLICIES_HISTORY_ALERTS_SERVICE_ENABLED_CONF_KEY;

public class IspnAlertsServiceImplProducer {

    @Singleton
    @IfBuildProperty(name = POLICIES_HISTORY_ALERTS_SERVICE_ENABLED_CONF_KEY, stringValue = "false")
    public AlertsService getActionsService() {
        return new IspnAlertsServiceImpl();
    }
}
