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
package com.jd.live.agent.governance.policy.listener;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigListener;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupervisor;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract class for a configuration listener that implements the ConfigListener interface.
 *
 * @param <T> The type of the parsed configuration value.
 */
public abstract class AbstractListener<T> implements ConfigListener {

    private static final Logger logger = LoggerFactory.getLogger(AbstractListener.class);

    /**
     * Maximum number of retries for status updates.
     */
    protected static final int UPDATE_MAX_RETRY = 100;

    /**
     * The class type of the items being listened for
     */
    protected final Class<T> type;

    /**
     * The policy supervisor responsible for managing and updating policies.
     */
    protected final PolicySupervisor supervisor;

    protected final ObjectParser parser;

    /**
     * Creates a new instance of the AbstractListener class.
     *
     * @param type       The class type of the items being listened for.
     * @param supervisor The policy supervisor to use for updating policies.
     * @param parser     The JSON parser to use for parsing event data.
     */
    public AbstractListener(Class<T> type, PolicySupervisor supervisor, ObjectParser parser) {
        this.type = type;
        this.supervisor = supervisor;
        this.parser = parser;
    }

    @Override
    public boolean onUpdate(ConfigEvent event) {
        try {
            return update(event);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Updates the policy based on the given configuration.
     *
     * @param event The configuration update.
     * @return true if the update was successful, false otherwise.
     */
    protected boolean update(ConfigEvent event) {
        for (int i = 0; i < UPDATE_MAX_RETRY; i++) {
            if (supervisor.update(policy -> newPolicy(policy, event))) {
                logger.info("Success " + event.getType().getName() + " " + event.getDescription());
                onSuccess(event);
                return true;
            }
        }

        return false;
    }

    /**
     * Creates a new policy based on the given policy and configuration.
     *
     * @param policy The existing policy.
     * @param event  The configuration update.
     * @return The new policy.
     */
    protected GovernancePolicy newPolicy(GovernancePolicy policy, ConfigEvent event) {
        GovernancePolicy result = policy == null ? new GovernancePolicy() : policy.copy();
        switch (event.getType()) {
            case DELETE_ITEM:
                deleteItem(result, event);
                break;
            case UPDATE_ITEM:
                updateItem(result, parseItem(event, type), event);
                break;
            case UPDATE_ALL:
                updateItems(result, parseList(event, type), event);
                break;
        }
        return result;
    }

    /**
     * Parses a configuration value as a list of objects of the specified type.
     *
     * @param event The configuration to parse.
     * @param type  The type of objects in the list.
     * @param <M>   The type of objects in the list.
     * @return A list of objects of the specified type, or an empty list if the configuration value is null or cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    protected <M> M parseItem(ConfigEvent event, Class<M> type) {
        Object value = event.getValue();
        if (value == null) {
            return null;
        } else if (type.isAssignableFrom(value.getClass())) {
            return (M) value;
        } else if (value instanceof String) {
            String str = ((String) value).trim();
            return str.isEmpty() ? null : parser.read(new StringReader(str), type);
        } else {
            return null;
        }
    }

    /**
     * Parses a configuration value as a list of objects of the specified type.
     *
     * @param event The configuration to parse.
     * @param type  The type of objects in the list.
     * @param <M>   The type of objects in the list.
     * @return A list of objects of the specified type, or an empty list if the configuration value is null or cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    protected <M> List<M> parseList(ConfigEvent event, Class<M> type) {
        Object value = event.getValue();
        if (value == null) {
            return new ArrayList<>();
        } else if (value instanceof List) {
            return (List<M>) value;
        } else if (value instanceof String) {
            String str = ((String) value).trim();
            return str.isEmpty() ? new ArrayList<>() : parser.read(new StringReader(str), new TypeReference<List<M>>() {
            });
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Updates the given policy with the parsed configuration value.
     *
     * @param policy The policy to update.
     * @param items  The parsed configuration value.
     * @param event  The original configuration update.
     */
    protected abstract void updateItems(GovernancePolicy policy, List<T> items, ConfigEvent event);

    /**
     * Updates the governance policy and a specific item based on the given config event.
     *
     * @param policy The governance policy to be updated.
     * @param item   The specific item to be updated.
     * @param event  The config event that triggered the update.
     */
    protected abstract void updateItem(GovernancePolicy policy, T item, ConfigEvent event);

    /**
     * Deletes the specified configuration from the policy.
     *
     * @param policy The policy to delete from.
     * @param event  The configuration to delete.
     */
    protected abstract void deleteItem(GovernancePolicy policy, ConfigEvent event);

    /**
     * Called when the policy update is successful.
     *
     * @param event The configuration update.
     */
    protected void onSuccess(ConfigEvent event) {

    }

}
