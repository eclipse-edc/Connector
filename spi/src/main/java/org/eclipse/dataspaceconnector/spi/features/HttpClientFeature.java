package org.eclipse.dataspaceconnector.spi.features;

import org.eclipse.dataspaceconnector.spi.system.Feature;

@Feature(HttpClientFeature.FEATURE)
public interface HttpClientFeature {
    String FEATURE = "edc:core:base:http-client";
}
