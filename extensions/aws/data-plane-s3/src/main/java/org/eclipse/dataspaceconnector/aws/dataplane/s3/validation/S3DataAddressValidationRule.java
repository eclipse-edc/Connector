package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.util.List;

import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.REGION;

public class S3DataAddressValidationRule implements ValidationRule<DataAddress> {

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        var composite = new CompositeValidationRule<>(
                List.of(
                        new EmptyValueValidationRule(BUCKET_NAME),
                        new EmptyValueValidationRule(REGION)
                )
        );

        return composite.apply(dataAddress.getProperties());
    }
}
