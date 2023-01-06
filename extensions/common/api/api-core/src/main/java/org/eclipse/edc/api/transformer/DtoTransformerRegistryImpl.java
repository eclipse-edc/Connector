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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.api.transformer;

import org.eclipse.edc.transform.spi.TypeTransformerRegistryImpl;

/**
 * Marker class to allow for a type-safe registry that only holds implementations of the {@link DtoTransformer} interface.
 */
public class DtoTransformerRegistryImpl extends TypeTransformerRegistryImpl<DtoTransformer<?, ?>> implements DtoTransformerRegistry {

}
