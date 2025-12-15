package com.jd.live.agent.plugin.registry.nacos.v3_0.config;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.filter.IConfigFilter;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.FuzzyWatchEventWatcher;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.governance.subscription.config.gray.ConfigFetcher;
import com.jd.live.agent.governance.subscription.config.gray.ConfigKey;
import com.jd.live.agent.governance.subscription.config.gray.ConfigWatcher;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NacosConfigService implements ConfigService {

    private final ConfigService delegate;

    private final Application application;

    private final ObjectParser json;

    private final Map<ConfigKey, NacosConfigWatcher> watchers = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final AtomicBoolean started = new AtomicBoolean();

    public NacosConfigService(ConfigService delegate, Application application, ObjectParser json) {
        this.delegate = delegate;
        this.application = application;
        this.json = json;
    }

    public boolean isStarted() {
        return started.get();
    }

    @Override
    public String getConfig(String dataId, String group, long timeoutMs) throws NacosException {
        ConfigKey key = new ConfigKey(dataId, group);
        ConfigFetcher<ConfigService> fetcher = watchers.get(key);
        fetcher = fetcher == null ? new NacosConfigFetcher(delegate, key, application, json) : fetcher;
        try {
            return fetcher.getConfig(timeoutMs);
        } catch (Exception e) {
            throw toNacosException(e);
        }
    }

    @Override
    public String getConfigAndSignListener(String dataId, String group, long timeoutMs, Listener listener) throws NacosException {
        addListener(dataId, group, listener);
        return getConfig(dataId, group, timeoutMs);
    }

    @Override
    public void addListener(String dataId, String group, Listener listener) throws NacosException {
        if (dataId == null || dataId.isEmpty() || listener == null) {
            return;
        }
        ConfigKey key = new ConfigKey(dataId, group);
        AtomicReference<Exception> ref = new AtomicReference<>();
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            if (!isStarted()) {
                throw new NacosException(NacosException.SERVER_ERROR, "Config service is already shutdown.");
            }
            watchers.compute(key, (k, v) -> {
                if (v == null) {
                    NacosConfigWatcher watcher = new NacosConfigWatcher(delegate, k, application, json);
                    try {
                        watcher.addListener(listener);
                        watcher.subscribe();
                        return watcher;
                    } catch (Exception e) {
                        ref.set(e);
                        return null;
                    }
                } else {
                    v.addListener(listener);
                    return v;
                }
            });
        } finally {
            readLock.unlock();
        }
        Exception e = ref.get();
        if (e != null) {
            throw toNacosException(e);
        }
    }

    @Override
    public boolean publishConfig(String dataId, String group, String content) throws NacosException {
        return delegate.publishConfig(dataId, group, content);
    }

    @Override
    public boolean publishConfig(String dataId, String group, String content, String type) throws NacosException {
        return delegate.publishConfig(dataId, group, content, type);
    }

    @Override
    public boolean publishConfigCas(String dataId, String group, String content, String casMd5) throws NacosException {
        return delegate.publishConfigCas(dataId, group, content, casMd5);
    }

    @Override
    public boolean publishConfigCas(String dataId, String group, String content, String casMd5, String type) throws NacosException {
        return delegate.publishConfigCas(dataId, group, content, casMd5, type);
    }

    @Override
    public boolean removeConfig(String dataId, String group) throws NacosException {
        return delegate.removeConfig(dataId, group);
    }

    @Override
    public void removeListener(String dataId, String group, Listener listener) {
        if (dataId == null || dataId.isEmpty() || listener == null) {
            return;
        }
        ConfigKey key = new ConfigKey(dataId, group);
        AtomicReference<NacosConfigWatcher> ref = new AtomicReference<>();
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            watchers.computeIfPresent(key, (k, v) -> {
                if (v.removeListener(listener) && v.isEmpty()) {
                    ref.set(v);
                    v = null;
                }
                return v;
            });
        } finally {
            readLock.unlock();
        }
        NacosConfigWatcher watcher = ref.get();
        if (watcher != null) {
            watcher.unsubscribe();
        }
    }

    @Override
    public String getServerStatus() {
        return delegate.getServerStatus();
    }

    @Override
    public void addConfigFilter(IConfigFilter configFilter) {
        delegate.addConfigFilter(configFilter);
    }

    @Override
    public void fuzzyWatch(String groupNamePattern, FuzzyWatchEventWatcher watcher) throws NacosException {
        delegate.fuzzyWatch(groupNamePattern, watcher);
    }

    @Override
    public void fuzzyWatch(String dataIdPattern, String groupNamePattern, FuzzyWatchEventWatcher watcher) throws NacosException {
        delegate.fuzzyWatch(dataIdPattern, groupNamePattern, watcher);
    }

    @Override
    public Future<Set<String>> fuzzyWatchWithGroupKeys(String groupNamePattern, FuzzyWatchEventWatcher watcher) throws NacosException {
        return delegate.fuzzyWatchWithGroupKeys(groupNamePattern, watcher);
    }

    @Override
    public Future<Set<String>> fuzzyWatchWithGroupKeys(String dataIdPattern, String groupNamePattern, FuzzyWatchEventWatcher watcher) throws NacosException {
        return delegate.fuzzyWatchWithGroupKeys(dataIdPattern, groupNamePattern, watcher);
    }

    @Override
    public void cancelFuzzyWatch(String groupNamePattern, FuzzyWatchEventWatcher watcher) throws NacosException {
        delegate.cancelFuzzyWatch(groupNamePattern, watcher);
    }

    @Override
    public void cancelFuzzyWatch(String dataIdPattern, String groupNamePattern, FuzzyWatchEventWatcher watcher) throws NacosException {
        delegate.cancelFuzzyWatch(dataIdPattern, groupNamePattern, watcher);
    }

    @Override
    public void shutDown() throws NacosException {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (started.compareAndSet(true, false)) {
                watchers.forEach((k, v) -> v.unsubscribe());
                delegate.shutDown();
            }
        } finally {
            writeLock.unlock();
        }
    }

    private NacosException toNacosException(Exception e) {
        if (e instanceof NacosException) {
            return (NacosException) e;
        }
        return new NacosException(NacosException.SERVER_ERROR, e.getMessage(), e);
    }

    /**
     * Nacos configuration fetcher that retrieves configuration data from Nacos server.
     */
    private static class NacosConfigFetcher extends ConfigFetcher<ConfigService> {

        NacosConfigFetcher(ConfigService client, ConfigKey key, Application application, ObjectParser json) {
            super(client, key, application, json);
        }

        @Override
        protected String doGetConfig(ConfigKey key, long timeout) throws Exception {
            return getClient().getConfig(key.getName(), key.getGroup(), timeout);
        }
    }

    /**
     * Watches for configuration changes in Nacos, handling both normal and beta configurations.
     * Automatically switches between release/beta configs based on policy matching.
     */
    private static class NacosConfigWatcher extends ConfigWatcher<ConfigService, Listener> {

        NacosConfigWatcher(ConfigService client, ConfigKey key, Application application, ObjectParser json) {
            super(client, key, application, json);
        }

        @Override
        protected Listener createOnPolicyListener() {
            return new AbstractListener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    try {
                        onUpdatePolicy(configInfo);
                    } catch (Exception e) {
                        logger.error("Failed to update policy {}, caused by {}", keyPolicy, e.getMessage(), e);
                    }
                }
            };
        }

        @Override
        protected Listener createOnUpdateListener() {
            return new AbstractListener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    onUpdateConfig(configInfo);
                }
            };
        }

        @Override
        protected void doSubscribe(ConfigKey key, Listener listener) throws Exception {
            getClient().addListener(key.getName(), key.getGroup(), listener);
        }

        @Override
        protected void doUnsubscribe(ConfigKey key, Listener listener) {
            getClient().removeListener(key.getName(), key.getGroup(), listener);
        }

        @Override
        protected String doGetConfig(ConfigKey key, long timeout) throws Exception {
            return getClient().getConfig(key.getName(), key.getGroup(), timeout);
        }

        @Override
        protected void doUpdateConfig(Listener listener, String value) {
            listener.receiveConfigInfo(value);
        }
    }
}
