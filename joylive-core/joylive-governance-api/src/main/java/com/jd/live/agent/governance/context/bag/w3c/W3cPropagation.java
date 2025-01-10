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
package com.jd.live.agent.governance.context.bag.w3c;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.context.bag.*;
import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;

import java.util.Collection;

import static com.jd.live.agent.core.util.StringUtils.*;
import static com.jd.live.agent.core.util.tag.Label.join;
import static com.jd.live.agent.core.util.tag.Label.parseValue;

@Injectable
@Extension(value = "w3c", order = Propagation.ORDER_W3C)
public class W3cPropagation extends AbstractPropagation {

    private static final String KEY_BAGGAGE = "baggage";

    @Override
    public void write(Carrier carrier, HeaderWriter writer) {
        if (carrier == null || writer == null) {
            return;
        }
        Collection<Cargo> cargos = carrier.getCargos();
        if (cargos == null || cargos.isEmpty()) {
            return;
        }
        if (writer.isDuplicable()) {
            writer.addHeader(KEY_BAGGAGE, appendCargo(cargos, new StringBuilder()));
        } else {
            String baggage = writer.getHeader(KEY_BAGGAGE);
            StringBuilder builder = baggage == null || baggage.isEmpty() ? new StringBuilder() : new StringBuilder(baggage);
            writer.setHeader(KEY_BAGGAGE, appendCargo(cargos, builder));
        }
    }

    @Override
    public void write(HeaderReader reader, HeaderWriter writer) {
        if (reader == null || writer == null) {
            return;
        }
        Iterable<String> baggages = reader.getHeaders(KEY_BAGGAGE);
        if (baggages == null) {
            return;
        }
        CargoRequire require = getRequire();
        int counter;
        for (String baggage : baggages) {
            counter = splitMap(baggage, COMMA, true, (key, value) -> {
                if (require.match(key)) {
                    writer.setHeader(key, value);
                    return true;
                }
                return false;
            });
            if (counter > 0) {
                return;
            }
        }
    }

    @Override
    public boolean read(Carrier carrier, HeaderReader reader) {
        if (carrier == null || reader == null) {
            return false;
        }
        Iterable<String> baggages = reader.getHeaders(KEY_BAGGAGE);
        if (baggages == null) {
            return false;
        }
        CargoRequires require = getRequire();
        int counter;
        for (String baggage : baggages) {
            counter = splitMap(baggage, COMMA, true, (key, value) -> {
                if (require.match(key)) {
                    carrier.addCargo(new Cargo(key, parseValue(value), true));
                    return true;
                }
                return false;
            });
            if (counter > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Appends the key-value pairs of each Cargo in the collection to the provided StringBuilder.
     * The values are joined into a single string using the join method, and the key-value pair
     * is formatted as "key=value". Pairs are separated by commas.
     *
     * @param cargos  the collection of Cargo objects to be added
     * @param builder the StringBuilder to which the key-value pairs will be appended
     * @return the value with the appended key-value pairs
     */
    private String appendCargo(Collection<Cargo> cargos, StringBuilder builder) {
        for (Cargo cargo : cargos) {
            append(builder, CHAR_COMMA, cargo.getKey(), join(cargo.getValues()), true);
        }
        return builder.toString();
    }


}
