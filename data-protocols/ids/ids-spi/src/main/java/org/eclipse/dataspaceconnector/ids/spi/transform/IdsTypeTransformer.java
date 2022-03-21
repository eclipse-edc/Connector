/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.ids.spi.transform;

import org.eclipse.dataspaceconnector.spi.transformer.TypeTransformer;

/**
 * Implementations transform between an IDS and EDC type.
 */
public interface IdsTypeTransformer<INPUT, OUTPUT> extends TypeTransformer<INPUT, OUTPUT> {

    // marker interface for IDS transformers
}
