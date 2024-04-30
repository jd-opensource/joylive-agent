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
package com.jd.live.agent.core.thread;

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * The Camera interface represents a contract for camera operations that can be extended.
 * It is annotated with the {@link Extensible} annotation to indicate that it can be extended
 * with additional functionality. The "Camera" string in the annotation serves as a unique
 * identifier or description for this extensibility point.
 */
@Extensible("Camera")
public interface Camera {

    /**
     * Takes a snapshot of the current state of the camera.
     *
     * @return an object representing the snapshot of the camera's state.
     */
    Object snapshot();

    /**
     * Restores the camera's state to a previous snapshot.
     *
     * @param snapshot the snapshot object previously obtained from the {@link #snapshot()} method.
     */
    void restore(Object snapshot);

    /**
     * Removes the camera or its associated resources.
     * This could be used to clean up any allocated resources when the camera is no longer needed.
     */
    void remove();
}

