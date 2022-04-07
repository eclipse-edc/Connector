package org.eclipse.dataspaceconnector.aws.dataplane.s3;

import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

public class TestFunctions {

    public static final String VALID_REGION = "validRegion";
    public static final String VALID_BUCKET_NAME = "validBucketName";
    public static final String VALID_ACCESS_KEY_ID = "validAccessKeyId";
    public static final String VALID_SECRET_ACCESS_KEY = "validSecretAccessKey";

    public static DataAddress validS3DataAddress() {
        return DataAddress.Builder.newInstance()
                .type(S3BucketSchema.TYPE)
                .property(S3BucketSchema.BUCKET_NAME, VALID_BUCKET_NAME)
                .property(S3BucketSchema.REGION, VALID_REGION)
                .property(S3BucketSchema.ACCESS_KEY_ID, VALID_ACCESS_KEY_ID)
                .property(S3BucketSchema.SECRET_ACCESS_KEY, VALID_SECRET_ACCESS_KEY)
                .build();
    }
}
