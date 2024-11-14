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
package com.jd.live.agent.core.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A functional interface that represents a reader operation which reads data of type T from a reader of type R.
 *
 * @param <R> The type of Reader that is used for reading. This extends the Reader class.
 * @param <T> The type of the object that results from the read operation.
 */
@FunctionalInterface
public interface ObjectReader<R extends Reader, T> {

    /**
     * Reads data from the provided Reader and transforms it into an object of type T.
     *
     * @param reader The reader from which data is read. It is a subclass of Reader, such as BufferedReader.
     * @return An object of type T that holds the data read from the reader.
     * @throws IOException If an I/O error occurs while reading from the reader.
     */
    T read(R reader) throws IOException;

    /**
     * A class that reads a string from a reader.
     *
     * @param <R> The type of the reader.
     */
    class StringReader<R extends Reader> implements ObjectReader<R, String> {

        @Override
        public String read(R reader) throws IOException {
            BufferedReader bufferedReader = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            int i = 0;
            while ((line = bufferedReader.readLine()) != null) {
                if (i++ > 0) {
                    stringBuilder.append('\n');
                }
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }
    }

}

