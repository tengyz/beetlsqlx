package org.beetl.sql.core.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.beetl.sql.core.BeetlSQLException;
import org.beetl.sql.core.ConnectionSource;
import org.beetl.sql.core.SQLManager;
import org.beetl.sql.core.kit.ThreadSafeCaseInsensitiveHashMap;


public class MetadataManager {

	private ConnectionSource ds = null;
	ThreadSafeCaseInsensitiveHashMap map = null;
	TableDesc NOT_EXIST = new TableDesc("$NOT_EXIST","");
	SQLManager sm = null;
	String defaultSchema;
	String defalutCatalog;
	String dbType = null;
	
	public MetadataManager(ConnectionSource ds,SQLManager sm) {
		super();
		this.ds = ds;
		this.sm = sm ;
		this.dbType = sm.getDbStyle().getName();
		//获取数据库shcema
		initDefaultSchema();
	
	}

	public ConnectionSource getDs() {
		return ds;
	}

	public void setDs(ConnectionSource ds) {
		this.ds = ds;
	}

	/***
	 * 表是否在数据库中
	 * 
	 * @param tableName
	 * @return
	 */
	public boolean existTable(String tableName) {
		TableDesc t = getTable(tableName);
		return t!=null;
	}




	public TableDesc getTable(String name){
		TableDesc table =getTableFromMap(name);		
		if(table==null){
			throw new BeetlSQLException(BeetlSQLException.TABLE_NOT_EXIST,"table \""+name+"\" not exist");
		}
		
		if(table.getCols().size()==0){
			table = initTable(table);
		}
		return table;
	}
	
	public Set<String> allTable(){
		if(map==null){
			this.initMetadata();
		}
		return this.map.keySet();
	}
	
	private TableDesc getTableFromMap(String tableName){
		
		if(map==null){
			synchronized(this){
				if(map!=null) return (TableDesc)map.get(tableName);
				this.initMetadata();
			}
		}
		TableDesc desc = (TableDesc) map.get(tableName);
		if(desc==NOT_EXIST){
			return null;
		}else if(desc==null){
			int index = tableName.indexOf(".");
			if(index!=-1){
				//
				String schema = tableName.substring(0, index);
				String table = tableName.substring(index+1);
				return initOtherSchemaTabel(schema,table);
				
			}else{
				return null;
			}
			
		}else{
			return desc;
		}
	}
	
	private  TableDesc  initTable(TableDesc desc){
	
	
		synchronized (desc){
			
			if(desc.getCols().size()!=0){
				return desc ;
			}
			Connection conn=null;
			ResultSet rs = null;
			try {
				String catalog = this.defalutCatalog;
				String schema = this.defaultSchema;
				conn =  ds.getMaster();
				DatabaseMetaData dbmd =  conn.getMetaData();
				rs = dbmd.getPrimaryKeys(catalog,schema, desc.getName());
				
				while (rs.next()) {
					String idName=rs.getString("COLUMN_NAME");
					desc.addIdName(idName);
				}
				rs.close();
				
				
				rs = dbmd.getColumns(catalog,schema, desc.getName(), "%");
				while(rs.next()){
					String colName = rs.getString("COLUMN_NAME");
					Integer sqlType = rs.getInt("DATA_TYPE");
					Integer size = rs.getInt("COLUMN_SIZE");
					Object o = rs.getObject("DECIMAL_DIGITS");
					Integer digit = null;
					if(o!=null){
						digit = ((Number)o).intValue();
					}
					
					String remark = rs.getString("REMARKS");
					ColDesc col = new ColDesc(colName,sqlType,size,digit,remark);
					desc.addCols(col);
				}
				rs.close();
				return desc;
				
			} catch (SQLException e) {
				throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
			}finally{
				close(conn);
			}
		}
		
		
	}
	
