package org.eclipse.dataspaceconnector.spi.system.health;

import java.util.function.Supplier;

public interface StartupStatusProvider extends Supplier<HealthCheckResult> {
}
