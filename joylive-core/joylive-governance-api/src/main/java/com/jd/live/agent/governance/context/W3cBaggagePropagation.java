package com.jd.live.agent.governance.context;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.context.bag.Cargo;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.request.HeaderReader;
import com.jd.live.agent.governance.request.HeaderWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

@Injectable
@Extension("W3cBaggagePropagation")
public class W3cBaggagePropagation implements Propagation {
    private static final Logger logger = LoggerFactory.getLogger(W3cBaggagePropagation.class);

    private final CargoRequires require;
    @Inject
    private List<CargoRequire> requires;

    {
        this.require = new CargoRequires(requires);
    }

    @Override
    public void write(Carrier carrier, HeaderWriter writer) {
        StringBuilder baggage = new StringBuilder();
        Collection<Cargo> cargos = carrier.getCargos();

        if (cargos != null) {
            for (Cargo cargo : cargos) {
                if (cargo.getValues() != null) {
                    List<String> values = new ArrayList<>();
                    for (String value : cargo.getValues()) {
                        try {
                            String encodedValue = URLEncoder.encode(value, "UTF-8");
                            values.add(encodedValue);
                        } catch (UnsupportedEncodingException e) {
                            logger.error("URL encoding failed for value: " + value, e);
                        }
                    }

                    if (!values.isEmpty()) {
                        baggage.append(cargo.getKey()).append("=")
                                .append(String.join(";", values)).append(",");
                    }
                }
            }
        }

        if (baggage.length() > 0) {
            baggage.setLength(baggage.length() - 1);
            writer.setHeader("baggage", baggage.toString());
        }
    }

    @Override
    public void read(Carrier carrier, HeaderReader reader) {
        String baggage = reader.getHeader("baggage");
        if (baggage == null || baggage.isEmpty())
            return;

        String[] keyValuePairs = baggage.split(",");
        for (String pair : keyValuePairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String headerName = keyValue[0];
                if (require.match(headerName)) {
                    List<String> headerValues = new ArrayList<>();
                    String[] values = keyValue[1].split(";");
                    for (String value : values) {
                        try {
                            String decodedValue = URLDecoder.decode(value, "UTF-8");
                            headerValues.add(decodedValue);
                        } catch (UnsupportedEncodingException e) {
                            System.err.println("URL decoding failed for value: " + value);
                        }
                    }

                    if (!headerValues.isEmpty()) {
                        carrier.addCargo(new Cargo(headerName, headerValues));
                    }
                }
            }
        }
    }
}
