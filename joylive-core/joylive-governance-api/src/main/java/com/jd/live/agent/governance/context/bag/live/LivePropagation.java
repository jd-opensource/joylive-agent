package com.jd.live.agent.governance.context.bag.live;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.context.bag.*;
import com.jd.live.agent.governance.request.HeaderFeature;
import com.jd.live.agent.governance.request.HeaderReader;
import com.jd.live.agent.governance.request.HeaderWriter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    public void write(Carrier carrier, HeaderWriter writer) {
        if (carrier == null || writer == null) {
            return;
        }
        Collection<Cargo> cargos = carrier.getCargos();
        int size = cargos == null ? 0 : cargos.size();
        if (size > 0) {
            HeaderFeature feature = writer.getFeature();
            if (size > 1 && feature.isBatchable()) {
                writer.setHeaders(toMap(cargos, Cargo::getKey, cargo -> join(cargo.getValues())));
            } else {
                for (Cargo cargo : cargos) {
                    writer.setHeader(cargo.getKey(), join(cargo.getValues()));
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
                Map<String, String> headers = toMap(names, require::match, name -> name, name -> join(reader.getHeaders(name)));
                if (headers != null && !headers.isEmpty()) {
                    writer.setHeaders(headers);
                }
            } else {
                iterate(names, require::match, name -> writer.setHeader(name, join(reader.getHeaders(name))));
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
                require::match) > 0;
    }
}
