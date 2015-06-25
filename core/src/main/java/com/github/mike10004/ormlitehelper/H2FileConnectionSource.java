/*
 * (c) 2015 Mike Chaberski
 */
package com.github.mike10004.ormlitehelper;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author mchaberski
 */
public class H2FileConnectionSource extends AbstractH2FileConnectionSource {

    private final File databaseFile;
    private final String schema;
    
    public H2FileConnectionSource(File databaseFile) {
        this(databaseFile, defaultSchemaTransformInstance);
    }
    
    protected H2FileConnectionSource(File databaseFile, Function<File, String> schemaTransform) {
        this.databaseFile = checkNotNull(databaseFile);
        schema = schemaTransform.apply(databaseFile);
        checkNotNull(schema);
    }
    
    @Override
    protected String getSchema() {
        return schema;
    }

    public File getDatabaseFile() {
        return databaseFile;
    }

}
