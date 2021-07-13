package org.hawkular.alerts;

import com.redhat.cloud.policies.engine.actions.QuarkusActionPluginRegister;
import io.quarkus.runtime.LaunchMode;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hawkular.alerts.api.services.ActionsService;
import org.hawkular.alerts.api.services.AlertsHistoryService;
import org.hawkular.alerts.api.services.AlertsService;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.hawkular.alerts.api.services.StatusService;
import org.hawkular.alerts.engine.cache.ActionsCacheManager;
import org.hawkular.alerts.engine.cache.IspnCacheManager;
import org.hawkular.alerts.engine.cache.PublishCacheManager;
import org.hawkular.alerts.engine.impl.AlertsContext;
import org.hawkular.alerts.engine.impl.AlertsEngineImpl;
import org.hawkular.alerts.engine.impl.DataDrivenGroupCacheManager;
import org.hawkular.alerts.engine.impl.DroolsRulesEngineImpl;
import org.hawkular.alerts.engine.impl.ExtensionsServiceImpl;
import org.hawkular.alerts.engine.impl.IncomingDataManagerImpl;
import org.hawkular.alerts.engine.impl.PartitionManagerImpl;
import org.hawkular.alerts.engine.impl.StatusServiceImpl;
import org.hawkular.alerts.engine.impl.ispn.IspnActionsServiceImpl;
import org.hawkular.alerts.engine.impl.ispn.IspnAdminService;
import org.hawkular.alerts.engine.impl.ispn.IspnAlertsServiceImpl;
import org.hawkular.alerts.engine.impl.ispn.IspnDefinitionsServiceImpl;
import org.hawkular.alerts.filter.CacheClient;
import org.hawkular.alerts.log.MsgLogger;
import org.hawkular.alerts.log.MsgLogging;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.query.impl.massindex.DistributedExecutorMassIndexer;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hawkular.alerts.api.util.Util.isEmpty;

/**
 * Factory helper for standalone use cases.
 *
 * @author Lucas Ponce
 */
@Singleton
public class AlertsStandalone {
    private static final MsgLogger log = MsgLogging.getMsgLogger(AlertsStandalone.class);
    private static ExecutorService executor;

    @Inject
    QuarkusActionPluginRegister pluginRegister;

    @Inject
    AlertsHistoryService alertsHistoryService;

    //    @ConfigProperty(name = "engine.backend.ispn.reindex", defaultValue = "false")
    private boolean ispnReindex;

    private ActionsCacheManager actionsCacheManager;
    private AlertsContext alertsContext;
    private AlertsEngineImpl engine;
    private CacheClient dataIdCache;
    private DataDrivenGroupCacheManager dataDrivenGroupCacheManager;
    private DroolsRulesEngineImpl rules;
    private EmbeddedCacheManager cacheManager;
    private ExtensionsServiceImpl extensions;
    private IncomingDataManagerImpl incoming;
    private IspnActionsServiceImpl ispnActions;
    private IspnAlertsServiceImpl ispnAlerts;
    private IspnDefinitionsServiceImpl ispnDefinitions;
    private StatusServiceImpl status;
    private PartitionManagerImpl partitionManager;
    private PublishCacheManager publishCacheManager;
    private IspnAdminService adminService;

