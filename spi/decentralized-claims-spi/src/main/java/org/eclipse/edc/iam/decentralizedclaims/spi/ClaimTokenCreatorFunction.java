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

package org.eclipse.edc.iam.decentralizedclaims.spi;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;

import java.util.List;
import java.util.function.Function;

/**
 * This is a marker interface for functions that convert a {@link List} of {@link VerifiableCredential}s to a ClaimToken.
 * Implementors decide which information should be extracted from a {@link VerifiableCredential} and how it should be represented
 * inside the {@link ClaimToken}.
 * For example, an implementor could choose to simply attach the entire list of credentials to the ClaimToken using a reasonable key.
 */
public interface ClaimTokenCreatorFunction extends Function<List<VerifiableCredential>, Result<ClaimToken>> {
}
