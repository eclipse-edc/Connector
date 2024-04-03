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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice;

/**
 * Represents the {@code descriptor_map} of a <a href="https://identity.foundation/presentation-exchange/spec/v2.0.0/#presentation-submission">Presentation Submission</a>
 */
public record InputDescriptorMapping(String id, String format, String path) {
}
