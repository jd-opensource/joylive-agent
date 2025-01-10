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
package com.jd.live.agent.governance.context.bag;

import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.live.LiveCargoRequire;
import com.jd.live.agent.governance.context.bag.live.LivePropagation;
import com.jd.live.agent.governance.context.bag.w3c.W3cPropagation;
import com.jd.live.agent.governance.request.HeaderReader.MultiValueMapReader;
import com.jd.live.agent.governance.request.HeaderReader.StringMapReader;
import com.jd.live.agent.governance.request.HeaderWriter.MultiValueMapWriter;
import com.jd.live.agent.governance.request.HeaderWriter.StringMapWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

public class PropagationTest {

    private static final Map<String, String> liveSingleReader = new HashMap<>();

    private static final Map<String, String> liveSingleWriter = new HashMap<>();

    private static final Map<String, List<String>> liveMultiReader = new HashMap<>();

    private static final Map<String, List<String>> liveMultiWriter = new HashMap<>();

    private static final Map<String, String> w3cSingleReader = new HashMap<>();

    private static final Map<String, String> w3cSingleWriter = new HashMap<>();

    private static final Map<String, List<String>> w3cMultiReader = new HashMap<>();

    private static final Map<String, List<String>> w3cMultiWriter = new HashMap<>();

    private static final GovernanceConfig config = new GovernanceConfig();

    private static final List<CargoRequire> cargoRequires = Arrays.asList(new LiveCargoRequire(config));

    private static final Propagation w3cPropagation = new W3cPropagation(cargoRequires);

    private static final Propagation livePropagation = new LivePropagation(cargoRequires);

    @BeforeAll
    static void setup() {
        config.getTransmitConfig().setPrefixes(new HashSet<>(Arrays.asList("x-live-")));
        liveSingleReader.put("x-live-unit", "unit1");
        List<String> list = liveMultiReader.computeIfAbsent("x-live-cell", k -> new ArrayList<>());
        list.add("cell1");
        list.add("cell2");
        w3cSingleReader.put("baggage", "x-live-unit=unit1,x-live-cell=[cell1|cell2]");
        list = w3cMultiReader.computeIfAbsent("baggage", k -> new ArrayList<>());
        list.add("x-live-unit=unit1");
        list.add("x-live-cell=[cell1|cell2]");
    }

    @BeforeEach
    void clean() {
        liveSingleWriter.clear();
        liveMultiWriter.clear();
        w3cSingleWriter.clear();
        w3cMultiWriter.clear();
    }

    @Test
    void testLive() {
        Carrier carrier = RequestContext.create();
        livePropagation.read(carrier, new StringMapReader(liveSingleReader));
        livePropagation.read(carrier, new MultiValueMapReader(liveMultiReader));
        Cargo cargo = carrier.getCargo("x-live-unit");
        Assertions.assertNotNull(cargo);
        Assertions.assertEquals("unit1", cargo.getValue());
        cargo = carrier.getCargo("x-live-cell");
        Assertions.assertNotNull(cargo);
        Assertions.assertEquals("[cell1,cell2]", cargo.getValue());
        livePropagation.write(carrier, new StringMapWriter(liveSingleWriter));
        livePropagation.write(carrier, new MultiValueMapWriter(liveMultiWriter));
        Assertions.assertEquals("unit1", liveSingleWriter.get("x-live-unit"));
        Assertions.assertEquals("[cell1|cell2]", liveSingleWriter.get("x-live-cell"));
        Assertions.assertEquals("unit1", liveMultiWriter.get("x-live-unit"));
        Assertions.assertEquals("[cell1|cell2]", Label.join(liveMultiWriter.get("x-live-cell")));
    }

    @Test
    void testW3c() {
        Carrier carrier = RequestContext.create();
        w3cPropagation.read(carrier, new StringMapReader(w3cSingleReader));
        w3cPropagation.read(carrier, new MultiValueMapReader(w3cMultiReader));
        Cargo cargo = carrier.getCargo("x-live-unit");
        Assertions.assertNotNull(cargo);
        Assertions.assertEquals("unit1", cargo.getValue());
        cargo = carrier.getCargo("x-live-cell");
        Assertions.assertNotNull(cargo);
        Assertions.assertEquals("[cell1|cell2]", cargo.getValue());
        w3cPropagation.write(carrier, new StringMapWriter(w3cSingleWriter));
        w3cPropagation.write(carrier, new MultiValueMapWriter(w3cMultiWriter));
        Assertions.assertEquals("x-live-unit=unit1,x-live-cell=[cell1|cell2]", w3cSingleWriter.get("baggage"));
        Assertions.assertEquals("x-live-unit=unit1,x-live-cell=[cell1|cell2]", Label.join(w3cMultiWriter.get("baggage")));
    }

}
