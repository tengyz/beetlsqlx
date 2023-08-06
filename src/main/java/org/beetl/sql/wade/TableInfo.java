package org.beetl.sql.wade;

import com.wade.framework.data.IDataMap;
import com.wade.framework.data.impl.DataHashMap;

public class TableInfo implements ITable {
    
    private String tableCat;
    
    private String tableName;
    
    private String tableType;
    
    private String tableSchem;
    
    private String remarks;
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    
    public String getTableCat() {
        return tableCat;
    }
    
    public void setTableCat(String tableCat) {
        this.tableCat = tableCat;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public String getTableSchem() {
        return tableSchem;
    }
    
    public void setTableSchem(String tableSchem) {
        this.tableSchem = tableSchem;
    }
    
    public String getTableType() {
        return tableType;
    }
    
    public void setTableType(String tableType) {
        this.tableType = tableType;
    }
    
    public IDataMap toData() {
        IDataMap table = new DataHashMap();
        table.put("TABLE_CAT", getTableCat());
        table.put("TABLE_SCHEM", getTableSchem());
        table.put("TABLE_NAME", getTableName());
        table.put("TABLE_TYPE", getTableType());
        table.put("REMARKS", getRemarks());
        return table;
    }
}
