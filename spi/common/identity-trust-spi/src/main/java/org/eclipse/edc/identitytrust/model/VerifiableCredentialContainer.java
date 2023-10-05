/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.identitytrust.model;

/**
 * This container object is intended to hold a {@link VerifiableCredential}, its raw representation and which format it is in.
 *
 * @param rawVc      A String containing the VC in its raw format. This must be exactly how it was originally received by the issuer.
 * @param format     indicates whether the VC is present in JWT or JSON-LD format
 * @param credential the {@link VerifiableCredential}, as it was deserialized from the raw VC string. Note that JSON-LD and JWT VCs
 *                   have to be deserialized differently
 */
public record VerifiableCredentialContainer(String rawVc, CredentialFormat format, VerifiableCredential credential) {
}
