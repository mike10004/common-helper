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
package com.novetta.ibg.common.dbhelp;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author mike
 */
public class DefaultDatabaseContextTest {
    
    private DefaultDatabaseContext db;
    
    @Before
    public void initDb() throws SQLException {
        db = new DefaultDatabaseContext(new H2MemoryConnectionSource(DefaultDatabaseContextTest.class.getSimpleName()));
    }
    
    @Test
    public void testGetDao_uuidKey() throws Exception {
        db.getTableUtils().createTable(Thing.class);
        Thing t = new Thing("red");
        db.getDao(Thing.class).create(t);
        assertNotNull(t.thingId);
        Thing u = db.getDao(Thing.class).queryForSameId(t);
        assertEquals(t.thingId, u.thingId);
        List<String[]> thingColsRs = db.getDao(Thing.class).queryRaw("SHOW COLUMNS FROM Thing").getResults();
        assertNotNull(thingColsRs);
        assertEquals(2, thingColsRs.size());
        String[] idCol = thingColsRs.get(0);
        String[] colorCol = thingColsRs.get(1);
//        System.out.println("id column: " + Arrays.toString(idCol));
//        System.out.println("color column: " + Arrays.toString(colorCol));
        assertTrue("thingId".equalsIgnoreCase(idCol[0]));
        assertEquals("VARCHAR(48)", idCol[1]);
        assertTrue("color".equalsIgnoreCase(colorCol[0]));
        String[] thing1 = db.getDao(Thing.class).queryRaw("SELECT * FROM Thing WHERE thingId = ?", t.thingId.toString()).getFirstResult();
        assertNotNull(thing1);
        assertEquals(t.thingId.toString(), thing1[0]);
        assertEquals(t.thingId, UUID.fromString(thing1[0]));
    }
    
    @DatabaseTable
    public static class Thing {
        @DatabaseField(generatedId = true)
        public UUID thingId;
        @DatabaseField
        public String color;

        public Thing(String color) {
            this.color = color;
        }

        public Thing() {
        }
        
    }
    
}
