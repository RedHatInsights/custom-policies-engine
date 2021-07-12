package com.redhat.cloud.policies.engine;

import com.redhat.cloud.policies.engine.actions.QuarkusActionPluginRegister;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hawkular.alerts.AlertsStandalone;
import org.hawkular.alerts.api.services.ActionsService;
import org.hawkular.alerts.api.services.StatusService;
import org.hawkular.alerts.engine.impl.StatusServiceImpl;
import org.hawkular.alerts.log.MsgLogger;
import org.hawkular.alerts.log.MsgLogging;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import static org.hawkular.alerts.api.util.Util.isEmpty;

@Singleton
public class AlertStarter {

    public static MsgLogger LOGGER = MsgLogging.getMsgLogger(AlertStarter.class);
    private final static String BUILD_COMMIT_ENV_NAME = "OPENSHIFT_BUILD_COMMIT";

    static final String FILTER_REGEX = ".*(/health(/\\w+)?|/metrics|/hawkular/alerts/triggers\\?triggerIds=dummy) HTTP/[0-9].[0-9]\" 200.*\\n?";
    private static final Pattern pattern = Pattern.compile(FILTER_REGEX);

    @ConfigProperty(name = "quarkus.http.access-log.category")
    String loggerName;

    @Inject
    AlertsStandalone alerts;

    @Inject
    QuarkusActionPluginRegister pluginRegister;

    @Inject
    StatusService statusService;

    @Produces
    public ActionsService getActionsService() {
        return alerts.getActionsService();
    }

    void startApp(@Observes StartupEvent startup) {
        initAccessLogFilter();
        String commit = System.getenv(BUILD_COMMIT_ENV_NAME);
        if(!isEmpty(commit)) {
            LOGGER.infof("Starting Policies Engine, build: %s", commit);
        } else {
            LOGGER.info("Starting Policies Engine.");
        }
        initialize();
    }

    void stopApp(@Observes ShutdownEvent shutdown) {
        LOGGER.info("Shutting down Policies Engine.");
        alerts.stop();
    }

    void initialize() {
        // PluginRegister is already using CDI, but we want it to initialize after Alerts has started
        try {
            alerts.init();
            pluginRegister.init();
            ((StatusServiceImpl) statusService).setStarted(true);
            LOGGER.info("Started Policies Engine");
        } catch (Throwable t) {
            LOGGER.error("Engine start failed", t);
            alerts.stop();
        }
    }

    private void initAccessLogFilter() {
        java.util.logging.Logger accessLog = java.util.logging.Logger.getLogger(loggerName);
        accessLog.setFilter(record -> {
            final String logMessage = record.getMessage();
            Matcher matcher = pattern.matcher(logMessage);
            return !matcher.matches();
        });
    }
}
