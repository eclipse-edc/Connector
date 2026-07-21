/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.dataplane.spi.strategy;

import java.util.Collection;

/**
 * List that contains all {@link SelectionStrategy} instances available to a selector runtime
 */
public interface SelectionStrategyRegistry {
    SelectionStrategy find(String strategyName);

    void add(SelectionStrategy strategy);

    Collection<String> getAll();
}

