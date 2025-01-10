package com.jd.live.agent.governance.context.bag.live;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.context.bag.*;
import com.jd.live.agent.governance.request.HeaderReader;
import com.jd.live.agent.governance.request.HeaderWriter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

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
        if (cargos != null) {
            for (Cargo cargo : cargos) {
                writer.setHeaders(cargo.getKey(), cargo.getValues());
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
        if (names != null) {
            String name;
            while (names.hasNext()) {
                name = names.next();
                if (require.match(name)) {
                    writer.setHeaders(name, toList(reader.getHeaders(name)));
                }
            }
        }
    }

    @Override
    public boolean read(Carrier carrier, HeaderReader reader) {
        if (carrier == null || reader == null) {
            return false;
        }
        int counter = 0;
        CargoRequire require = getRequire();
        Iterator<String> names = reader.getNames();
        if (names != null) {
            String name;
            Iterable<String> values;
            while (names.hasNext()) {
                name = names.next();
                if (require.match(name)) {
                    counter++;
                    values = reader.getHeaders(name);
                    if (values != null) {
                        carrier.addCargo(new Cargo(name, toList(values)));
                    }
                }
            }
        }

        return counter > 0;
    }
}
