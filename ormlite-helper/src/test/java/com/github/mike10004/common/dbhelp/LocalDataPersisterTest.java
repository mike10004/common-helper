/*
 * The MIT License
 *
 * Copyright 2016 mike.
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
package com.github.mike10004.common.dbhelp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.H2DatabaseType;
import com.j256.ormlite.field.DataPersister;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseResults;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author mike
 */
public class LocalDataPersisterTest {
    
    @Test
    public void testUseOfLocalPersister() throws Exception {
        ConnectionSource connectionSource = new H2MemoryConnectionSource();
        DatabaseContext db = new LocalDatabaseContext(connectionSource, new LocalPersister());
        
        Dao<Foo, Integer> dao = db.getDao(Foo.class, Integer.class);
        TableUtils.createTable(connectionSource, Foo.class);
        assertEquals("count", 0L, dao.countOf());
        
        Foo recordWithNullWidget = new Foo();
        dao.create(recordWithNullWidget);
        assertEquals("count", 1L, dao.countOf());
        recordWithNullWidget = dao.queryForSameId(recordWithNullWidget);
        assertNull("recordWithNullWidget.widget", recordWithNullWidget.widget);
        
        Foo recordWithWidget = new Foo();
        recordWithWidget.widget = new Widget();
        dao.create(recordWithWidget);
        assertEquals("count", 2L, dao.countOf());
        recordWithWidget = dao.queryForSameId(recordWithWidget);
        //noinspection ConstantConditions
        assertTrue("instanceof Widget", recordWithWidget.widget instanceof Widget);
    }
    
    @Test
    public void testRawSerialization() throws Exception {
        ConnectionSource connectionSource = new H2MemoryConnectionSource();
        DatabaseContext db = new LocalDatabaseContext(connectionSource, new LocalPersister());
        Dao<Foo, Integer> dao = db.getDao(Foo.class, Integer.class);
        TableUtils.createTable(connectionSource, Foo.class);
        Foo foo = new Foo();
        foo.widget = new Widget();
        dao.create(foo);
        String[] rawRecord = dao.queryRaw("SELECT * FROM `Foo` WHERE fooId = ?", foo.fooId.toString()).getFirstResult();
        assertEquals(LocalPersister.SERIALIZED_WIDGET, rawRecord[1]);
        Foo deserializedFoo = dao.queryForId(foo.fooId);
        //noinspection ConstantConditions
        assertTrue(deserializedFoo.widget instanceof Widget);
    }

    @Test
    public void testDaoIsCached() throws Exception {
        ConnectionSource connectionSource = new H2MemoryConnectionSource();
        DatabaseContext db = new LocalDatabaseContext(connectionSource, new LocalPersister());
        Dao<Foo, Integer> dao1 = db.getDao(Foo.class, Integer.class);
        Dao<Foo, Integer> dao2 = db.getDao(Foo.class, Integer.class);
        assertSame("expect dao cached", dao1, dao2);
    }
    
    private static class LocalDatabaseContext extends DefaultDatabaseContext {
        
        private final DataPersister localPersister;
        private final Map<Class<?>, DatabaseTableConfig<?>> tableConfigs;
        
        public LocalDatabaseContext(ConnectionSource connectionSource, DataPersister localPersister) {
            super(connectionSource);
            this.localPersister = checkNotNull(localPersister);
            tableConfigs = new HashMap<>();
        }

        @Override
        public <T, K> Dao<T, K> getDao(Class<T> clazz, Class<K> keyType) throws SQLException {
            if (!isLocalPersisterRequired(clazz)) {
                return super.getDao(clazz, keyType);
            }
            DatabaseTableConfig<T> localTableConfig = loadTableConfig(clazz);
            return DaoManager.createDao(getConnectionSource(), localTableConfig);
        }

        @SuppressWarnings("unchecked")
        private @Nullable <T> DatabaseTableConfig<T> getCachedTableConfig(Class<T> clazz) {
            return (DatabaseTableConfig<T>) tableConfigs.get(clazz);
        }
        
