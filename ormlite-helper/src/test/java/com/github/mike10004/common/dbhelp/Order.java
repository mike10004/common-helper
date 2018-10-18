package com.github.mike10004.common.dbhelp;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Objects;

/**
 * Database entity representing a customer order, used for testing purposes only.
 */
@DatabaseTable
public class Order {
    
    @DatabaseField(generatedId = true)
    public Integer id;
    
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    public Customer customer;
    
    @DatabaseField
    public String productName;
    
    @DatabaseField
    public int quantity;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.id);
        hash = 19 * hash + Objects.hashCode(this.customer);
        hash = 19 * hash + Objects.hashCode(this.productName);
        hash = 19 * hash + this.quantity;
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
        final Order other = (Order) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.customer, other.customer)) {
            return false;
        }
        if (!Objects.equals(this.productName, other.productName)) {
            return false;
        }
        if (this.quantity != other.quantity) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Order{" + "id=" + id + ", customer=" + customer + ", productName=" + productName + ", quantity=" + quantity + '}';
    }

}
