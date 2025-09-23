package com.jd.live.agent.governance.config;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.option.OptionSupplier;
import com.jd.live.agent.core.util.template.Template;
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
        String host = uri.getHost();
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

    /**
     * Supplements missing configuration values from provided options.
     *
     * @param config the configuration option
     * @param option the additional option
     */
    public void supplement(Option config, Option option) {
        URI uri = address == null || address.isEmpty() ? null : URI.parse(address);
        String host = uri == null ? null : uri.getHost();
        if (host == null || host.isEmpty()) {
            address = evaluate("address", config, option);
        }
        if (username == null || username.isEmpty()) {
            username = evaluate("username", config, option);
        }
        if (password == null || password.isEmpty()) {
            password = evaluate("password", config, option);
        }
    }

    /**
     * Evaluates a configuration key using template processing.
     *
     * @param key    the configuration key
     * @param config the configuration option
     * @param option the additional option
     * @return the evaluated value, or null if key is empty
     */
    private String evaluate(String key, Option config, Option option) {
        String expression = config.getString(key);
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        return (String) Template.evaluate(expression, option);
    }
}
