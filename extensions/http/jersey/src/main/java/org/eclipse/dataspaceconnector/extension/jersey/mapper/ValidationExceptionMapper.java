package org.eclipse.dataspaceconnector.extension.jersey.mapper;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.eclipse.dataspaceconnector.spi.ApiErrorDetail;

import java.util.Objects;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var errors = exception.getConstraintViolations().stream()
                .map(violation -> ApiErrorDetail.Builder.newInstance()
                        .message(violation.getMessage())
                        .type(violation.getMessageTemplate())
                        .path(violation.getPropertyPath().toString())
                        .value(Objects.toString(violation.getInvalidValue()))
                        .build())
                .collect(Collectors.toList());

        return Response
                .status(BAD_REQUEST)
                .entity(errors)
                .build();
    }
}
