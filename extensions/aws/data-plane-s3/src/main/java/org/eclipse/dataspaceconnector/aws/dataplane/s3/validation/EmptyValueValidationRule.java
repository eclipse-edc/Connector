package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Map;

public class EmptyValueValidationRule implements ValidationRule<Map<String, String>> {

    private final String keyName;

    public EmptyValueValidationRule(String keyName) {
        this.keyName = keyName;
    }

    @Override
    public Result<Void> apply(Map<String, String> map) {
        return StringUtils.isNullOrBlank(map.get(keyName))
                ? Result.failure("Missing or invalid value for key " + keyName)
                : Result.success();
    }
}
