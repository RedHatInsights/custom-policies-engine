package com.redhat.cloud.policies.engine.actions;

import org.hawkular.alerts.actions.api.ActionMessage;
import org.hawkular.alerts.actions.api.ActionPluginListener;
import org.hawkular.alerts.actions.api.model.StandaloneActionMessage;
import org.hawkular.alerts.api.model.action.Action;
import org.hawkular.alerts.api.services.ActionListener;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.hawkular.alerts.log.AlertingLogger;
import org.hawkular.alerts.log.MsgLogging;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class QuarkusActionPluginListener implements ActionListener {
    private static final AlertingLogger log = MsgLogging.getMsgLogger(AlertingLogger.class, QuarkusActionPluginListener.class);

    @Inject
    DefinitionsService definitions;

    ExecutorService executorService;

    private Map<String, ActionPluginListener> plugins;

    public QuarkusActionPluginListener() {
        this.plugins = new HashMap<>();
        this.executorService = Executors.newCachedThreadPool();
    }

    public void addPlugin(String pluginKey, ActionPluginListener listener) {
        this.plugins.put(pluginKey, listener);
    }

    @Override
    public void process(Action action) {
        if (plugins.isEmpty()) {
            log.warnNoPluginsFound();
            return;
        }
        if (action == null || action.getActionPlugin() == null) {
            log.warnMessageReceivedWithoutPluginInfo();
            return;
        }

        try {
            String actionPlugin = action.getActionPlugin();
            final ActionPluginListener plugin = plugins.get(actionPlugin);
            if (plugin == null) {
                if (log.isDebugEnabled()) {
                    log.debugf("Received action [%s] but no ActionPluginListener found on this deployment", actionPlugin);
                }
                return;
            }

            ActionMessage pluginMessage = new StandaloneActionMessage(action);
            try {
                plugin.process(pluginMessage);
            } catch (Exception e) {
                log.debugf("Error processing action: %s", action.getActionPlugin(), e);
                log.errorProcessingAction(e.getMessage());
            }
        } catch (Exception e) {
            log.debugf("Error setting up action processing: %s", action.getActionPlugin(), e);
            log.errorProcessingAction(e.getMessage());
        }
    }

    @Override
    public void flush() {
        for (Map.Entry<String, ActionPluginListener> pluginEntry : plugins.entrySet()) {
            ActionPluginListener plugin = pluginEntry.getValue();
            log.debugf("Flushing %s\n", pluginEntry.getKey());
            plugin.flush();
        }
    }

    public void close() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("StandaloneActionPluginListener - [")
                .append(String.join(",", plugins.keySet()))
                .append("] plugins")
                .toString();
    }
}
