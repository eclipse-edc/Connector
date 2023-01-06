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

package org.eclipse.edc.protocol.ids.transform;

import org.eclipse.edc.protocol.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistryImpl;

/**
 * Marker class to allow for a type-safe registry that only holds implementations of the {@link IdsTypeTransformer} interface.
 */
public class IdsTransformerRegistryImpl extends TypeTransformerRegistryImpl<IdsTypeTransformer<?, ?>> implements IdsTransformerRegistry {

}
