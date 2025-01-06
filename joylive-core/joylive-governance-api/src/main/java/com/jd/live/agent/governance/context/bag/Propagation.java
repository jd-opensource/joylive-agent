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

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;

import java.util.Collection;

/**
 * Interface for propagating headers between carriers using a HeaderWriter and HeaderReader.
 * <p>
 * This interface defines methods to write and read headers from a carrier, using a HeaderWriter to write
 * and a HeaderReader to read the headers.
 */
@Extensible("Propagation")
public interface Propagation {

    String COMPONENT_PROPAGATION = "propagation";

    int ORDER_W3C = 0;

    int ORDER_LIVE = 10;

    /**
     * Writes headers from the carrier to the writer.
     *
     * @param carrier The carrier from which to read the headers.
     * @param writer  The writer to which the headers should be written.
     */
    void write(Carrier carrier, HeaderWriter writer);

    /**
     * Writes headers from the carrier to the writer.
     *
     * @param reader The reader to use for reading the headers.
     * @param writer The writer to which the headers should be written.
     */
    void write(HeaderReader reader, HeaderWriter writer);

    /**
     * Reads headers from the carrier using the specified reader.
     *
     * @param carrier The carrier from which to read the headers.
     * @param reader  The reader to use for reading the headers.
     * @return A boolean indicating whether the headers were successfully read.
     */
    boolean read(Carrier carrier, HeaderReader reader);

    /**
     * A class that implements the {@link Propagation} interface.
     * This class manages a list of {@link Propagation} readers and a single {@link Propagation} writer.
     * It delegates the reading operation to each reader in the list until one successfully reads the headers,
     * and it delegates the writing operation to the specified writer.
     */
    class AutoPropagation implements Propagation {

        private final Collection<Propagation> readers;

        private final Propagation writer;

        public AutoPropagation(Collection<Propagation> readers, Propagation writer) {
            this.readers = readers;
            this.writer = writer;
        }

        @Override
        public void write(Carrier carrier, HeaderWriter writer) {
            this.writer.write(carrier, writer);
        }

        @Override
        public void write(HeaderReader reader, HeaderWriter writer) {
            for (Propagation propagation : readers) {
                propagation.write(reader, writer);
            }
        }

        @Override
        public boolean read(Carrier carrier, HeaderReader reader) {
            for (Propagation propagation : readers) {
                if (propagation.read(carrier, reader)) {
                    return true;
                }
            }
            return false;
        }
    }

}
