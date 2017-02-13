/*
 * (c) 2015 Mike Chaberski 
 */

package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Objects;

/**
 * Database entity representing a customer, used for testing purposes
 * only.
 * @author mchaberski
 */
@DatabaseTable
public class Customer {
    
    @DatabaseField(generatedId = true)
    public Integer id;
    
    @DatabaseField
    public String name;
    
    @DatabaseField
    public String address;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.address);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Customer other = (Customer) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Customer{" + "id=" + id + ", name=" + name + ", address=" + address + '}';
    }
    
}
