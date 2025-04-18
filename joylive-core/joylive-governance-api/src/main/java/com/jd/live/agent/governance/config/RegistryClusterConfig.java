package com.jd.live.agent.governance.config;

import com.jd.live.agent.core.util.URI;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class RegistryClusterConfig {

    private String type;

    private String address;

    private String username;

    private String password;

    private String namespace;

    private String group;

    private boolean groupEnabled;

    private boolean denyEmptyEnabled;

    private RegistryRole role = RegistryRole.SECONDARY;

    private Map<String, String> properties;

    private RegistryMode mode = RegistryMode.FULL;

    public RegistryClusterConfig() {
    }

    public RegistryClusterConfig(RegistryRole role, RegistryMode mode) {
        this.mode = mode;
        this.role = role;
    }

    public boolean validate() {
        if (type == null || type.isEmpty()) {
            return false;
        } else if (address == null || address.isEmpty()) {
            return false;
        }
        URI uri = URI.parse(address);
        String host = uri == null ? null : uri.getHost();
        return host != null && !host.isEmpty();
    }

    public String getGroup(String defaultGroup) {
        if (!groupEnabled) {
            return null;
        }
        return group != null && !group.isEmpty() ? group : defaultGroup;
    }
}
