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
package com.jd.live.agent.plugin.registry.nacos.v1_4.util;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.naming.net.NamingProxy;
import com.alibaba.nacos.client.security.SecurityProxy;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;

/**
 * Security context utility for managing Nacos authentication.
 *
 * <p>Provides centralized management of security properties and
 * handles token refresh operations for Nacos client authentication.</p>
 */
public class SecurityContext {

    private static final Logger logger = LoggerFactory.getLogger(SecurityContext.class);

    private static final FieldAccessor SECURITY_PROXY = getAccessor(NamingProxy.class, "securityProxy");

    private static final FieldAccessor LAST_REFRESH_TIME = getAccessor(SecurityProxy.class, "lastRefreshTime");

    private static final FieldAccessor TOKEN_TTL = getAccessor(SecurityProxy.class, "tokenTtl");

    /**
     * Performs re-login operation to refresh authentication token.
     *
     * <p>Resets the last refresh time and triggers login with cached properties
     * if a valid token TTL is found.</p>
     *
     * @param proxy the naming client proxy
     */
    public static void reLogin(Object proxy) {
        try {
            SecurityProxy securityProxy = SECURITY_PROXY.get(proxy, SecurityProxy.class);
            Long ttl = TOKEN_TTL.get(securityProxy, Long.class);
            Long lastRefreshTime = LAST_REFRESH_TIME.get(securityProxy, Long.class);
            if (lastRefreshTime != null && lastRefreshTime > 0 && ttl != null && ttl > 0) {
                // reset last refresh time
                logger.info("403 no right exception, try to login again.");
                LAST_REFRESH_TIME.set(proxy, 0L);
            }
        } catch (Throwable e) {
            logger.error("Failed to relogin, caused by {} ", e.getMessage(), e);
        }
    }

    /**
     * Checks if the throwable indicates a permission denied error.
     *
     * <p>Examines NacosException and its nested causes to determine if the error
     * represents a "no right" (permission denied) condition. Handles both direct
     * NO_RIGHT errors and SERVER_ERROR wrapping NO_RIGHT exceptions.</p>
     *
     * @param throwable the exception to check
     * @return true if the exception indicates no permission, false otherwise
     */
    public static boolean isNoRight(Throwable throwable) {
        if (throwable instanceof NacosException) {
            NacosException e = ((NacosException) throwable);
            int errorCode = e.getErrCode();
            if (errorCode == NacosException.SERVER_ERROR) {
                if (e.getCause() != null && e.getCause() instanceof NacosException) {
                    e = (NacosException) e.getCause();
                    errorCode = e.getErrCode();
                }
            }
            if (NacosException.NO_RIGHT == errorCode) {
                return true;
            }
        }
        return false;
    }
}
