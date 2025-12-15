package com.jd.live.agent.governance.subscription.config.gray;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a composite key for Nacos configuration items, consisting of a dataId and group pair.
 */
@Getter
public class ConfigKey {

    private static final Set<String> FORMATS = new HashSet<>(Arrays.asList("json", "properties", "yml", "yaml", "xml", "txt"));
    private static final String BETA_POLICY_SUFFIX = "-beta-policy";
    private static final String BETA_POLICY_EXTENSION = ".json";

    private final String name;

    private final String extension;

    private final String group;

    public ConfigKey(String name, String group) {
        this.name = name;
        this.group = group;
        int pos = name.lastIndexOf('.');
        String ext = pos > 0 ? name.substring(pos + 1).toLowerCase() : null;
        this.extension = ext != null && FORMATS.contains(ext) ? ext : null;
    }

    /**
     * Generates a policy variant of this configuration key by appending "-beta-policy" suffix
     * and ".json" extension to the dataId. The group remains unchanged.
     *
     * @return a new {@code ConfigKey} instance representing the policy configuration variant
     */
    public ConfigKey getPolicyKey() {
        StringBuilder builder = new StringBuilder(name.length() + 20);
        String shortName = extension != null ? name.substring(0, name.length() - extension.length() - 1) : name;
        builder.append(shortName)
                .append(BETA_POLICY_SUFFIX)
                .append(BETA_POLICY_EXTENSION);
        String id = builder.toString();
        return new ConfigKey(id, group);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfigKey)) return false;
        ConfigKey that = (ConfigKey) o;
        return Objects.equals(name, that.name) && Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, group);
    }

    @Override
    public String toString() {
        return name + "@" + group;
    }
}
