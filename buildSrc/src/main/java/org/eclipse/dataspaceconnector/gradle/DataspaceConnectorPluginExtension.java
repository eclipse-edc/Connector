package org.eclipse.dataspaceconnector.gradle;

import org.gradle.api.provider.Property;

abstract public class DataspaceConnectorPluginExtension {
    abstract public Property<String> getSeverity();

    public boolean isFailSeverity() {
        return "fail".equals(getSeverity().get());
    }
}
