/*
 * The MIT License
 *
 * (c) 2015 Mike Chaberski.
 *
 * See LICENSE in base directory for distribution terms.
 *
 */
package com.novetta.ibg.common.dbhelp;

import com.google.common.base.CharMatcher;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.UUID;

/**
 * Implementation of connection source for an H2 memory-only database.
 * 
 * @author mchaberski
 */
public class H2MemoryConnectionSource extends H2ConnectionSource {

    /**
     * Matcher of characters allowed in a new unique schema. Used by
     * {@link #createUniqueSchema() }.
     */
    private static final CharMatcher allowedSchemaChars = CharMatcher.JAVA_LETTER_OR_DIGIT;
    
    private final String schema;

    /**
     * Constructs a connection source with a new unique schema name.
     * @see #createUniqueSchema() 
     */
    public H2MemoryConnectionSource() {
        this(createUniqueSchema());
    }
    
    /**
     * Constructs a connection source with the given schema name.
     * @param schema a non-null, non-empty string to use as the schema name
     */
    public H2MemoryConnectionSource(String schema) {
        this.schema = checkNotNull(schema);
        if (schema.isEmpty()) {
            throw new IllegalArgumentException("schema must be non-empty string");
        }
    }
    
    @Override
    protected String getProtocol() {
        return "jdbc:h2:mem";
    }

    @Override
    public String getSchema() {
        return schema;
    }
    
    /**
     * Creates a unique string usable as a schema name. Constructed with the
     * character 'u' plus a UUID (stripped of non-alphanumeric characters).
     * @return the unique string
     */
    protected static String createUniqueSchema() {
        String uuid = UUID.randomUUID().toString();
        uuid = allowedSchemaChars.retainFrom(uuid);
        String schema = 'u' + uuid;
        return schema;
    }
    
}
