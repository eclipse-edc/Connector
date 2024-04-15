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

package org.eclipse.edc.protocol.dsp.http.spi.message;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;

/**
 * Provides serialization and deserialization for continuation token, used for resource pagination.
 */
public interface ContinuationTokenSerDes {

    /**
     * Serialize query spec to continuation token
     *
     * @param querySpec {@link QuerySpec}.
     * @return the token if success, failure otherwise.
     */
    Result<String> serialize(QuerySpec querySpec);

    /**
     * Deserialize continuation token to expanded json ld representation.
     *
     * @param serialized the continuation token.
     * @return the {@link JsonObject} if success, failure otherwise.
     */
    Result<JsonObject> deserialize(String serialized);
}
