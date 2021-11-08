package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.badParameters;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.messageTypeNotSupported;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.notFound;

class MultipartResponseUtil {

    public static MultipartResponse createBadParametersErrorMultipartResponse(@Nullable String connectorId, @Nullable Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(badParameters(message, connectorId))
                .build();
    }

    public static MultipartResponse createNotFoundErrorMultipartResponse(@Nullable String connectorId, @Nullable Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(notFound(message, connectorId))
                .build();
    }

    public static MultipartResponse createErrorMultipartResponse(@Nullable String connectorId, @Nullable Message message) {
        return MultipartResponse.Builder.newInstance()
                .header(messageTypeNotSupported(message, connectorId))
                .build();
    }
}
