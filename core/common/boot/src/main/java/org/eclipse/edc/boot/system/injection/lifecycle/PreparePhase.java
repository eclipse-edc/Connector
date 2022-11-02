/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.boot.system.injection.lifecycle;

import org.eclipse.edc.spi.system.ServiceExtension;

/**
 * Represents an {@link ServiceExtension}'s lifecycle phase where it's {@linkplain ServiceExtension#prepare()} method is
 * invoked by the {@link ExtensionLifecycleManager}.
 */
public class PreparePhase extends Phase {
    protected PreparePhase(Phase other) {
        super(other);
    }

    public void prepare() {
        var target = getTarget();
        target.prepare();
        monitor.info("Prepared " + target.name());
    }
}
