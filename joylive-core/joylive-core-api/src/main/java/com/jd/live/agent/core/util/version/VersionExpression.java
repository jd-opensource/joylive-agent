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

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.version.Version.split;

/**
 * VersionExpression
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class VersionExpression implements VersionMatcher {

    private final List<VersionMatcher> versions;

    public VersionExpression(String expression) {
        versions = parse(expression);
    }

    protected List<VersionMatcher> parse(String expression) {
        List<VersionMatcher> result = new ArrayList<>();
        if (expression == null || expression.isEmpty()) {
            result.add(new VersionInterval(Version.LOWEST, Version.HIGHEST, IntervalState.LEFT_CLOSE_RIGHT_CLOSE));
        } else {
            List<String> parts = split(expression, ';');
            for (String part : parts) {
                if (!part.isEmpty()) {
                    switch (part.charAt(0)) {
                        case '(':
                        case '[':
                            result.add(new VersionInterval(part));
                            break;
                        default:
                            result.add(new Version(part));
                    }
                }
            }
        }
        return result;
    }

    public boolean match(Version version) {
        if (versions == null || versions.isEmpty())
            return true;
        if (version == null)
            return false;
        for (VersionMatcher matcher : versions) {
            if (matcher.match(version))
                return true;
        }
        return false;
    }

    public static VersionExpression of(String expression) {
        return new VersionExpression(expression);
    }
}
