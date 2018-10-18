package com.github.mike10004.common.dbhelp;

import java.util.function.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract superclass of connection source implementations that utilize an H2 file database.
 */
public abstract class AbstractH2FileConnectionSource extends H2ConnectionSource {
    
    private static final Joiner semicolonJoiner = Joiner.on(';');
    protected static final Function<File, String> defaultSchemaTransformInstance = new DefaultSchemaTransform();
    private boolean autoMixedMode;
    
    public static class DefaultSchemaTransform implements Function<File, String> {

        static final String ILLEGAL_DB_FILENAME_MESSAGE = "h2 database filenames must end with '.h2.db'";
        
        public static final String REQUIRED_SUFFIX = ".h2.db";
        
        @Override
        public String apply(File databaseFile) {
            String normalizedFilename = FilenameUtils.normalize(databaseFile.getName());
            Preconditions.checkArgument(normalizedFilename.endsWith(REQUIRED_SUFFIX), 
                    ILLEGAL_DB_FILENAME_MESSAGE);
            String filenameWithoutSuffix = StringUtils
                    .removeEnd(normalizedFilename, REQUIRED_SUFFIX);
            File fileWithoutSuffix = new File(databaseFile.getParentFile(), 
                    filenameWithoutSuffix).getAbsoluteFile();
            String uriPathPart = FilenameUtils.normalize(fileWithoutSuffix.getPath(), true);
            String schema = FilenameUtils.normalize(uriPathPart, true); // unix separator
            return schema;
        }
        
    }

    @Override
    protected String getProtocol() {
        return "jdbc:h2:file";
    }

    @Override
    protected String getJdbcUrlSuffix() {
        List<String> clauses = new ArrayList<>();
        if (isAutoMixedMode()) {
            clauses.add("AUTO_SERVER=true");
        }
        if (clauses.isEmpty()) {
            return "";
        } else {
            int totalLength = 0;
            for (String clause : clauses) {
                totalLength += clause.length();
            }
            StringBuilder sb = new StringBuilder(1 + clauses.size() + totalLength);
            sb.append(';');
            semicolonJoiner.appendTo(sb, clauses);
            return sb.toString();
        }
    }

    public boolean isAutoMixedMode() {
        return autoMixedMode;
    }

    public void setAutoMixedMode(boolean autoMixedMode) {
        this.autoMixedMode = autoMixedMode;
    }
    
}
