/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *
 */

package org.eclipse.dataspaceconnector.ids.spi.types;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * DTO representing IDS Security Profile
 */
public enum SecurityProfile {
    BASE_SECURITY_PROFILE("base"),
    TRUST_SECURITY_PROFILE("trust"),
    TRUST_PLUS_SECURITY_PROFILE("trust-plus");

    private final String value;

    SecurityProfile(String value) {
        this.value = value;
    }

    public static SecurityProfile fromValue(String value) {
        for (SecurityProfile securityProfile : SecurityProfile.values()) {
            if (securityProfile.value.equalsIgnoreCase(value)) {
                return securityProfile;
            }
        }

        throw new IllegalArgumentException(
                String.format(
                        "IDS Settings: Invalid security profile value '%s'. Valid security profile values: [%s]",
                        value,
                        Arrays.stream(SecurityProfile.values()).map(it -> it.value).collect(Collectors.joining(", "))
                )
        );
    }

    public String getValue() {
        return value;
    }
}
