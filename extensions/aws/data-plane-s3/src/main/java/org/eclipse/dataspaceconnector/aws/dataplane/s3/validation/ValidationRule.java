package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Map;
import java.util.function.Function;

interface ValidationRule<T> extends Function<T, Result<Void>> {
}
