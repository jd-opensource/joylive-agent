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
package com.jd.live.agent.bootstrap.classloader;

import java.net.URL;

/**
 * An interface extending {@link Resourcer} to provide functionality
 * for adding URLs to a resource collection.
 */
public interface URLResourcer extends Resourcer {

    /**
     * Adds an array of URLs to the resource collection.
     *
     * @param urls An array of URLs to add.
     */
    void add(URL... urls);

    String getId();
}

