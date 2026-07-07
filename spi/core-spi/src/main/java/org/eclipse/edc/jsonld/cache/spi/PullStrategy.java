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

package org.eclipse.edc.jsonld.cache.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Determines <em>when</em> the content of a {@link CachedJsonLdContext} is (re)fetched from its {@code url},
 * mirroring the semantics of Docker's {@code --pull} policy. The string representation is the enum name in
 * {@code SCREAMING_SNAKE_CASE}.
 */
public enum PullStrategy {

    /**
     * Never fetch from the url; rely solely on the {@code content} supplied at creation.
     */
    NEVER,

    /**
     * Fetch from the url only when no {@code content} is present; otherwise keep the supplied content.
     */
    IF_NOT_PRESENT,

    /**
     * Fetch from the url immediately at creation and re-fetch on every scheduled refresh run.
     */
    ALWAYS;

    @JsonCreator
    public static PullStrategy fromValue(String value) {
        return PullStrategy.valueOf(value);
    }

    @JsonValue
    public String value() {
        return name();
    }
}
