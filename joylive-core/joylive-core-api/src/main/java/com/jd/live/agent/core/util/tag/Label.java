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
package com.jd.live.agent.core.util.tag;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.StringUtils.split;
import static java.util.Collections.addAll;

/**
 * The {@code Label} interface defines a contract for label handling. It provides constants
 * representing common label regions and methods for retrieving and parsing label values.
 */
public interface Label {

    /**
     * Constant for the label region.
     */
    String LABEL_REGION = "region";

    /**
     * Constant for the label zone.
     */
    String LABEL_ZONE = "zone";

    /**
     * Constant for the label live space ID.
     */
    String LABEL_LIVESPACE_ID = "liveSpaceId";

    /**
     * Constant for the label unit.
     */
    String LABEL_UNIT = "unit";

    /**
     * Constant for the label cell.
     */
    String LABEL_CELL = "cell";

    /**
     * Constant for the label lane space ID.
     */
    String LABEL_LANESPACE_ID = "laneSpaceId";

    /**
     * Constant for the label lane.
     */
    String LABEL_LANE = "lane";

    /**
     * Default value for labels.
     */
    String DEFAULT_VALUE = "";

    /**
     * Gets the key of the label.
     *
     * @return the label key
     */
    String getKey();

    /**
     * Gets a list of values associated with the label.
     *
     * @return a list of label values
     */
    List<String> getValues();

    /**
     * Gets the first value associated with the label, or the default value if no values are present.
     *
     * @return the first label value or the default value
     */
    String getFirstValue();

    /**
     * Gets a single value associated with the label, or the default value if no values are present.
     *
     * @return a single label value or the default value
     */
    String getValue();

    /**
     * Parses a label value string into a list of strings. The parsing logic handles special
     * formatting where values are enclosed in square brackets and separated by commas.
     *
     * @param value the label value string to parse
     * @return a list of parsed label values
     */
    static List<String> parseValue(String value) {
        List<String> result;
        if (value == null || value.isEmpty()) {
            result = new ArrayList<>();
        } else if (value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']') {
            if (value.length() == 2) {
                result = new ArrayList<>();
            } else {
                String[] parts = split(value.substring(1, value.length() - 1), ',');
                result = new ArrayList<>(parts.length);
                addAll(result, parts);
            }
        } else {
            result = new ArrayList<>(1);
            result.add(value);
        }
        return result;
    }

}
