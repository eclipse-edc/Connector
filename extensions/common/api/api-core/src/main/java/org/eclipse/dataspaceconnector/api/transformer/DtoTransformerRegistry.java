/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.transformer;

import org.eclipse.dataspaceconnector.spi.transformer.TypeTransformerRegistry;

/**
 * Marker interface to allow for a type-safe registry that only holds implementations of the {@link DtoTransformer} interface.
 * This is useful because the registry should be registered in the {@link org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext}
 */
public interface DtoTransformerRegistry extends TypeTransformerRegistry<DtoTransformer<?, ?>> {
}