	private synchronized void initMetadata(){
		if(map!=null) return ;
		ThreadSafeCaseInsensitiveHashMap tempMap = new ThreadSafeCaseInsensitiveHashMap();
		Connection conn=null;
		try {
			conn =  ds.getMaster();
			DatabaseMetaData dbmd =  conn.getMetaData();
			
			String catalog = this.defalutCatalog;
			String schema = this.defaultSchema;
			
			String namePattern = this.getTableNamePattern(dbmd);
			ResultSet rs = dbmd.getTables(catalog,schema, namePattern,
					new String[] { "TABLE","VIEW" });
			while(rs.next()){
				String  name = rs.getString("TABLE_NAME");
				String remarks = rs.getString("REMARKS");
				TableDesc desc = new TableDesc(name,remarks);
				desc.setSchema(this.defaultSchema);
				tempMap.put(desc.getName(),desc);
			}
		
			rs.close();
			this.map = tempMap;
		} catch (SQLException e) {
			throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
		}finally{
			close(conn);
		}
	}
	
	private TableDesc initOtherSchemaTabel(String sc,String table){
		Connection conn=null;
		try {
			conn =  ds.getMaster();
			DatabaseMetaData dbmd =  conn.getMetaData();
			String catalog = this.defalutCatalog;
			String schema = this.defaultSchema;
			
			ResultSet rs = null; rs = dbmd.getTables(catalog,schema, getDbTableName(table),
						new String[] { "TABLE","VIEW" });
		
			TableDesc desc  = null;
			while(rs.next()){
				String  name = rs.getString("TABLE_NAME");
				String remarks = rs.getString("REMARKS");
				desc = new TableDesc(name,remarks);
				desc.setSchema(sc);
				map.put(sc+"."+table,desc);
			}
			rs.close();
			if(desc!=null){
				return desc ;
			}else{
				map.put(schema+"."+table,NOT_EXIST);
				return null;
			}
			
			
		} catch (SQLException e) {
			throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
		}finally{
			close(conn);
		}
	}
	
	private void close(Connection conn){
		try{
			if(!ds.isTransaction()){
				if(conn!=null)conn.close();
			}
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	
	private void initDefaultSchema(){
		this.defaultSchema = sm.getDefaultSchema();
		if(defaultSchema==null){
			Connection conn = ds.getMaster();
			
			try {
				setDefaultSchema(conn);
				conn.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		
	}
	private void setDefaultSchema(Connection conn) throws SQLException{
		
		this.defalutCatalog = conn.getCatalog();
		try{
			
			this.defaultSchema =  conn.getSchema();
			
		}catch(Throwable e){
			// jdbc低版本不支持
			String dbName = sm.getDbStyle().getName();
			if(dbName.equals("postgres")){
				defaultSchema = "public";
			}else if(dbName.equals("sqlserver")){
				defaultSchema="dbo";
			}else if(dbName.equals("oracle")){
				defaultSchema = conn.getMetaData().getUserName();
			}else{
				defaultSchema = null;
			}
			
		}
		
	}
	/**
	 * 
	 * 按照我理解，对于访问表xx.yyy, 不同数据库有不同的catalog和schema
	 */
	
	/**
	 * 
	 * @param namespace
	 * @return
	 */
	private String getDbSchema(DatabaseMetaData dbmd,String namespace){
		if(dbType.equals("mysql")){
			return null;
		}else if(dbType.equals("oracle")){
			return namespace.toUpperCase();
		}else{
			return namespace;
		}
	}
	private String getTableNamePattern(DatabaseMetaData meta) throws SQLException{
		//mysql 6 是个在开发版本，有问题，不支持	
		String p=meta.getDatabaseProductName();
		
		if(p.equalsIgnoreCase("mysql")){
			int c = meta.getDriverMajorVersion();
			if(c==6){
				return "%";
			}
		}
	
		return null;
	}
	
//	private String getDbCatalog(DatabaseMetaData meta,String schema) throws SQLException {
//		if(dbType.equals("mysql")){
//			String p=meta.getDatabaseProductName();
//			if(p.equalsIgnoreCase("mysql")){
//				int c = meta.getDriverMajorVersion();
//				if(c==6){
//					return schema;
//				}
//			}
//			return schema;
//		}else{
//			return null;
//		}
//	}
	
	private String getDbTableName(String name){
		if(dbType.equals("oracle")){
			return name.toUpperCase();
		}else{
			return name;
		}
	}
}
