package com.jd.live.agent.governance.context.bag.live;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.context.bag.*;
import com.jd.live.agent.governance.request.HeaderFeature;
import com.jd.live.agent.governance.request.HeaderReader;
import com.jd.live.agent.governance.request.HeaderWriter;

import java.util.*;

import static com.jd.live.agent.core.util.CollectionUtils.iterate;
import static com.jd.live.agent.core.util.CollectionUtils.toMap;
import static com.jd.live.agent.core.util.tag.Label.join;

@Injectable
@Extension(value = "live", order = Propagation.ORDER_LIVE)
public class LivePropagation extends AbstractPropagation {

    public static final Propagation LIVE_PROPAGATION = new LivePropagation();

    public LivePropagation() {
    }

    public LivePropagation(List<CargoRequire> requires) {
        super(requires);
    }

    @Override
    public void write(Carrier carrier, Location location, HeaderWriter writer) {
        if (writer == null) {
            return;
        }
        Collection<Cargo> cargos = carrier.getCargos();
        Map<String, String> tags = location == null ? null : location.getTags();
        int tagSize = tags == null ? 0 : tags.size();
        int size = (cargos == null ? 0 : cargos.size()) + tagSize;
        if (size > 0) {
            if (size > 1 && writer.getFeature().isBatchable()) {
                Map<String, String> map = toMap(cargos, Cargo::getKey, cargo -> join(cargo.getValues()));
                if (tagSize > 0) {
                    map = map == null ? new HashMap<>(3) : map;
                    map.putAll(tags);
                }
                writer.setHeaders(map);
            } else {
                if (cargos != null) {
                    for (Cargo cargo : cargos) {
                        writer.setHeader(cargo.getKey(), join(cargo.getValues()));
                    }
                }
                if (tagSize > 0) {
                    tags.forEach(writer::setHeader);
                }
            }
        }
    }

    @Override
    public void write(HeaderReader reader, HeaderWriter writer) {
        if (reader == null || writer == null) {
            return;
        }
        CargoRequire require = getRequire();
        Iterator<String> names = reader.getNames();
        HeaderFeature feature = writer.getFeature();
        if (names != null && names.hasNext()) {
            if (feature.isBatchable()) {
                Map<String, String> headers = toMap(names, require, name -> name, name -> join(reader.getHeaders(name)));
                if (headers != null && !headers.isEmpty()) {
                    writer.setHeaders(headers);
                }
            } else {
                iterate(names, require, name -> writer.setHeader(name, join(reader.getHeaders(name))));
            }
        }
    }

    @Override
    public boolean read(Carrier carrier, HeaderReader reader) {
        if (carrier == null || reader == null) {
            return false;
        }
        CargoRequire require = getRequire();
        return reader.read((name, values) ->
                        carrier.addCargo(new Cargo(name, Label.parseValue(values), true)),
                require) > 0;
    }
}
