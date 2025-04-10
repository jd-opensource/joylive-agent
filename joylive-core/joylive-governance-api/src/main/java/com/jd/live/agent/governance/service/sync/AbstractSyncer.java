package com.jd.live.agent.governance.service.sync;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.AgentPath;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.config.SyncConfig;
import com.jd.live.agent.governance.service.AbstractPolicyService;
import lombok.Setter;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractNacosSyncer is responsible for create/close Nacos Service.
 */
public abstract class AbstractSyncer<K extends SyncKey, T> extends AbstractPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSyncer.class);

    @Setter
    @Inject(Application.COMPONENT_APPLICATION)
    protected Application application;

    @Inject(value = AgentPath.COMPONENT_AGENT_PATH, nullable = true)
    @Setter
    protected AgentPath agentPath;

    @Inject(ObjectParser.JSON)
    @Setter
    protected ObjectParser parser;

    protected Syncer<K, T> syncer;

    protected Template template;

    protected final Map<String, Subscription<K, T>> subscriptions = new ConcurrentHashMap<>();

    @Override
    protected CompletableFuture<Void> doStart() {
        try {
            syncer = createSyncer();
            template = createTemplate();
            startSync();
            return CompletableFuture.completedFuture(null);
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        stopSync();
        Close.instance().closeIfExists(syncer, v -> {
            subscriptions.clear();
            v.close();
        });
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the synchronization configuration for this service syncer.
     *
     * @return The synchronization configuration.
     */
    protected abstract SyncConfig getSyncConfig();

    /**
     * Creates a new instance of the SubscriptionSyncer class.
     *
     * @return A new instance of the SubscriptionSyncer class.
     */
    protected abstract Syncer<K, T> createSyncer();

    /**
     * Starts the synchronization process for the LanSpaces.
     *
     * @throws Exception If an error occurs during the synchronization process.
     */
    protected abstract void startSync() throws Exception;

    /**
     * Stops the synchronization process for the LanSpaces.
     */
    protected void stopSync() {
    }

    /**
     * Creates a new instance of the Template class.
     *
     * @return A new instance of the Template class.
     */
    protected Template createTemplate() {
        return null;
    }

    /**
     * Saves the given configuration content to a specified file under the agent's output directory.
     *
     * @param config    the configuration content to be saved (must not be {@code null})
     * @param directory the subdirectory path where the config should be saved (may be {@code null} or empty
     *                  to use the root output directory). This is relative to the agent's output directory.
     * @param name      the filename to use for saving the configuration (must not be {@code null} or empty)
     */
    protected void saveConfig(String config, String directory, String name) {
        if (agentPath == null) {
            return;
        }
        File file = directory != null && !directory.isEmpty()
                ? new File(agentPath.getOutputPath(), directory)
                : agentPath.getOutputPath();
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(file, name);
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(config);
            fw.flush();
        } catch (Throwable e) {
            logger.error("Failed to save config to file: {}, caused by {}", file.getPath(), e.getMessage());
        }
    }

    /**
     * Serializes an object to a string and saves it as a configuration file.
     *
     * @param obj       the object to be serialized and saved (must not be {@code null})
     * @param parser    the object parser used for serialization (must not be {@code null})
     * @param directory the target directory where the file will be saved (must not be {@code null} or empty)
     * @param name      the filename to use (must not be {@code null} or empty)
     */
    protected void saveConfig(Object obj, ObjectParser parser, String directory, String name) {
        // save config to local file
        if (obj != null) {
            StringWriter writer = new StringWriter(1024 * 5);
            parser.write(writer, obj);
            saveConfig(writer.toString(), directory, name);
        }
    }

}
