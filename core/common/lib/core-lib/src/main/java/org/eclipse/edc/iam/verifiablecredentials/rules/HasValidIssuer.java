/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.rules;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.spi.result.Result;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX;
import static org.eclipse.edc.protocol.spi.TrustedIssuer.WILDCARD_TYPE;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * A class that implements the {@link CredentialValidationRule} interface and checks if a {@link VerifiableCredential} has a trusted issuer,
 * and if the credential types are supported for this issuer.
 * <p>
 * By default, the issuer must support every type of the credential, except the base {@code VerifiableCredential} type. If
 * {@code matchAnyType} is set, it is enough that the issuer supports one of the types of the credential.
 */
public class HasValidIssuer implements CredentialValidationRule {
    /**
     * The base type of every credential, in compact form (JWT) and as expanded JSON-LD IRI.
     */
    private static final Set<String> BASE_TYPES = Set.of("VerifiableCredential", VC_PREFIX + "VerifiableCredential");

    private final DataspaceProfileContext profile;
    private final boolean matchAnyType;

    public HasValidIssuer(DataspaceProfileContext profile) {
        this(profile, false);
    }

    /**
     * Constructor for {@link HasValidIssuer}.
     *
     * @param profile      the dataspace profile containing the trusted issuers.
     * @param matchAnyType if true, a credential is accepted when any of its types is supported by the issuer.
     */
    public HasValidIssuer(DataspaceProfileContext profile, boolean matchAnyType) {
        this.profile = profile;
        this.matchAnyType = matchAnyType;
    }

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        var issuer = credential.getIssuer();
        if (issuer.id() == null) {
            return failure("VC's Issuer did not contain an 'id' field.");
        }

        var supportedTypes = profile.trustedIssuers().stream()
                .filter(i -> i.getId().equals(issuer.id())).findAny().map(TrustedIssuer::getSupportedTypes)
                .orElse(emptyList());

        if (supportedTypes.contains(WILDCARD_TYPE)) {
            return success();
        }

        var types = credential.getType();
        if (matchAnyType) {
            return Collections.disjoint(types, supportedTypes) ? notSupported(types, issuer.id()) : success();
        }

        var typesToCheck = types.stream().filter(type -> !BASE_TYPES.contains(type)).toList();
        if (typesToCheck.isEmpty()) {
            typesToCheck = types;
        }

        var unsupportedTypes = typesToCheck.stream().filter(type -> !supportedTypes.contains(type)).toList();
        if (typesToCheck.isEmpty() || !unsupportedTypes.isEmpty()) {
            return failure("%s (unsupported: %s)".formatted(notSupportedMessage(types, issuer.id()), unsupportedTypes));
        }
        return success();
    }

    private Result<Void> notSupported(List<String> types, String issuerId) {
        return failure(notSupportedMessage(types, issuerId));
    }

    private String notSupportedMessage(List<String> types, String issuerId) {
        return "Credential types '%s' are not supported for issuer '%s'".formatted(types, issuerId);
    }

}
