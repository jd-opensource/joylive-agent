package com.jd.live.agent.governance.context;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.context.bag.Cargo;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.request.HeaderReader;
import com.jd.live.agent.governance.request.HeaderWriter;

import java.util.*;

@Injectable
@Extension("LivePropagation")
public class LivePropagation implements Propagation {
    private final CargoRequires require;
    @Inject
    private List<CargoRequire> requires;

    {
        this.require = new CargoRequires(requires);
    }

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
                        for (String value : values) {
                            writer.setHeader(cargo.getKey(), value);
                        }
                }
            }
        }
    }

    @Override
    public void read(Carrier carrier, HeaderReader reader) {
        Iterator<String> headerNames = reader.getHeaderNames();
        while (headerNames.hasNext()) {
            String headerName = headerNames.next();
            List<String> headerValues = reader.getHeaders(headerName);
            if (require.match(headerName) && headerValues != null && !headerValues.isEmpty()) {
                carrier.addCargo(new Cargo(headerName, new ArrayList<>(headerValues)));
            }
        }
    }
}
