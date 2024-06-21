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
package com.jd.live.agent.governance.policy.live;

import com.jd.live.agent.core.parser.json.DeserializeConverter;
import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.governance.policy.live.converter.VariableMissingActionDeserializer;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
public class UnitRule {
    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    @JsonAlias("type")
    private LiveType liveType;

    @Getter
    @Setter
    @JsonAlias("biz")
    private String business;

    @Getter
    @Setter
    private String variable;

    @Getter
    @Setter
    @JsonAlias("accessor")
    private String variableSource;

    @Getter
    @Setter
    @JsonAlias("function")
    private String variableFunction;

    @Getter
    @Setter
    @JsonAlias("actionWithoutVariable")
    @DeserializeConverter(VariableMissingActionDeserializer.class)
    private VariableMissingAction variableMissingAction;

    @Getter
    @Setter
    private int modulo;

    @Getter
    @Setter
    @JsonAlias("units")
    private List<UnitRoute> unitRoutes;

    private final transient Cache<String, UnitRoute> unitRouteCache = new MapCache<>(new ListBuilder<>(() -> unitRoutes, UnitRoute::getCode));

    public UnitRoute getUnitRoute(String code) {
        return unitRouteCache.get(code);
    }

    public UnitRoute getUnitRoute(String variable, UnitFunction function) {
        int size = unitRoutes == null ? 0 : unitRoutes.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return unitRoutes.get(0);
        } else if (variable == null || variable.isEmpty()) {
            if (variableMissingAction == VariableMissingAction.CENTER) {
                for (UnitRoute unitRoute : unitRoutes) {
                    if (unitRoute.isCenter()) {
                        return unitRoute;
                    }
                }
            }
            return null;
        } else {
            int value = function == null ? -1 : function.compute(variable, modulo);
            UnitRoute result = null;
            for (UnitRoute unitRoute : unitRoutes) {
                // Prioritize whitelists, then prefix whitelists, followed by ranges.
                if (unitRoute.isAllow(variable)) {
                    return unitRoute;
                } else if (unitRoute.isPrefix(variable)) {
                    result = unitRoute;
                } else if (result == null && unitRoute.contains(value)) {
                    result = unitRoute;
                }
            }
            return result;
        }
    }

    public boolean contains(UnitRoute route, String variable, UnitFunction function) {
        return route != null && (route.isAllow(variable)
                || route.isPrefix(variable)
                || (function != null && route.contains(function.compute(variable, modulo))));
    }

    public int size() {
        return unitRoutes == null ? 0 : unitRoutes.size();
    }

    public boolean isFailover(String unit) {
        UnitRoute localRoute = getUnitRoute(unit);
        return localRoute == null || localRoute.getFailoverUnit() != null && !localRoute.getFailoverUnit().isEmpty();
    }

    public void cache() {
        getUnitRoute("");
        if (unitRoutes != null) {
            unitRoutes.forEach(UnitRoute::cache);
        }
    }
}


