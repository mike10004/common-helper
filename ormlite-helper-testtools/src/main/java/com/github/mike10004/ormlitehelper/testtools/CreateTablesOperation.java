package com.github.mike10004.ormlitehelper.testtools;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.github.mike10004.common.dbhelp.DatabaseContext;

/**
 * Database setup operation that creates tables corresponding to entity classes.
 */
public class CreateTablesOperation implements DatabaseContextRule.SetupOperation {

    private final ImmutableList<Class<?>> entityClasses;

    /**
     * Constructs an instance of the rule. Invokes
     * {@link #CreateTablesOperation(java.lang.Iterable)}.
     * @param firstClass class representing a table to be created
     * @param otherClasses more classes representing tables to be created
     */
    public CreateTablesOperation(Class firstClass, Class...otherClasses) {
        this(Lists.<Class<?>>asList(firstClass, otherClasses));
    }

    /**
     * Constructs an instance of the rule.
     * @param <T> parameter that is irrelevant
     * @param entityClasses iterable over classes representing tables to be created
     */
    public <T> CreateTablesOperation(Iterable<Class<? extends T>> entityClasses) {
        this.entityClasses = ImmutableList.<Class<?>>copyOf(entityClasses);
        checkArgument(!this.entityClasses.isEmpty(), "entity classes list must be non-empty");
    }

    @Override
    public void perform(DatabaseContext db) throws Exception {
        db.getTableUtils().createAllTables(entityClasses);
    }
}
