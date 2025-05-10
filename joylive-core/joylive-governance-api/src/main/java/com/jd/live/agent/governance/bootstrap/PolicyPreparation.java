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
package com.jd.live.agent.governance.bootstrap;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.LiveSpace;

/**
 * Prepares and validates governance policies during application startup.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Waits for policy supervisor readiness
 *   <li>Logs application location and enabled features
 *   <li>Validates live and lane configuration
 * </ul>
 *
 * @since 1.6.0
 */
@Injectable
@Extension(value = "PolicyPreparation", order = AppListener.ORDER_POLICY_PREPARATION)
public class PolicyPreparation extends AppListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PolicyPreparation.class);

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Config(GovernanceConfig.CONFIG_LIVE_ENABLED)
    private boolean liveEnabled;

    @Config(GovernanceConfig.CONFIG_LANE_ENABLED)
    private boolean laneEnabled;

    @Config(GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED)
    private boolean flowControlEnabled;

    @Config(GovernanceConfig.CONFIG_GOVERN_MQ_ENABLED)
    private boolean mqEnabled;

    @Config(GovernanceConfig.CONFIG_PROTECT_ENABLED)
    private boolean protectEnabled;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    @Override
    public void onStarted(AppContext context) {
        policySupervisor.waitReady();
    }

    @Override
    public void onReady(AppContext context) {
        printLocation();
        printFeatures();
        validateLiveConfig(policySupervisor.getPolicy(), application.getLocation());
        validateLaneConfig(policySupervisor.getPolicy(), application.getLocation());
    }

    /**
     * Logs the application's current location.
     */
    private void printLocation() {
        logger.info(application.getLocation().toString());
    }

    /**
     * Logs the status of all governance features including:
     * <ul>
     *   <li>Flow control</li>
     *   <li>Lanes (with fallback status)</li>
     *   <li>Config center</li>
     *   <li>Registry</li>
     *   <li>Live (with fallback status)</li>
     *   <li>Message queue</li>
     *   <li>Database protection</li>
     * </ul>
     */
    private void printFeatures() {
        StringBuilder builder = new StringBuilder(256);
        builder.append("Features:\n")
                // flow control feature
                .append("Feature flowcontrol is ").append(flowControlEnabled ? "enabled" : "disabled").append("\n")
                .append("Feature lane is ").append(laneEnabled ? "enabled" : "disabled").append(", and fallbackLocationIfNoSpace is ")
                .append(governanceConfig.getLaneConfig().isFallbackLocationIfNoSpace() ? "enabled" : "disabled").append("\n")
                .append("Feature config center is ").append(governanceConfig.getConfigCenterConfig().isEnabled() ? "enabled" : "disabled").append("\n")
                .append("Feature registry is ").append(governanceConfig.getRegistryConfig().isEnabled() ? "enabled" : "disabled").append("\n")
                // live feature
                .append("Feature live is ").append(liveEnabled ? "enabled" : "disabled").append(", and fallbackLocationIfNoSpace is ")
                .append(governanceConfig.getLiveConfig().isFallbackLocationIfNoSpace() ? "enabled" : "disabled").append("\n")
                .append("Feature mq is ").append(mqEnabled ? "enabled" : "disabled").append("\n")
                .append("Feature database protect is ").append(protectEnabled ? "enabled" : "disabled");
        logger.info(builder.toString());
    }

    /**
     * Validates live configuration requirements:
     * <ul>
     *   <li>Checks liveSpaceId exists and matches policy</li>
     *   <li>Verifies unit is configured</li>
     *   <li>Validates cell configuration</li>
     * </ul>
     *
     * @param policy   Current governance policy
     * @param location Application location to validate
     */
    private void validateLiveConfig(GovernancePolicy policy, Location location) {
        if (!laneEnabled) {
            return;
        }
        LiveSpace liveSpace = policy != null ? policy.getLocalLiveSpace() : null;
        if (location.getLiveSpaceId() == null || location.getLiveSpaceId().isEmpty()) {
            logger.error("Location is missing liveSpaceId for feature live");
        } else if (liveSpace == null) {
            logger.error("LiveSpace {} is not found for feature live", location.getLiveSpaceId());
        }
        if (location.getUnit() == null || location.getUnit().isEmpty()) {
            logger.error("Location is missing unit for feature live");
        } else if (liveSpace != null && liveSpace.getLocalUnit() == null) {
            logger.error("Unit {} is not found for feature live", location.getUnit());
        }
        if (location.getCell() == null || location.getCell().isEmpty()) {
            logger.error("Location is missing cell for feature live");
        } else if (liveSpace != null && liveSpace.getLocalCell() == null) {
            logger.error("Cell {} is not found for feature live", location.getUnit());
        }
    }

    /**
     * Validates lane configuration requirements:
     * <ul>
     *   <li>Checks laneSpaceId exists and matches policy</li>
     *   <li>Verifies lane is properly configured</li>
     * </ul>
     *
     * @param policy   Current governance policy
     * @param location Application location to validate
     */
    private void validateLaneConfig(GovernancePolicy policy, Location location) {
        if (!laneEnabled) {
            return;
        }
        LaneSpace laneSpace = policy != null ? policy.getLocalLaneSpace() : null;
        if (location.getLaneSpaceId() == null || location.getLaneSpaceId().isEmpty()) {
            logger.error("Location is missing laneSpaceId for feature lane");
        } else if (laneSpace == null) {
            logger.error("LaneSpace {} is not found for feature lane", location.getLaneSpaceId());
        }
        if (location.getLane() == null || location.getLane().isEmpty()) {
            logger.error("Location is missing lane for feature lane");
        } else if (laneSpace != null && laneSpace.getCurrentLane() == null) {
            logger.error("Lane {} is not found for feature lane", location.getUnit());
        }
    }


}
