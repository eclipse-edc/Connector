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

package org.eclipse.dataspaceconnector.boot.system.injection.lifecycle;

import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

/**
 * Represents an {@link ServiceExtension}'s lifecycle phase where it's {@linkplain ServiceExtension#start()} method is invoked by the
 * {@link ExtensionLifecycleManager}.
 */
public class StartPhase extends Phase {

    protected StartPhase(Phase other) {
        super(other);
    }

    /**
     * Starts a {@link ServiceExtension}. This should only be done <em>after</em> the initialization phase is complete
     */
    protected void start() {
        var target = getTarget();
        target.start();
        monitor.info("Started " + target.name());
    }

}
