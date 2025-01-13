package com.jd.live.agent.governance.context.bag;

import com.jd.live.agent.core.inject.annotation.Inject;

import java.util.List;

/**
 * Abstract base class for propagation implementations.
 * <p>
 * This abstract class implements the {@link Propagation} interface and provides a
 * mechanism to initialize and manage a {@link CargoRequires} object based on a list
 * of {@link CargoRequire} objects.
 */
public abstract class AbstractPropagation implements Propagation {

    @Inject
    private List<CargoRequire> requires;

    private volatile CargoRequires require;

    public AbstractPropagation() {
    }

    public AbstractPropagation(List<CargoRequire> requires) {
        this.requires = requires;
    }

    public CargoRequires getRequire() {
        if (require == null) {
            synchronized (this) {
                if (require == null) {
                    require = new CargoRequires(requires);
                }
            }
        }
        return require;
    }
}
