package com.microsoft.dagx.demo.file.schema;

import com.microsoft.dagx.schema.DataSchema;
import com.microsoft.dagx.schema.SchemaAttribute;

public class FileSchema extends DataSchema {

    public static final String TYPE = "FileSchema";
    public static String TARGET_FILE_NAME = "targetFileName";
    public static String TARGET_DIRECTORY = "targetFolderName";
    public static String IS_COMPRESSION_REQUESTED = "isCompressionRequested";

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute(TARGET_FILE_NAME, true));
        attributes.add(new SchemaAttribute(TARGET_DIRECTORY, true));
        attributes.add(new SchemaAttribute(IS_COMPRESSION_REQUESTED, false));
    }

    @Override
    public String getName() {
        return TYPE;
    }

}