package org.beetl.sql.wade;

import com.wade.framework.data.IDataMap;

public interface ITable {
    
    public String getRemarks();
    
    public void setRemarks(String remarks);
    
    public String getTableCat();
    
    public void setTableCat(String tableCat);
    
    public String getTableName();
    
    public void setTableName(String tableName);
    
    public String getTableSchem();
    
    public void setTableSchem(String tableSchem);
    
    public String getTableType();
    
    public void setTableType(String tableType);
    
    public IDataMap toData();
    
}
