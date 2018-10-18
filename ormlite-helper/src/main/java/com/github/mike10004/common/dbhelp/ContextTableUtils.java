package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface for table utils objects that are associated with a specific
 * database context. This interface duplicates the functionality of 
 * {@link TableUtils} but doesn't require the connection source
 * parameter that those methods require.
 *
 * @see TableUtils
 */
public interface ContextTableUtils {

    /**
     * Clear all data out of the table. For certain database types and with large sized tables, which may take a long
     * time. In some configurations, it may be faster to drop and re-create the table.
     *
     * <p>
     * <b>WARNING:</b> This is (obviously) very destructive and is unrecoverable.
     * </p>
     * @param <T> entity type
     * @param dataClass entity type
     * @return number of statements executed to perform this action
     * @throws java.sql.SQLException on errors clearing table data
     * @see TableUtils#clearTable(com.j256.ormlite.support.ConnectionSource, java.lang.Class) 
     */
    <T> int clearTable(Class<T> dataClass) throws SQLException;

    /**
     * Clear all data out of the table. For certain database types and with 
     * large sized tables, which may take a long time. In some configurations, 
     * it may be faster to drop and re-create the table.
     *
     * <p>
     * <b>WARNING:</b> This is [obviously] very destructive and is unrecoverable.
     * </p>
     * @param <T> the entity type
     * @param tableConfig the table config
     * @return number of statements executed to perform this action
     * @throws java.sql.SQLException on errors clearing table data
     */
    <T> int clearTable(DatabaseTableConfig<T> tableConfig) throws SQLException;

    /**
     * Issue the database statements to create the table associated with a class.
     *
     * @param <T> entity type
     * @param dataClass entity class for which a table will be created.
     * @return number of statements executed to perform this action
     * @throws java.sql.SQLException on errors creating the table
     */
    <T> int createTable(Class<T> dataClass) throws SQLException;

    /**
     * Issue the database statements to create the table associated with a table configuration.
     *
     * @param <T> entity type
     * @param tableConfig hand or Spring-wired table configuration; if null 
     * then the class must have {@link DatabaseField} annotations.
     * @return number of statements executed to perform this action
     * @throws java.sql.SQLException if creating the table fails
     */
    <T> int createTable(DatabaseTableConfig<T> tableConfig) throws SQLException;

    /**
     * Create a table if it does not already exist. This is not supported by all databases.
     * @param <T> entity type
     * @param dataClass the entity type
     * @return number of statements executed to perform this action
     * @throws java.sql.SQLException on errors creating the table
     */
    <T> int createTableIfNotExists(Class<T> dataClass) throws SQLException;

    /**
     * Create multiple tables except those which already exist.
     * @param dataClasses entity classes
     * @return number of statements executed to perform this action
     * @throws SQLException on errors creating the tables
     */
    int createAllTablesIfNotExists(Iterable<Class<?>> dataClasses) throws SQLException;
    
    /**
     * Create multiple tables except those which already exist.
     * @param dataClasses entity classes
     * @return number of statements executed to perform this action
     * @throws SQLException  on errors creating the tables
     */
    int createAllTables(Iterable<Class<?>> dataClasses) throws SQLException;
    
    /**
     * Create a table if it does not already exist. This is not supported by all databases.
     * @param <T> the entity type
     * @param tableConfig the table configuration
     * @return number of statements executed in performing this action
     * @throws java.sql.SQLException on errors creating the table
     */
    <T> int createTableIfNotExists(DatabaseTableConfig<T> tableConfig) throws SQLException;

    /**
     * Issue the database statements to drop the table associated with a class.
     *
     * <p>
     * <b>WARNING:</b> This is [obviously] very destructive and is unrecoverable.
     * </p>
     *
     * @param <T> entity type
     * @param <ID> primary key type
     * @param dataClass
     *            The class for which a table will be dropped.
     * @param ignoreErrors
     *            If set to true then try each statement regardless of {@link SQLException} thrown previously.
     * @return The number of statements executed to do so.
     * @throws java.sql.SQLException on errors dropping the table
     */
    <T, ID> int dropTable(Class<T> dataClass, boolean ignoreErrors) throws SQLException;

    /**
     * Issue the database statements to drop the table associated with a table configuration.
     *
     * <p>
     * <b>WARNING:</b> This is [obviously] very destructive and is unrecoverable.
     * </p>
     *
     * @param <T> entity type
     * @param <ID> primary key type
     * @param tableConfig
     *            Hand or spring wired table configuration. If null then the class must have {@link DatabaseField}
     *            annotations.
     * @param ignoreErrors
     *            If set to true then try each statement regardless of {@link SQLException} thrown previously.
     * @return The number of statements executed to do so.
     * @throws java.sql.SQLException on database error
     */
    <T, ID> int dropTable(DatabaseTableConfig<T> tableConfig, boolean ignoreErrors) throws SQLException;

    /**
     * Return an ordered collection of SQL statements that need to be run to create a table. To do the work of creating,
     * you should call {@link #createTable}.
     *
     * @param <T> entity type
     * @param <ID> primary key type
     * @param dataClass
     *            The class for which a table will be created.
     * @return The collection of table create statements.
     * @throws java.sql.SQLException on database error
     */
    <T, ID> List<String> getCreateTableStatements(Class<T> dataClass) throws SQLException;

    /**
     * Return an ordered collection of SQL statements that need to be run to create a table. To do the work of creating,
     * you should call {@link #createTable}.
     *
     * @param <T> entity type
     * @param <ID> primary key type
     * @param tableConfig
     *            Hand or spring wired table configuration. If null then the class must have {@link DatabaseField}
     *            annotations.
     * @return The collection of table create statements.
     * @throws java.sql.SQLException on database error
     */
    <T, ID> List<String> getCreateTableStatements(DatabaseTableConfig<T> tableConfig) throws SQLException;
    
}
