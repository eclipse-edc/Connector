package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.ACCESS_KEY_ID;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.REGION;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.SECRET_ACCESS_KEY;

public class S3DataAddressValidationRule implements ValidationRule<DataAddress> {

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        var properties = dataAddress.getProperties();

        var rules = Stream.of(
                new EmptyValueValidationRule(BUCKET_NAME),
                new EmptyValueValidationRule(REGION),
                new EmptyValueValidationRule(ACCESS_KEY_ID),
                new EmptyValueValidationRule(SECRET_ACCESS_KEY)
        );

        var failed = rules.map(p -> p.apply(properties)).filter(AbstractResult::failed).collect(Collectors.toList());
        if (failed.isEmpty()) {
            return Result.success();
        } else {
            var errorMessages = failed.stream().flatMap(it -> it.getFailureMessages().stream()).collect(Collectors.toList());
            return Result.failure(errorMessages);
        }
    }
}
