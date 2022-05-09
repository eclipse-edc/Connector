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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.system.health;

/**
 * Provides information about the connector's health status.
 * The three different health aspects are based on <a href=https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#types-of-probe>Kubernetes' definition.</a>
 * <p>
 * Implementors should not forward every incoming request directly to the providers, since that does not scale, might
 * cause bottlenecks and can clog the system. Instead, all providers should be queried ("crawled") in a periodic manner and the results
 * should be cached internally.
 */
public interface HealthCheckService {

    void addLivenessProvider(LivenessProvider provider);

    void addReadinessProvider(ReadinessProvider provider);

    void addStartupStatusProvider(StartupStatusProvider provider);

    HealthStatus isLive();

    HealthStatus isReady();

    HealthStatus getStartupStatus();

    void refresh();
}
