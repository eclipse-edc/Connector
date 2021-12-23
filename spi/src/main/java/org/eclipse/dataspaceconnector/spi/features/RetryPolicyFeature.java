package org.eclipse.dataspaceconnector.spi.features;

import org.eclipse.dataspaceconnector.spi.system.Feature;

@Feature(RetryPolicyFeature.FEATURE)
public interface RetryPolicyFeature {
    String FEATURE = "edc:core:base:retry-policy";
}
