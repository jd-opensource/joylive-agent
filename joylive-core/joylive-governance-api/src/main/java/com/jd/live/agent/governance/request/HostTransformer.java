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
package com.jd.live.agent.governance.request;

import java.util.Map;

/**
 * Transforms domain names.
 */
public interface HostTransformer {

    String KEY_UNIT = "unit";
    String KEY_LANE = "lane";
    String KEY_HOST = "host";

    /**
     * Transforms the given host.
     *
     * @param host the host to transform
     * @param ctx  the context
     * @return the transformed domain
     */
    String transform(String host, Map<String, Object> ctx);

    /**
     * Chains this transformer with another transformer.
     *
     * @param transformer the transformer to chain
     * @return the chained transformer
     */
    default HostTransformer then(HostTransformer transformer) {
        return ((host, context) -> {
            String newHost = transform(host, context);
            if (transformer == null) {
                return newHost;
            }
            context.put(KEY_HOST, newHost);
            return transformer.transform(newHost, context);
        });
    }

    /**
     * Transforms only the last domain a host name (before the first dot).
     */
    class LastDomainTransformer implements HostTransformer {

        private HostTransformer delegate;

        public LastDomainTransformer(HostTransformer delegate) {
            this.delegate = delegate;
        }

        @Override
        public String transform(String host, Map<String, Object> ctx) {
            if (host == null) {
                return null;
            }
            int pos = host.indexOf('.');
            if (pos == -1) {
                return delegate.transform(host, ctx);
            }
            String first = host.substring(0, pos);
            String other = host.substring(pos);
            ctx.put(KEY_HOST, first);
            return delegate.transform(first, ctx) + other;
        }
    }
}