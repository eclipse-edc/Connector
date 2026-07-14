/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.document.cache.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The kind of document held by a {@link CachedDocument}. It determines how the cached content is made
 * available at runtime: a {@link #JSON_LD} document is registered into the {@code JsonLd} service, whereas a
 * {@link #JSON_SCHEMA} document is served to the management API schema validator. The string representation is
 * the enum name in {@code SCREAMING_SNAKE_CASE}.
 */
public enum CachedDocumentType {

    /**
     * A JSON-LD context document, registered into the {@code JsonLd} service so it can be resolved during
     * expansion/compaction.
     */
    JSON_LD,

    /**
     * A JSON schema document, served to the management API schema validator so it can be resolved locally
     * instead of being fetched over the network.
     */
    JSON_SCHEMA;

    @JsonCreator
    public static CachedDocumentType fromValue(String value) {
        return CachedDocumentType.valueOf(value);
    }

    @JsonValue
    public String value() {
        return name();
    }
}
