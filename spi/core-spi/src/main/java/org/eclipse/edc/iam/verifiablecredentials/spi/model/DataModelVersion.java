/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.spi.model;

/**
 * Indicates, whether a {@link VerifiableCredential} or a {@link VerifiablePresentation} conform to the VC DataModel
 * V1.1 or V2.0.
 * The decision should be made based on the {@code @context} object, defaulting to V1.1
 *
 * @see <a href="https://www.w3.org/TR/vc-data-model">VC DataModel 1.1</a>
 * @see <a href="https://www.w3.org/TR/vc-data-model-2.0">VC DataModel 2.0</a>
 */
public enum DataModelVersion {
    V_1_1,
    V_2_0
}
