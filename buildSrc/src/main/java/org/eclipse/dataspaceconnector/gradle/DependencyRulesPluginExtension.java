package org.eclipse.dataspaceconnector.gradle;

import org.gradle.api.provider.Property;

public abstract class DependencyRulesPluginExtension {
    public abstract Property<String> getSeverity();

    public boolean isFailSeverity() {
        return "fail".equals(getSeverity().get());
    }
}
