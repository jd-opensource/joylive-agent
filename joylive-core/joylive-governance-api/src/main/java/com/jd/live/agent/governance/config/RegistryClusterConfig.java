package com.jd.live.agent.governance.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistryClusterConfig {

    private String type;

    private String address;

    private String username;

    private String password;

    private String namespace;

    private String group;

    public boolean validate() {
        return type != null && !type.isEmpty() && address != null && !address.isEmpty();
    }

    public String getGroup(String defaultGroup) {
        return group != null && !group.isEmpty() ? group : defaultGroup;
    }
}
