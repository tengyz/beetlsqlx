package org.beetl.sql.wade;

import com.wade.framework.data.IDataMap;
import com.wade.framework.data.impl.DataHashMap;

public class ColumnInfo implements IColumn {
    
    private String columnName;
    
    private int columnType;
    
    private String columnDesc;
    
    private int columnSize;
    
    private int decimalDigits;
    
    private boolean key;
    
    private boolean nullable;
    
    public String getColumnName() {
        return columnName;
    }
    
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    
    public int getColumnType() {
        return columnType;
    }
    
    public void setColumnType(int columnType) {
        this.columnType = columnType;
    }
    
    public String getColumnDesc() {
        return columnDesc;
    }
    
    public void setColumnDesc(String columnDesc) {
        this.columnDesc = columnDesc;
    }
    
    public int getColumnSize() {
        return columnSize;
    }
    
    public void setColumnSize(int columnSize) {
        this.columnSize = columnSize;
    }
    
    public int getDecimalDigits() {
        return decimalDigits;
    }
    
    public void setDecimalDigits(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }
    
    public boolean isKey() {
        return key;
    }
    
    public void setKey(boolean key) {
        this.key = key;
    }
    
    public boolean isNullable() {
        return nullable;
    }
    
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }
    
    public IDataMap toData() {
        IDataMap column = new DataHashMap();
        
        column.put("COLUMN_NAME", columnName);
        column.put("COLUMN_TYPE", String.valueOf(columnType));
        column.put("COLUMN_DESC", columnDesc);
        column.put("COLUMN_SIZE", String.valueOf(columnSize));
        column.put("DECIMAL_DIGITS", String.valueOf(decimalDigits));
        column.put("KEY", String.valueOf(key));
        column.put("NULLABLE", String.valueOf(nullable));
        
        return column;
    }
}