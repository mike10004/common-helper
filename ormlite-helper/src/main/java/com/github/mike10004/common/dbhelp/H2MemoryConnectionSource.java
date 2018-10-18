package com.github.mike10004.common.dbhelp;

import com.google.common.base.CharMatcher;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of connection source for an H2 memory-only database.
 */
public class H2MemoryConnectionSource extends H2ConnectionSource {

    /**
     * Matcher of characters allowed in a new unique schema. Used by
     * {@link #createUniqueSchema() }.
     */
    @SuppressWarnings("deprecation")
    private static final CharMatcher allowedSchemaChars = CharMatcher.javaLetterOrDigit();
    
    private final String schema;
    private final boolean keepContentForLifeOfVM;

    public H2MemoryConnectionSource(boolean keepContentForLifeOfVM) {
        this(createUniqueSchema(), keepContentForLifeOfVM);
    }

    public H2MemoryConnectionSource(String schema, boolean keepContentForLifeOfVM) {
        this.schema = checkNotNull(schema);
        if (schema.isEmpty()) {
            throw new IllegalArgumentException("schema must be non-empty string");
        }
        this.keepContentForLifeOfVM = keepContentForLifeOfVM;
    }

    /**
     * Constructs a connection source with a new unique schema name. Content
     * is not retained for the life of VM.
     * @see #createUniqueSchema() 
     */
    public H2MemoryConnectionSource() {
        this(false);
    }
    
    /**
     * Constructs a connection source with the given schema name. Content
     * is not retained for the life of VM.
     * @param schema a non-null, non-empty string to use as the schema name
     */
    public H2MemoryConnectionSource(String schema) {
        this(schema, false);
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

    @Override
    protected String getJdbcUrlSuffix() {
        if (keepContentForLifeOfVM) {
            return ";DB_CLOSE_DELAY=-1";
        } else {
            return "";
        }
    }
}
