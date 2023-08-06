package org.beetl.sql.wade;

import com.wade.framework.data.IDataMap;

public interface IColumn {
    
    public String getColumnName();
    
    public void setColumnName(String columnName);
    
    public int getColumnType();
    
    public void setColumnType(int columnType);
    
    public String getColumnDesc();
    
    public void setColumnDesc(String columnDesc);
    
    public int getColumnSize();
    
    public void setColumnSize(int columnSize);
    
    public int getDecimalDigits();
    
    public void setDecimalDigits(int decimalDigits);
    
    public boolean isKey();
    
    public void setKey(boolean key);
    
    public boolean isNullable();
    
    public void setNullable(boolean nullable);
    
    public IDataMap toData();
    
}
