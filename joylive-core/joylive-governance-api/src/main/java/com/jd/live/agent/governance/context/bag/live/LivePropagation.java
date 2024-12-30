package com.jd.live.agent.governance.context.bag.live;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.context.bag.*;
import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Injectable
@Extension(value = "live", order = Propagation.ORDER_LIVE)
public class LivePropagation extends AbstractPropagation {

    public static final Propagation LIVE_PROPAGATION = new LivePropagation();

    @Override
    public void write(Carrier carrier, HeaderWriter writer) {
        Collection<Cargo> cargos = carrier.getCargos();
        if (cargos != null) {
            List<String> values;
            for (Cargo cargo : cargos) {
                values = cargo.getValues();
                int size = values == null ? 0 : values.size();
                switch (size) {
                    case 0:
                        writer.setHeader(cargo.getKey(), null);
                        break;
                    case 1:
                        writer.setHeader(cargo.getKey(), values.get(0));
                        break;
                    default:
                        writer.setHeader(cargo.getKey(), Label.join(values));
                }
            }
        }
    }

    @Override
    public boolean read(Carrier carrier, HeaderReader reader) {
        CargoRequire require = getRequire();
        Iterator<String> headerNames = reader.getHeaderNames();
        int counter = 0;
        while (headerNames.hasNext()) {
            String headerName = headerNames.next();
            List<String> headerValues = reader.getHeaders(headerName);
            if (require.match(headerName)) {
                counter++;
                if (headerValues != null) {
                    carrier.addCargo(new Cargo(headerName, new ArrayList<>(headerValues)));
                }
            }
        }
        return counter > 0;
    }
}
