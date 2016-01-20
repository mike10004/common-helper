package com.github.mike10004.ormlitehelper.testtools;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Entity class used for unit tests.
 */
@DatabaseTable
public class Widget {

    public Widget() {
    }

    public Widget(String color) {
        this.color = color;
    }

    @DatabaseField(generatedId = true)
    public Integer id;

    @DatabaseField
    public String color;
}
