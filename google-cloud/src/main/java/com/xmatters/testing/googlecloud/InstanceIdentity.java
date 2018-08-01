package com.xmatters.testing.googlecloud;

import com.google.api.services.compute.model.Instance;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstanceIdentity {
    private static final Pattern PROJECT_ZONE = Pattern.compile("^https://www.googleapis.com/compute/v1/projects/([\\w-]+)/zones/([\\w-]+)$");

    @Getter
    private String project;

    @Getter
    private String zone;

    @Getter
    private String name;

    private InstanceIdentity() {}

    public static InstanceIdentity extract(Instance instance) {
        InstanceIdentity id = new InstanceIdentity();
        Matcher m = PROJECT_ZONE.matcher(instance.getZone());

        if (m.find()) {
            String project = m.group(1);
            String zone = m.group(2);

            id.project = project;
            id.zone = zone;
            id.name = instance.getName();
        }

        return id;
    }
}
