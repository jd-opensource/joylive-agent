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

import com.jd.live.agent.bootstrap.util.Inclusion;
import com.jd.live.agent.bootstrap.util.Inclusion.InclusionType;
import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@ToString
public class UnitRoute {

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    @JsonAlias("whitelist")
    private Set<String> allows;

    @Getter
    @Setter
    @JsonAlias("prefix")
    private Set<String> prefixes;

    @Getter
    @Setter
    @JsonAlias("range")
    private List<UnitRange> ranges;

    @Getter
    @Setter
    private List<CellRoute> cells;

    @Getter
    @Setter
    private String failoverUnit;

    @Getter
    @Setter
    private transient Unit unit;

    private final transient Cache<String, CellRoute> cellRouteCache = new MapCache<>(new ListBuilder<>(() -> cells, CellRoute::getCode));

    private transient Inclusion inclusion;

    public InclusionType allow(String variable) {
        return inclusion.include(variable);
    }

    public boolean contains(int value) {
        if (ranges != null) {
            for (UnitRange range : ranges) {
                if (range.contains(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    public CellRoute getCellRoute(String code) {
        return cellRouteCache.get(code);
    }

    public CellRoute getCellRouteByVariable(String variable) {
        CellRoute result = null;
        if (variable != null && !variable.isEmpty()) {
            for (CellRoute cellRoute : cells) {
                if (cellRoute.isAllow(variable)) {
                    result = cellRoute;
                    break;
                } else if (result == null && cellRoute.isPrefix(variable)) {
                    result = cellRoute;
                }
            }
        }
        return result;
    }

    public boolean isCenter() {
        return unit != null && unit.getType() == UnitType.CENTER;
    }

    public boolean isEmpty() {
        return cells == null || cells.isEmpty();
    }

    public int size() {
        return cells == null ? 0 : cells.size();
    }

    public void cache() {
        if (cells != null) {
            for (CellRoute cell : cells) {
                if (cell.getAllows() != null) {
                    cell.getAllows().remove("");
                }
                if (cell.getPrefixes() != null) {
                    cell.getPrefixes().remove("");
                }
            }
        }
        getCellRoute("");
        inclusion = new Inclusion(allows, prefixes);
    }

}
