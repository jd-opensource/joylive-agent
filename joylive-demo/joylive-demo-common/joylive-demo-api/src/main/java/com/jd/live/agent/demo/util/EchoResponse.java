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
package com.jd.live.agent.demo.util;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

@Getter
@Setter
public class EchoResponse {

    private String name;

    private LiveTransmission transmission;

    private LiveLocation location = new LiveLocation();

    private String message;

    public EchoResponse(String name, LiveTransmission transmission, String message) {
        this.name = name;
        this.transmission = transmission;
        this.message = message;
    }

    public EchoResponse(String name, String carrier, Function<String, String> tagFunc, String message) {
        this(name, new LiveTransmission(carrier, tagFunc), message);
    }

    public EchoResponse(String name, String carrier, Function<String, String> tagFunc) {
        this(name, new LiveTransmission(carrier, tagFunc), null);
    }

    public EchoResponse(String name, LiveTransmission transmission) {
        this(name, transmission, null);
    }

    @Override
    public String toString() {
        return name + ":\n  " + transmission.toString() + "\n  " + location.toString() + "\n\n" + (message == null ? "" : message) + "\n";
    }
}
