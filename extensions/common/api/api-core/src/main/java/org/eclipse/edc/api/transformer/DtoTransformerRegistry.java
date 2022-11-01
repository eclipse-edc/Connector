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

package org.eclipse.edc.api.transformer;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

/**
 * Marker interface to allow for a type-safe registry that only holds implementations of the {@link DtoTransformer} interface.
 * This is useful because the registry should be registered in the {@link ServiceExtensionContext}
 */
@ExtensionPoint
public interface DtoTransformerRegistry extends TypeTransformerRegistry<DtoTransformer<?, ?>> {
}
