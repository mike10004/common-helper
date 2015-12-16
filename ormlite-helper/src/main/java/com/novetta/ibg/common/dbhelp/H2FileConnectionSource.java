/*
 * The MIT License
 *
 * (c) 2015 Mike Chaberski.
 *
 * See LICENSE in base directory for distribution terms.
 *
 */
package com.novetta.ibg.common.dbhelp;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import java.io.File;

/**
 * Class that implements a connection source using an H2 database reading from
 * and writing to a file.
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