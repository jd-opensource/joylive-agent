package com.jd.live.agent.governance.context;

import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.request.HeaderReader;
import com.jd.live.agent.governance.request.HeaderWriter;

public interface Propagation {
    void write(Carrier carrier, HeaderWriter writer);

    void read(Carrier carrier, HeaderReader reader);
}
