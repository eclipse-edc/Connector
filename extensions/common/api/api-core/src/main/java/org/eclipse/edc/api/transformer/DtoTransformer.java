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

import org.eclipse.edc.transform.spi.TypeTransformer;

/**
 * Marker interface to group transformers which are intended for use in API controllers, i.e. converting business objects
 * into DTOs
 *
 * @param <INPUT>  the type of input object. Usually this is a business object.
 * @param <OUTPUT> the type that the input gets converted into. Usually this is a DTO.
 */
public interface DtoTransformer<INPUT, OUTPUT> extends TypeTransformer<INPUT, OUTPUT> {
}
