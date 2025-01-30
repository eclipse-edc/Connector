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

public record CredentialSchema(String id, String type) {
    public static final String CREDENTIAL_SCHEMA_ID_PROPERTY = "@id";
    public static final String CREDENTIAL_SCHEMA_TYPE_PROPERTY = "@type";

}
