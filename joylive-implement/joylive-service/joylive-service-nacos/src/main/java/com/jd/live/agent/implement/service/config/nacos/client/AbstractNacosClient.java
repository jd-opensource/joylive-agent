/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.implement.service.config.nacos.client;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.Executors;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.option.OptionSupplier;
import com.jd.live.agent.core.util.task.RetryVersionTask;
import com.jd.live.agent.core.util.task.RetryVersionTimerTask;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.probe.*;
import com.jd.live.agent.governance.probe.FailoverAddressList.SimpleAddressList;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.StringUtils.*;

/**
 * A client for interacting with the Nacos.
 */
public abstract class AbstractNacosClient<T extends OptionSupplier, M> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNacosClient.class);
    protected static final String KEY_AUTO_RECOVER = "autoRecover";
    protected static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";
    protected static final String KEY_INITIALIZATION_TIMEOUT = "initializationTimeout";
    protected static final String ENV_NACOS_AUTO_RECOVER = "NACOS_AUTO_RECOVER";
    protected static final String ENV_NACOS_CONNECTION_TIMEOUT = "NACOS_CONNECTION_TIMEOUT";
    protected static final String ENV_NACOS_INITIALIZATION_TIMEOUT = "NACOS_INITIALIZATION_TIMEOUT";
    protected static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    protected static final int DEFAULT_INITIALIZATION_TIMEOUT = 30000;

    protected final T config;
    protected final HealthProbe probe;
    protected final Timer timer;

    protected final int initializationTimeout;
    protected final boolean autoRecover;
    protected final List<String> servers;
    protected String server;
    protected final FailoverAddressList addressList;
    protected final Properties properties;

    protected volatile M client;

    protected final CountDownLatch connectLatch = new CountDownLatch(1);
    protected final AtomicBoolean started = new AtomicBoolean(false);
    protected final AtomicBoolean connected = new AtomicBoolean(false);
    protected final AtomicLong versions = new AtomicLong(0);
    protected final Predicate<RetryVersionTask> predicate = p -> started.get() && p.getVersion() == versions.get();
    protected final Supplier<Long> delay = () -> Timer.getRetryInterval(1000L, 3000L);

    public AbstractNacosClient(T config, HealthProbe probe, Timer timer) {
        this.config = config;
        this.probe = probe;
        this.timer = timer;
        Option option = config.getOption();
        this.autoRecover = Converts.getBoolean(option.getString(KEY_AUTO_RECOVER, System.getenv(ENV_NACOS_AUTO_RECOVER)), true);
        // keep url parameter to get grpc.port
        this.servers = splitList(getAddress(config), CHAR_SEMICOLON);
        this.server = servers.isEmpty() ? null : servers.get(0);
        this.initializationTimeout = getInitializationTimeout(option);
        this.addressList = new SimpleAddressList(servers);
        this.properties = convert(config);
    }

    /**
     * Starts Nacos client connection with timeout.
     *
     * @throws NacosException If connection fails or times out
     */
    protected void doStart() throws NacosException {
        if (started.compareAndSet(false, true)) {
            logger.info("Try detecting healthy nacos {}", join(servers));
            try {
                // wait for connected
                addDetectTask(0, false);
                if (initializationTimeout > 0 && !connectLatch.await(initializationTimeout, TimeUnit.MILLISECONDS)) {
                    logger.error("It's timeout to connect to nacos. {}", join(servers));
                    // cancel task.
                    throw new NacosException(NacosException.CLIENT_DISCONNECT, "It's timeout to connect to nacos.");
                }
            } catch (NacosException e) {
                started.set(false);
                throw e;
            } catch (InterruptedException e) {
                started.set(false);
                Thread.currentThread().interrupt();
                throw new NacosException(NacosException.CLIENT_DISCONNECT, "The nacos connecting thread is interrupted.");
            }
        }

    }

    /**
     * Closes the Nacos client connection.
     */
    protected void doClose() {
        if (started.compareAndSet(true, false)) {
            connected.set(false);
            close(client);
        }
    }

    /**
     * Gets initialization timeout from option or environment.
     *
     * @param option Configuration option
     * @return Positive timeout value
     */
    protected int getInitializationTimeout(Option option) {
        return getInitializationTimeout(option, true);
    }

    /**
     * Gets initialization timeout from option, environment or calculated default.
     * Handles both positive-only and regular integer conversion based on parameter.
     *
     * @param option   Configuration option containing timeout values
     * @param positive When true, enforces positive-only conversion
     * @return Timeout value (positive when requested)
     */
    protected int getInitializationTimeout(Option option, boolean positive) {
        String value = option.getString(KEY_INITIALIZATION_TIMEOUT, System.getenv(ENV_NACOS_INITIALIZATION_TIMEOUT));
        return positive ? Converts.getPositive(value, DEFAULT_INITIALIZATION_TIMEOUT) : Converts.getInteger(value, DEFAULT_INITIALIZATION_TIMEOUT);
    }

    /**
     * Gets server address from config.
     *
     * @param config Client configuration
     * @return Server address
     */
    protected abstract String getAddress(T config);

    /**
     * Creates new Nacos client instance.
     *
     * @return New client instance
     * @throws NacosException If client creation fails
     */
    protected abstract M createClient() throws NacosException;

    /**
     * Converts config to Properties.
     *
     * @param config Client configuration
     * @return Properties representation
     */
    protected abstract Properties convert(T config);

    /**
     * Schedules a new failover detection task with version control.
     *
     * @param delay Initial delay before first execution (milliseconds)
     */
    protected void addDetectTask(long delay, boolean connected) {
        FailoverDetectTask detect = new FailoverDetectTask(addressList, probe, 1, connected, new NacosDetectTaskListener());
        RetryVersionTimerTask task = new RetryVersionTimerTask("nacos.detect", detect, versions.get(), predicate, timer);
        // fast to reconnect when initialization
        task.delay(delay);
    }

    /**
     * Gracefully shuts down client.
     *
     * @param client The client to close
     */
    protected abstract void close(M client);

    /**
     * Handles disconnection by closing current config service
     * and immediately scheduling a failover detection task.
     */
    protected void onDisconnected() {
        if (connected.compareAndSet(true, false)) {
            logger.info("Nacos client is disconnected from {}, close and reconnect it.", server);
            versions.incrementAndGet();
            connected.set(false);
            disconnect();
            addDetectTask(Timer.getRetryInterval(1000, 3000L), true);
        }
    }

    /**
     * Reconnects to the current Nacos server address.
     *
     * @param address The address to connect to
     */
    protected void onDetected(String address) {
        reconnect(address);
    }

    /**
     * Closes the Nacos client connection.
     */
    private void disconnect() {
        close(client);
    }

    /**
     * Reconnects to Nacos server at given address.
     *
     * @param address Server address
     */
    protected void reconnect(String address) {
        try {
            doReconnect(address);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect to nacos " + address, e);
        }
    }

    /**
     * Performs full reconnection process including:
     *
     * @param address Server address
     * @throws Exception If reconnection fails
     */
    protected void doReconnect(String address) throws Exception {
        properties.put(PropertyKeyConst.SERVER_ADDR, address);
        // re-create client
        client = Executors.call(this.getClass().getClassLoader(), this::createClient);
        server = address;
        recover();
        // after on connected
        connected.set(true);
        connectLatch.countDown();
        failover(address);
    }

    /**
     * Recover.
     */
    protected abstract void recover();

    /**
     * Attempts to detect and recover connection to the preferred nacos server.
     *
     * @param address The current address.
     */
    private void failover(String address) {
        if (!autoRecover) {
            return;
        }
        String first = addressList.first();
        if (Objects.equals(address, first)) {
            return;
        }
        logger.info("Try detecting unhealthy preferred nacos {}...", first);
        FailoverRecoverTask execution = new FailoverRecoverTask(first, probe, 1, () -> {
            if (!Objects.equals(addressList.current(), first)) {
                logger.info("Try switching to the healthy preferred nacos {}.", first);
                // recover immediately
                connected.set(false);
                close(client);
                // reset preferred nacos
                addressList.reset();
                onDetected(addressList.current());
            }
        });
        RetryVersionTimerTask task = new RetryVersionTimerTask("nacos.recover", execution, versions.get(), predicate, timer);
        task.delay(Timer.getRetryInterval(1500, DEFAULT_CONNECTION_TIMEOUT));
    }

    /**
     * Listener for Nacos server detection tasks.
     * Handles connection success/failure events and manages ConfigService lifecycle.
     */
    private class NacosDetectTaskListener implements DetectTaskListener {
        @Override
        public void onSuccess() {
            String current = addressList.current();
            logger.info("Try connecting to healthy nacos {}", current);
            // reconnect to server
            onDetected(current);
        }

        @Override
        public void onFailure() {
            connectLatch.countDown();
        }
    }

}
