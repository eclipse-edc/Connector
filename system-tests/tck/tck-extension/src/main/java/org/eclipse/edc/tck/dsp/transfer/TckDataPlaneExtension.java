/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp.transfer;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

public class TckDataPlaneExtension implements ServiceExtension {

    @Provider
    public DataFlowController dataFlowController() {
        return new NoOpDataFlowController();
    }

}
