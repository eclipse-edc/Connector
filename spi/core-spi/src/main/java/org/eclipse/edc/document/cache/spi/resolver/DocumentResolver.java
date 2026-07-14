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

package org.eclipse.edc.document.cache.spi.resolver;

import jakarta.json.JsonObject;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

/**
 * Fetches a cached document (a JSON-LD context or a JSON schema) from its {@code url}. The default implementation
 * resolves the document over http/https; extensions may provide an alternative by overriding the default {@code @Provider}.
 */
@ExtensionPoint
public interface DocumentResolver {

    /**
     * Obtains a fresh copy of the JSON document located at the given url.
     *
     * @param url the context url.
     * @return a successful {@link Result} with the document content, or a failure.
     */
    Result<JsonObject> resolve(String url);

}
