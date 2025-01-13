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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * An interface that extends both {@link HeaderReader} and {@link HeaderWriter}.
 * It represents a component that can both read and write headers.
 */
public interface HeaderParser extends HeaderReader, HeaderWriter {

    /**
     * A class that extends {@link StringMapWriter} and implements the {@link HeaderParser} interface.
     * It provides functionality to both read and write headers from/to a map of strings.
     */
    class StringMapParser extends StringMapWriter implements HeaderParser {

        public StringMapParser(Map<String, String> map) {
            super(map);
        }

        public StringMapParser(Map<String, String> map, BiConsumer<String, String> setter) {
            super(map, setter);
        }

        @Override
        public Iterator<String> getNames() {
            return map == null ? Collections.emptyIterator() : map.keySet().iterator();
        }
    }

    /**
     * A class that extends {@link ObjectMapWriter} and implements the {@link HeaderParser} interface.
     * It provides functionality to both read and write headers from/to a map of strings.
     */
    class ObjectMapParser extends ObjectMapWriter implements HeaderParser {

        public ObjectMapParser(Map<String, Object> map) {
            super(map);
        }

        public ObjectMapParser(Map<String, Object> map, BiConsumer<String, Object> setter) {
            super(map, setter);
        }

        @Override
        public Iterator<String> getNames() {
            return map == null ? Collections.emptyIterator() : map.keySet().iterator();
        }
    }

    /**
     * A class that extends {@link MultiValueMapWriter} and implements the {@link HeaderParser} interface.
     * It provides functionality to both read and write headers from/to a map of strings.
     */
    class MultiValueMapParser extends MultiValueMapWriter implements HeaderParser {

        public MultiValueMapParser(Map<String, List<String>> map) {
            super(map);
        }

        @Override
        public Iterator<String> getNames() {
            return map == null ? Collections.emptyIterator() : map.keySet().iterator();
        }
    }

}
