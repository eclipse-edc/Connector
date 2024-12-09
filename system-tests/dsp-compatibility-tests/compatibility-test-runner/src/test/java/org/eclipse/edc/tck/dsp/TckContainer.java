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

package org.eclipse.edc.tck.dsp;

import org.testcontainers.containers.GenericContainer;

public class TckContainer<SELF extends TckContainer<SELF>> extends GenericContainer<SELF> {
    public TckContainer(String imageName) {
        super(imageName);
        addFixedExposedPort(8083, 8083); // TCK will use this as callback address - must be fixed!
    }

}
