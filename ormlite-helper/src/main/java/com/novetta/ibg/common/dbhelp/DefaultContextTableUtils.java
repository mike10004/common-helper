/*
 * The MIT License
 *
 * Copyright 2015 mchaberski.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.novetta.ibg.common.dbhelp;

import com.google.common.base.Preconditions;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.List;

/**
 * Class that implements default context table utilities.
 * @author mchaberski
 */
public class DefaultContextTableUtils implements ContextTableUtils {
    
    private final ConnectionSource connectionSource;

    /**
     * Constructs an instance with a given connection source.
     * @param connectionSource the connection source
     */
    public DefaultContextTableUtils(ConnectionSource connectionSource) {
        this.connectionSource = Preconditions.checkNotNull(connectionSource, "connectionSource");
    }

    protected ConnectionSource getConnectionSource() {
        return connectionSource;
    }

    @Override
    public <T> int createTable(Class<T> dataClass) throws SQLException {
        return TableUtils.createTable(getConnectionSource(), dataClass);
    }

    @Override
    public <T> int createTableIfNotExists(Class<T> dataClass) throws SQLException {
        return TableUtils.createTableIfNotExists(getConnectionSource(), dataClass);
    }

    @Override
    public <T> int createTable(DatabaseTableConfig<T> tableConfig) throws SQLException {
        return TableUtils.createTable(getConnectionSource(), tableConfig);
    }

    @Override
    public <T> int createTableIfNotExists(DatabaseTableConfig<T> tableConfig) throws SQLException {
        return TableUtils.createTableIfNotExists(getConnectionSource(), tableConfig);
    }

    @Override
    public int createAllTablesIfNotExists(Iterable<Class<?>> dataClasses) throws SQLException {
        int totalNumStatementsExecuted = 0;
        for (Class<?> clz : dataClasses) {
            totalNumStatementsExecuted += createTableIfNotExists(clz);
        }
        return totalNumStatementsExecuted;
    }

    @Override
    public int createAllTables(Iterable<Class<?>> dataClasses) throws SQLException {
        int totalNumStatementsExecuted = 0;
        for (Class<?> clz : dataClasses) {
            totalNumStatementsExecuted += createTable(clz);
        }
        return totalNumStatementsExecuted;
    }

    @Override
    public <T, ID> List<String> getCreateTableStatements(Class<T> dataClass) throws SQLException {
        return TableUtils.getCreateTableStatements(getConnectionSource(), dataClass);
    }

    @Override
    public <T, ID> List<String> getCreateTableStatements(DatabaseTableConfig<T> tableConfig) throws SQLException {
        return TableUtils.getCreateTableStatements(getConnectionSource(), tableConfig);
    }

    @Override
    public <T, ID> int dropTable(Class<T> dataClass, boolean ignoreErrors) throws SQLException {
        return TableUtils.dropTable(getConnectionSource(), dataClass, ignoreErrors);
    }

    @Override
    public <T, ID> int dropTable(DatabaseTableConfig<T> tableConfig, boolean ignoreErrors) throws SQLException {
        return TableUtils.dropTable(getConnectionSource(), tableConfig, ignoreErrors);
    }

    @Override
    public <T> int clearTable(Class<T> dataClass) throws SQLException {
        return TableUtils.clearTable(getConnectionSource(), dataClass);
    }

    @Override
    public <T> int clearTable(DatabaseTableConfig<T> tableConfig) throws SQLException {
        return TableUtils.clearTable(getConnectionSource(), tableConfig);
    }
    
}
