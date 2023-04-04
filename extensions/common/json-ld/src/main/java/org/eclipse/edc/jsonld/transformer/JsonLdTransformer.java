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

package org.eclipse.edc.jsonld.transformer;

import org.eclipse.edc.transform.spi.TypeTransformer;

/**
 * Base type for transformers that operate on JSON-LD types. JSON-LD types (input and output) must be expanded per the JSON-LD Processing Algorithms API.
 * <p>
 * {@see https://www.w3.org/TR/json-ld11-api/}
 */
public interface JsonLdTransformer<INPUT, OUTPUT> extends TypeTransformer<INPUT, OUTPUT> {
}