        private synchronized <T> DatabaseTableConfig<T> loadTableConfig(Class<T> clazz) throws SQLException {
            if (Foo.class.equals(clazz)) {
                DatabaseTableConfig<T> dtc = getCachedTableConfig(clazz);
                if (dtc == null) {
                    DatabaseType databaseType = new H2DatabaseType(); // H2 is used in unit tests
                    String tableName = DatabaseTableConfig.extractTableName(databaseType, clazz);
                    List<DatabaseFieldConfig> fieldConfigs = buildFieldConfigs(clazz, tableName);
                    dtc = new DatabaseTableConfig<>(databaseType, clazz, fieldConfigs);
                    tableConfigs.put(clazz, dtc);
                }
                return dtc;
            } else {
                throw new IllegalArgumentException("local persister not supported for " + clazz);
            }
        }
        
        private static boolean isLocalPersisterRequired(Field field) {
            DatabaseField df = field.getAnnotation(DatabaseField.class);
            if (df != null && df.persisted() && LocalPersister.class.isAssignableFrom(df.persisterClass())) {
                return true;
            }
            return false;
        }
        
        private static final Predicate<Field> localPersisterRequiredPredicate = LocalDatabaseContext::isLocalPersisterRequired;
        
        private boolean isLocalPersisterRequired(Class<?> clazz) {
            return Iterables.any(Arrays.asList(clazz.getFields()), localPersisterRequiredPredicate::test);
        }

        private <T> List<DatabaseFieldConfig> buildFieldConfigs(Class<T> clazz, String tableName) throws SQLException {
            @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
            Iterable<Pair<Field, DatabaseField>> fields = Iterables.transform(Arrays.asList(clazz.getFields()), input -> {
                DatabaseField df = input.getAnnotation(DatabaseField.class);
                if (df != null) {
                    return Pair.of(input, df);
                }
                return null;
            });
            fields = Iterables.filter(fields, Objects::nonNull);
            ImmutableList.Builder<DatabaseFieldConfig> configs = ImmutableList.builder();
            for (Pair<Field, DatabaseField> p : fields) {
                Field field = p.getLeft();
                DatabaseField databaseField = p.getRight();
                DatabaseType databaseType = getConnectionSource().getDatabaseType();
                DatabaseFieldConfig config = DatabaseFieldConfig.fromDatabaseField(databaseType, tableName, field, databaseField);
                if (isLocalPersisterRequired(field)) {
                    config.setDataPersister(localPersister);
                } 
                configs.add(config);
            }
            return configs.build();
        }
        
    }
    
    public static class LocalPersister extends BaseDataType {
           public LocalPersister() {
               super(SqlType.LONG_STRING, new Class<?>[]{Object.class});
           }

        @Override
        public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
            return null;
        }

        @Override
        public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
            throw new UnsupportedOperationException("intermediate method obviated by this persister");
        }

        @Override
        public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
            throw new UnsupportedOperationException("intermediate method obviated by this persister");
        }

        @Override
        public Object resultToJava(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
            String serializedWidget = results.getString(columnPos);
            if (serializedWidget == null) {
                return null;
            }
            checkArgument(SERIALIZED_WIDGET.equals(serializedWidget), "expected %s but was %s", SERIALIZED_WIDGET, StringUtils.abbreviate(serializedWidget, 128));
            return new Widget();
        }
        
        @Override
        public @Nullable Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
            if (javaObject == null) {
                return null;
            }
            checkArgument(javaObject instanceof Widget, "not a widget: %s", javaObject.getClass());
            return SERIALIZED_WIDGET;
        }
        private static final String SERIALIZED_WIDGET = "I_am_Widget";
        
    }
    
    public static class Widget {
        private Widget() {
        }
    }
    
    @DatabaseTable
    public static class Foo {
        
        @DatabaseField(generatedId = true)
        public Integer fooId;
        
        @DatabaseField(persisterClass = LocalPersister.class)
        public Widget widget;
    }
    
}
