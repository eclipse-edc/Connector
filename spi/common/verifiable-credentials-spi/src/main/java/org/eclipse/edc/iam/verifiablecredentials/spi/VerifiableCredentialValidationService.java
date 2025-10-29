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

package org.eclipse.edc.iam.verifiablecredentials.spi;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.spi.result.Result;

import java.util.Collection;
import java.util.List;

/**
 * Aggregate service to perform all necessary validation of a list of {@link VerifiablePresentation} objects (typically,
 * when performing any sort of presentation request, the response contains an <em>array</em> of presentations).
 * <p>
 * This should include cryptographic verification of the presentations and all {@link org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential} objects contained
 * therein, as well as basic rule validation of the credentials, such as checking the validity period, revocation status, etc.
 * <p>
 * Implementations may choose to only support one format (LDP or JWT).
 */
public interface VerifiableCredentialValidationService {

    /**
     * Performs the validation of a list of {@link VerifiablePresentation} objects. Note that the result is only successful, if <em>all</em> presentations
     * are validated successfully.
     * <p>
     * Implementors must check at least the following:
     * <ul>
     *     <li>Cryptographic integrity of the presentation and all credentials. Note that this may involve remote calls, e.g. when resolving DIDs</li>
     *     <li>Dtructural integrity, e.g. JWT VPs <strong>must</strong> contain a {@code vp} claim</li>
     *     <li>Validity: the credentials must already be valid (e.g. {@code nbf} claim of a JWT-VC) and may not be expired</li>
     *     <li>Subject-IDs: the subject ID of every credential must match the {@code holder} field of the presentation</li>
     *     <li>Revocation: if a credential contains a {@code credentialStatus} object, the revocation status is checked. See {@link RevocationListService} for details.</li>
     *     <li>Valid issuer: the issuer of every credential is checked against a list of trusted issuers ({@link TrustedIssuerRegistry}).</li>
     * </ul>
     *
     * @param presentations         A list of {@link VerifiablePresentation} objects. Empty lists are interpreted as <em>valid</em>
     * @param audience              The expected audience of the presentations
     * @param additionalValidations An optional list of additional validation rules that are applied on the credentials. May be empty, never null.
     * @return {@link Result#success()} if (and only if) every presentation and every credential are determined to be valid.
     */
    default Result<Void> validate(List<VerifiablePresentationContainer> presentations, String audience, CredentialValidationRule... additionalValidations) {
        return validate(presentations, audience, List.of(additionalValidations));
    }

    /**
     * Performs the validation of a list of {@link VerifiablePresentation} objects. Note that the result is only successful, if <em>all</em> presentations
     * are validated successfully.
     * <p>
     * Implementors must check at least the following:
     * <ul>
     *     <li>Cryptographic integrity of the presentation and all credentials. Note that this may involve remote calls, e.g. when resolving DIDs</li>
     *     <li>Dtructural integrity, e.g. JWT VPs <strong>must</strong> contain a {@code vp} claim</li>
     *     <li>Validity: the credentials must already be valid (e.g. {@code nbf} claim of a JWT-VC) and may not be expired</li>
     *     <li>Subject-IDs: the subject ID of every credential must match the {@code holder} field of the presentation</li>
     *     <li>Revocation: if a credential contains a {@code credentialStatus} object, the revocation status is checked. See {@link RevocationListService} for details.</li>
     *     <li>Valid issuer: the issuer of every credential is checked against a list of trusted issuers ({@link TrustedIssuerRegistry}).</li>
     * </ul>
     *
     * @param presentations         A list of {@link VerifiablePresentation} objects. Empty lists are interpreted as <em>valid</em>
     * @param audience              The expected audience of the presentations
     * @param additionalValidations An optional list of additional validation rules that are applied on the credentials. May be empty, never null.
     * @return {@link Result#success()} if (and only if) every presentation and every credential are determined to be valid.
     */
    Result<Void> validate(List<VerifiablePresentationContainer> presentations, String audience, Collection<? extends CredentialValidationRule> additionalValidations);
}
