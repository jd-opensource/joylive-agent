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

import lombok.Getter;

/**
 * The Snapshot class represents a captured state of a camera along with the photo taken at that state.
 * It provides methods to restore the camera to the captured state and to remove the snapshot.
 */
@Getter
public class Snapshot {

    /**
     * The camera from which this snapshot was taken.
     */
    private final Camera camera;

    /**
     * The photo object representing the image captured by the camera.
     */
    private final Object photo;

    /**
     * Constructs a new Snapshot with the provided camera and photo.
     *
     * @param camera the camera from which the snapshot was taken
     * @param photo  the photo object representing the captured image
     */
    public Snapshot(Camera camera, Object photo) {
        this.camera = camera;
        this.photo = photo;
    }

    /**
     * Restores the camera to the state captured in this snapshot.
     */
    public void restore() {
        camera.restore(photo);
    }

    /**
     * Removes the snapshot and any associated resources.
     * This may also involve calling the camera's remove method to clean up resources there.
     */
    public void remove() {
        camera.remove();
    }
}

