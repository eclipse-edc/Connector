package org.eclipse.dataspaceconnector.ids.core.configuration;

import org.eclipse.dataspaceconnector.ids.spi.IdsId;

import java.net.URI;

public final class SettingDefaults {
    public static final String ID = IdsId.connector("edc").toUri().toString();
    public static final String TITLE = "Eclipse Dataspace Connector";
    public static final String DESCRIPTION = "Eclipse Dataspace Connector";
    public static final String MAINTAINER = "https://example.com";
    public static final String CURATOR = IdsId.participant("curator").getValue();
    public static final String ENDPOINT = "https://example.com";
    public static final String SECURITY_PROFILE = "BASE_SECURITY_PROFILE";
}
