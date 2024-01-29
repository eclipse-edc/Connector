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

package org.eclipse.edc.spi.query;

import java.util.List;

/**
 * Supported criterion operators
 */
public interface CriterionOperator {

    List<String> ALL = List.of(CriterionOperatorRegistry.EQUAL, CriterionOperatorRegistry.IN, CriterionOperatorRegistry.LIKE, CriterionOperatorRegistry.CONTAINS);
}
