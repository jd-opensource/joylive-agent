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
package com.jd.live.agent.core.mcp.spec.v1;

import lombok.*;

import java.util.List;

/**
 * Describes the name and version of an MCP implementation, with an optional title for
 * UI representation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Implementation implements Identifier {
    /**
     * Intended for programmatic or logical use, but used as a display name in
     * past specs or fallback (if title isn't present).
     */
    private String name;
    /**
     * Intended for UI and end-user contexts
     */
    private String title;
    /**
     * The version of the implementation.
     */
    private String version;
    /**
     * The description of the implementation.
     */
    private String description;
    /**
     * The website url of the implementation.
     */
    private String websiteUrl;

    /**
     * A standard URI pointing to an icon resource. May be an HTTP/HTTPS URL or a
     * `data:` URI with Base64-encoded image data.
     */
    private String src;

    /**
     * Optional MIME type override if the source MIME type is missing or generic.
     * For example: `"image/png"`, `"image/jpeg"`, or `"image/svg+xml"`.
     */
    private String mimeType;

    /**
     * Optional array of strings that specify sizes at which the icon can be used.
     * Each string should be in WxH format (e.g., `"48x48"`, `"96x96"`) or `"any"` for scalable formats like SVG.
     * <p>
     * If not provided, the client should assume that the icon can be used at any size.
     */
    private List<String> sizes;

    /**
     * Optional specifier for the theme this icon is designed for. `light` indicates
     * the icon is designed to be used with a light background, and `dark` indicates
     * the icon is designed to be used with a dark background.
     * <p>
     * If not provided, the client should assume the icon can be used with any theme.
     */
    private String theme;

    public Implementation(String name, String version) {
        this.name = name;
        this.version = version;
    }
}
