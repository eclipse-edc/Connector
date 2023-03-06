package org.eclipse.edc.service.spi.result;

import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.NOT_FOUND;

class ServiceResultTest {

    @Test
    void verifyFromStorageResult() {
        var f = ServiceResult.from(StoreResult.notFound("test-message"));
        assertThat(f.reason()).isEqualTo(NOT_FOUND);
        assertThat(f.succeeded()).isFalse();

        var f2 = ServiceResult.from(StoreResult.alreadyExists("test-message"));
        assertThat(f2.reason()).isEqualTo(CONFLICT);
        assertThat(f2.succeeded()).isFalse();

        assertThat(ServiceResult.from(StoreResult.success("test-message"))).extracting(ServiceResult::succeeded).isEqualTo(true);
    }
}