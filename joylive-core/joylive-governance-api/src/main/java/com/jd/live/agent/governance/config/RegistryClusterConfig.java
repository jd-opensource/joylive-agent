package com.jd.live.agent.governance.config;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.option.OptionSupplier;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class RegistryClusterConfig implements OptionSupplier {

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
            return defaultGroup;
        }
        return group != null && !group.isEmpty() ? group : defaultGroup;
    }

    public String getAuthority() {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            return username + ":" + password;
        }
        return null;
    }

    public String getProperty(String key) {
        return properties != null && key != null ? properties.get(key) : null;
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    @Override
    public Option getOption() {
        return MapOption.of(properties);
    }
}
