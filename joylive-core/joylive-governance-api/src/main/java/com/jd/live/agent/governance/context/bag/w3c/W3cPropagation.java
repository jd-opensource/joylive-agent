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
import com.jd.live.agent.governance.context.bag.AbstractPropagation;
import com.jd.live.agent.governance.context.bag.Cargo;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;

import java.util.Collection;
import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.splitMap;
import static com.jd.live.agent.core.util.tag.Label.join;
import static java.util.Collections.singletonList;

@Injectable
@Extension(value = "w3c", order = Propagation.ORDER_W3C)
public class W3cPropagation extends AbstractPropagation {

    private static final String KEY_BAGGAGE = "baggage";

    @Override
    public void write(Carrier carrier, HeaderWriter writer) {
        Collection<Cargo> cargos = carrier.getCargos();
        if (cargos == null || cargos.isEmpty()) {
            return;
        }

        String baggage = writer.getHeader(KEY_BAGGAGE);
        StringBuilder builder = new StringBuilder(baggage == null ? "" : baggage);
        for (Cargo cargo : cargos) {
            String value = join(cargo.getValues());
            if (value != null && !value.isEmpty()) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(cargo.getKey()).append('=').append(value);
            }
        }
        writer.setHeader(KEY_BAGGAGE, builder.toString());
    }

    @Override
    public boolean read(Carrier carrier, HeaderReader reader) {
        String baggage = reader.getHeader(KEY_BAGGAGE);
        if (baggage == null || baggage.isEmpty()) {
            return false;
        }
        Map<String, String> map = splitMap(baggage);
        return carrier.addCargo(getRequire(), map, value -> singletonList(map.get(value)));
    }
}
