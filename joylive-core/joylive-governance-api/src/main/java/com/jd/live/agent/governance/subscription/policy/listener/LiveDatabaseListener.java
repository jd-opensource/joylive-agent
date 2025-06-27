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
package com.jd.live.agent.governance.subscription.policy.listener;

import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.db.LiveDatabaseGroup;
import com.jd.live.agent.governance.policy.live.db.LiveDatabaseSpec;
import com.jd.live.agent.governance.subscription.policy.PolicyEvent;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.filter;

/**
 * A listener class for live database configuration updates that extends the AbstractListener class.
 */
public class LiveDatabaseListener extends AbstractListener<LiveDatabaseSpec> {

    private final Publisher<DatabaseEvent> publisher;

    public LiveDatabaseListener(PolicySupervisor supervisor, ObjectParser parser, Publisher<DatabaseEvent> publisher) {
        super(LiveDatabaseSpec.class, supervisor, parser);
        this.publisher = publisher;
    }

    @Override
    protected void onSuccess(GovernancePolicy oldPolicy, GovernancePolicy newPolicy) {
        LiveDatabaseSpec oldSpace = oldPolicy == null ? null : oldPolicy.getLocalDatabaseSpec();
        LiveDatabaseSpec newSpace = newPolicy == null ? null : newPolicy.getLocalDatabaseSpec();
        List<LiveDatabaseGroup> oldGroups = oldSpace == null ? null : oldSpace.getGroups();
        List<LiveDatabaseGroup> newGroups = newSpace == null ? null : newSpace.getGroups();
        if (LiveDatabaseSpec.isChanged(oldGroups, newGroups)) {
            publisher.offer(new DatabaseEvent());
        }
    }

    @Override
    protected void updateItems(GovernancePolicy policy, List<LiveDatabaseSpec> items, PolicyEvent event) {
        policy.setDatabaseSpecs(items);
    }

    @Override
    protected void updateItem(GovernancePolicy policy, LiveDatabaseSpec item, PolicyEvent event) {
        // copy on write
        List<LiveDatabaseSpec> specs = policy.getDatabaseSpecs() == null ? new ArrayList<>() : new ArrayList<>(policy.getDatabaseSpecs());
        filter(specs, space -> !space.getId().equals(item.getId()));
        specs.add(item);
        policy.setDatabaseSpecs(specs);
    }

    @Override
    protected void deleteItem(GovernancePolicy policy, PolicyEvent event) {
        if (event.getName() == null) {
            return;
        }
        List<LiveSpace> spaces = policy.getLiveSpaces();
        if (spaces != null) {
            filter(spaces, space -> !space.getId().equals(event.getName()));
        }
    }
}