    public AlertsStandalone() {
        log.info("Policies Engine uses Infinispan backend");

        if((LaunchMode.current() == LaunchMode.DEVELOPMENT || LaunchMode.current() == LaunchMode.TEST) && isEmpty(System.getProperty("hawkular.data"))) {
            System.setProperty("hawkular.data", "target/hawkular.data");
        }

        cacheManager = IspnCacheManager.getCacheManager();

        if (executor == null) {
            executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }

        dataIdCache = new CacheClient();
        rules = new DroolsRulesEngineImpl();
        engine = new AlertsEngineImpl();
        alertsContext = new AlertsContext();
        partitionManager = new PartitionManagerImpl();
        status = new StatusServiceImpl();
        extensions = new ExtensionsServiceImpl();
        dataDrivenGroupCacheManager = new DataDrivenGroupCacheManager();
        incoming = new IncomingDataManagerImpl();
        actionsCacheManager = new ActionsCacheManager();
        publishCacheManager = new PublishCacheManager();
        adminService = new IspnAdminService();

        ispnReindex = ConfigProvider.getConfig().getValue("engine.backend.ispn.reindex", Boolean.class);

        ispnActions = new IspnActionsServiceImpl();
        ispnAlerts = new IspnAlertsServiceImpl();
        ispnDefinitions = new IspnDefinitionsServiceImpl();

        ispnActions.setActionsCacheManager(actionsCacheManager);
        ispnActions.setAlertsContext(alertsContext);
        ispnActions.setDefinitions(ispnDefinitions);

        ispnAlerts.setActionsService(ispnActions);
        ispnAlerts.setAlertsEngine(engine);
        ispnAlerts.setDefinitionsService(ispnDefinitions);
        ispnAlerts.setIncomingDataManager(incoming);

        ispnDefinitions.setAlertsEngine(engine);
        ispnDefinitions.setAlertsContext(alertsContext);


        actionsCacheManager.setDefinitions(ispnDefinitions);
        actionsCacheManager.setGlobalActionsCache(cacheManager.getCache("globalActions"));

        alertsContext.setPartitionManager(partitionManager);

        dataDrivenGroupCacheManager.setDefinitions(ispnDefinitions);

        dataIdCache.setCache(cacheManager.getCache("publish"));

        engine.setActions(ispnActions);
        engine.setAlertsService(ispnAlerts);
        engine.setDefinitions(ispnDefinitions);
        engine.setExecutor(executor);
        engine.setExtensionsService(extensions);
        engine.setPartitionManager(partitionManager);
        engine.setRules(rules);

        incoming.setAlertsEngine(engine);
        incoming.setDataDrivenGroupCacheManager(dataDrivenGroupCacheManager);
        incoming.setDataIdCache(dataIdCache);
        incoming.setDefinitionsService(ispnDefinitions);
        incoming.setExecutor(executor);
        incoming.setPartitionManager(partitionManager);

        partitionManager.setDefinitionsService(ispnDefinitions);

        actionsCacheManager.setDefinitions(ispnDefinitions);
        actionsCacheManager.setGlobalActionsCache(cacheManager.getCache("globalActions"));

        publishCacheManager.setDefinitions(ispnDefinitions);
        publishCacheManager.setPublishCache(cacheManager.getCache("publish"));
        publishCacheManager.setPublishDataIdsCache(cacheManager.getCache("dataIds"));

        status.setPartitionManager(partitionManager);
    }

    @PostConstruct
    void postConstruct() {
        ispnAlerts.setAlertsHistoryService(alertsHistoryService);
    }

    public void init() {
        if (this.ispnReindex) {
            log.info("Reindexing of Infinispan [backend] started.");
            status.setReindexing(true);
            long startReindex = System.currentTimeMillis();
            SearchManager searchManager = Search
                    .getSearchManager(IspnCacheManager.getCacheManager().getCache("backend"));
            DistributedExecutorMassIndexer massIndexer = (DistributedExecutorMassIndexer) searchManager.getMassIndexer();
            // Lets block instead of async
            massIndexer.start();
            long stopReindex = System.currentTimeMillis();
            log.info("Reindexing of Infinispan [backend] completed in [" + (stopReindex - startReindex) + " ms]");
            ispnReindex = false;
            status.setReindexing(false);
        }
        // Initialization needs order and needs to be done after reindexing
        ispnAlerts.init();
        ispnDefinitions.init();
        ispnActions.init();
        adminService.init();

        partitionManager.init();
        alertsContext.init();
        dataDrivenGroupCacheManager.init();
        actionsCacheManager.init();
        publishCacheManager.init();
        extensions.init();
        engine.initServices();
    }

    public void stop() {
            engine.shutdown();
            partitionManager.shutdown();
            IspnCacheManager.stop();
    }

    public DefinitionsService getDefinitionsService() {
        return ispnDefinitions;
    }

    public AlertsService getAlertsService() {
        return ispnAlerts;
    }

    public ActionsService getActionsService() {
        return ispnActions;
    }

    @Produces
    public StatusService getStatusService() {
        return status;
    }

    @Produces
    public IspnAdminService getAdminService() {
        return adminService;
    }

    public boolean isReindexing() {
        return ispnReindex;
    }
}
