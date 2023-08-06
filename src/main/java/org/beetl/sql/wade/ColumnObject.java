package org.beetl.sql.wade;

public class ColumnObject {
    
    private String name;
    
    private Object value;
    
    private int type;
    
    public ColumnObject(String name, Object value, int type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public Object getValue() {
        return value;
    }
    
    public int getType() {
        return type;
    }
    
}