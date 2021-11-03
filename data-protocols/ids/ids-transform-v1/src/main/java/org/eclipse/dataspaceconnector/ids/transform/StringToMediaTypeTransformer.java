package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.CustomMediaTypeBuilder;
import de.fraunhofer.iais.eis.MediaType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class StringToMediaTypeTransformer implements IdsTypeTransformer<String, MediaType> {

    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public Class<MediaType> getOutputType() {
        return MediaType.class;
    }

    @Override
    public @Nullable MediaType transform(String object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        return new CustomMediaTypeBuilder()._filenameExtension_(object).build();
    }
}
