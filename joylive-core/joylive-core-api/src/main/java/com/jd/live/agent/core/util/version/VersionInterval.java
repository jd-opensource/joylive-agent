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
package com.jd.live.agent.core.util.version;

import com.jd.live.agent.core.exception.VersionException;
import lombok.Getter;

import java.util.List;

import static com.jd.live.agent.core.util.version.Version.split;

/**
 * VersionInterval
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Getter
public class VersionInterval implements VersionMatcher {

    private Version lowest;

    private Version highest;

    private IntervalState state;

    public VersionInterval(Version lowest, Version highest, IntervalState state) {
        this.lowest = lowest;
        this.highest = highest;
        this.state = state;
    }

    public VersionInterval(String expression) {
        int length = expression == null ? 0 : expression.length();
        char first = length > 0 ? expression.charAt(0) : 0;
        char last = length > 0 ? expression.charAt(expression.length() - 1) : 0;
        if (length == 0) {
            parse(expression, IntervalState.LEFT_CLOSE_RIGHT_CLOSE);
        } else if (first == '(' && last == ')') {
            parse(expression, IntervalState.LEFT_OPEN_RIGHT_OPEN);
        } else if (first == '(' && last == ']') {
            parse(expression, IntervalState.LEFT_OPEN_RIGHT_CLOSE);
        } else if (first == '[' && last == ')') {
            parse(expression, IntervalState.LEFT_CLOSE_RIGHT_OPEN);
        } else if (first == '[' && last == ']') {
            parse(expression, IntervalState.LEFT_CLOSE_RIGHT_CLOSE);
        } else
            throw new VersionException("illegal version interval " + expression);

    }

    protected void parse(String expression, IntervalState state) {
        this.state = state;
        if (expression == null || expression.length() <= 2) {
            this.lowest = Version.LOWEST;
            this.highest = Version.HIGHEST;
        } else {
            List<String> parts = split(expression.substring(1, expression.length() - 1), ',');
            if (parts.size() == 2) {
                String low = parts.get(0);
                String high = parts.get(1);
                this.lowest = low.isEmpty() ? Version.LOWEST : new Version(low);
                this.highest = high.isEmpty() ? Version.HIGHEST : new Version(high);
            } else {
                throw new VersionException("illegal version interval " + expression);
            }
        }
    }

    @Override
    public boolean match(Version version) {
        if (version == null)
            return false;
        switch (state) {
            case LEFT_OPEN_RIGHT_OPEN:
                return version.compareTo(lowest) > 0 && version.compareTo(highest) < 0;
            case LEFT_OPEN_RIGHT_CLOSE:
                return version.compareTo(lowest) > 0 && version.compareTo(highest) <= 0;
            case LEFT_CLOSE_RIGHT_OPEN:
                return version.compareTo(lowest) >= 0 && version.compareTo(highest) < 0;
            case LEFT_CLOSE_RIGHT_CLOSE:
                return version.compareTo(lowest) >= 0 && version.compareTo(highest) <= 0;
        }
        return false;
    }
}
