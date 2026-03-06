/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.directory;

import org.eclipse.edc.catalog.spi.testfixtures.TargetNodeDirectoryTestBase;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;

class InMemoryNodeDirectoryTest extends TargetNodeDirectoryTestBase {

    private final InMemoryNodeDirectory store = new InMemoryNodeDirectory();

    @Override
    protected TargetNodeDirectory getStore() {
        return store;
    }

}