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

package org.eclipse.edc.sql.translation;

import java.util.Optional;

/**
 * Implementation of the {@link SortFieldConverter}.
 */
public class SortFieldConverterImpl implements SortFieldConverter {

    private final TranslationMapping rootModel;

    public SortFieldConverterImpl(TranslationMapping rootModel) {
        this.rootModel = rootModel;
    }

    @Override
    public String convert(String sortField) {
        return Optional.ofNullable(rootModel.getFieldTranslator(sortField))
                .map(it -> it.apply(String.class))
                .orElse(null);
    }
}
