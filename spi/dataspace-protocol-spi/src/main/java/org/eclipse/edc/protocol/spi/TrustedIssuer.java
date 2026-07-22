/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a trusted credential issuer in a dataspace profile. A trusted issuer is identified
 * by its DID and the credential types it is authorized to issue.
 */
public class TrustedIssuer {

    public static final String TRUSTED_ISSUER_TYPE_TERM = "TrustedIssuer";
    public static final String TRUSTED_ISSUER_TYPE_IRI = EDC_NAMESPACE + TRUSTED_ISSUER_TYPE_TERM;

    public static final String TRUSTED_ISSUER_SUPPORTED_TYPES_TERM = "supportedTypes";
    public static final String TRUSTED_ISSUER_SUPPORTED_TYPES_IRI = EDC_NAMESPACE + TRUSTED_ISSUER_SUPPORTED_TYPES_TERM;

    private String id;
    private final List<String> supportedTypes = new ArrayList<>();

    public String getId() {
        return id;
    }

    public List<String> getSupportedTypes() {
        return supportedTypes;
    }

    public static final class Builder {

        private final TrustedIssuer instance = new TrustedIssuer();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {

        }

        public TrustedIssuer build() {
            Objects.requireNonNull(instance.id, "Trusted Issuer id cannot be null");
            return instance;
        }

        public Builder id(String id) {
            instance.id = id;
            return this;
        }

        public Builder supportedTypes(List<String> supportedTypes) {
            if (supportedTypes != null) {
                instance.supportedTypes.addAll(supportedTypes);
            }
            return this;
        }

    }
}
