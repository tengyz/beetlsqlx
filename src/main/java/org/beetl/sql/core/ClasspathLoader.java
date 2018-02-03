
package org.beetl.sql.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.UnexpectedException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.beetl.sql.core.db.DB2SqlStyle;
import org.beetl.sql.core.db.DBStyle;
import org.beetl.sql.core.db.MySqlStyle;
import org.beetl.sql.core.kit.MDParser;

/**
 * 从classpath系统加载sql模板，id应该格式是"xx.yyy",xx代表了文件名，yyy代表了sql标识 sql 模板格式如下：
 *
 * <pre>
 selectUser 
 ===

 select * from user 
</pre>
 * 
 * 
 * @author Administrator
 *
 */
public class ClasspathLoader implements SQLLoader {

	protected String sqlRoot = null;

	protected String lineSeparator = System.getProperty("line.separator", "\n");

	protected  Map<String, SQLSource> sqlSourceMap = new ConcurrentHashMap<String, SQLSource>();
	protected  Map<String, Integer> sqlSourceVersion = new ConcurrentHashMap<String, Integer> ();

	protected DBStyle dbs = null;
	
	
	protected boolean autoCheck = true;
	
	protected String charset = null;
	
	protected SQLIdNameConversion sqlIdNameConversion = new DefaultSQLIdNameConversion();

	public  ClasspathLoader() {
		this("/sql");
	}
	public  ClasspathLoader(String root) {
		this(root ,new MySqlStyle());
	}
	public  ClasspathLoader(String root,DBStyle dbs) {
		this.sqlRoot = root;
		this.dbs = dbs;
	}

	@Override
	public SQLSource getSQL(String id) {
		SQLSource ss = this.tryLoadSQL(id);
		
		if(ss==null){
			throw new BeetlSQLException(BeetlSQLException.CANNOT_GET_SQL,"未能找到"+id+"对应的sql");
		}
		return ss;
	}
	
	private SQLSource tryLoadSQL(String id){
		SQLSource ss = sqlSourceMap.get(id);	
		boolean hasLoad = false ;
		if (ss == null) {
			loadSql(id);
			hasLoad = true;
		}
		if(!hasLoad&&this.autoCheck&&isModified(id)){
			loadSql(id);
		}
		
		//处理完后再次获取
		ss = sqlSourceMap.get(id);
		return ss;
	}
	
	@Override
	public boolean isModified(String id) {
		int index = id.indexOf('.');
		if(index!=-1){
			String sqlName = id.substring(index);
			if(sqlName.startsWith("._gen")){
				return false;
			}
		}
        //如果db目录中有sql文件，直接使用db目录的文件判断版本（root中的文件会被db中的覆盖）
		InputStream is = this.getDBRootFile(id);
        if(is == null){
            //db目录中没有文件，使用root下的
            is = this.getRootFile(id);
        }

		if(is != null){
			Integer lastModify = is.hashCode();
			Integer oldVersion = sqlSourceVersion.get(id);
			if(oldVersion != null && oldVersion.equals(lastModify)){
				return false;
			}
		}
		
		return true;
	}
	
	public boolean exist(String id){
		return this.tryLoadSQL(id)!=null;
	}
	
	@Override
	public void addGenSQL(String id, SQLSource source) {		
	
		sqlSourceVersion.put(id, 0); //never change
		sqlSourceMap.put(id, source);
		
	}

	/***
	 *  考虑到跨数据库支持，ClasspathLoader加载SQL顺序如下：
		首先根据DBStyle.getName() 找到对应的数据库名称，然后在ROOT/dbName 下找对应的sql，
		如果ROOT/dbName 文件目录不存在，或者相应的sql文件不存在，再搜索ROOT目录下的sql文件。
		如mysql 里查找user.select2,顺序如下：
		- 先找ROOT/mysql/user.sql 文件，如果有此文件，且包含了select2，则返回此sql语句，
        - 如果没有，下一步查找ROOT/mysql/user.md,如果有此文件，且包含了slect2，则返回sql语句
        - 如果没有，下一步查找ROOT/user.sql,如果有此文件，且包含了slect2，则返回sql语句
		- 如果没有，下一步查找ROOT/user.md,如果有此文件，且包含了slect2，则返回sql语句
		- 都没有，抛错，告诉用户未在ROOT/,或者ROOT/mysql 下找到相关sql
	 * 
	 * @return
	 */
	private boolean loadSql(String id) {
        //读取root目录下的sql文件
		InputStream ins = this.getRootFile(id);
        boolean rootResult = readSqlFile(id,ins);
        //读取db目录下的sql文件，进行覆盖
        ins = this.getDBRootFile(id);
        boolean dbResult = readSqlFile(id,ins);
        if(rootResult || dbResult){
            return true;
        }else {
            String modelName = id.substring(0, id.lastIndexOf(".") );
            String path  = modelName.replace('.', '/');
            String filePath0 = sqlRoot + "/" + dbs.getName() + "/" + path + ".sql";
            String filePath1 = sqlRoot + "/" + dbs.getName() + "/" + path + ".md";
            String filePath2 = sqlRoot + "/" + path + ".sql";
            String filePath3 = sqlRoot + "/" + path + ".md";
            throw new BeetlSQLException(BeetlSQLException.CANNOT_GET_SQL, "在 "
                    + filePath0+" 和 "
                    + filePath1+" 和 "
                    + filePath2+" 和 "
                    + filePath3+" 和 "+
                    " 未找到[id="+id+"]相关的SQL");
        }
	}

    private boolean readSqlFile(String id,InputStream ins) {
        String modelName = id.substring(0, id.lastIndexOf(".") + 1);
        if(ins == null) return false ;

        Integer lastModified = ins.hashCode();
        sqlSourceVersion.put(id, lastModified);
        LinkedList<String> list = new LinkedList<String>();
        BufferedReader bf = null;
        try {
       
            bf = new BufferedReader(new InputStreamReader(ins,charset));
            MDParser parser = new MDParser(modelName,bf);
            SQLSource source = null;
	    		while((source=parser.next())!=null){
	    			 sqlSourceMap.put(source.getId(), source);
	    		}
          
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bf != null) {
                try {
                    bf.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
    
    
    private void buildSql(LinkedList<String> list,StringBuilder sql){
    	 while (!list.isEmpty()) {
    		 String s = list.pollFirst();
    		 if(s.startsWith("```")||s.startsWith("~~~")){
    			 //since 2.7.10,认为是beetlsql的块状符号
    			 continue ;
    		 }
             sql.append(s+lineSeparator);
         }
    }

	public Map<String, SQLSource> getSqlSourceMap() {
		return sqlSourceMap;
	}

	public String getSqlRoot() {
		return sqlRoot;
	}
	
	public void setSqlRoot(String sqlRoot) {
		this.sqlRoot = sqlRoot;
	}
	
	/***
	 * 获取.md文件
	 * md文件需放在classpath下
	 * @param id
	 * @return
	 * @throws UnexpectedException 
	 */
	private InputStream getRootFile(String id){
		String path = this.sqlIdNameConversion.getPath(id);
        String filePath0 = sqlRoot + "/" + path + ".sql";
		String filePath1 = sqlRoot + "/" + path + ".md";

		InputStream is = this.getFile(filePath0, id);
        if(is==null){
			is = this.getFile(filePath1, id);
            if(is==null){
                return null;
            }
        }
		return is;
	}

    private InputStream getDBRootFile(String id){
    	String path = this.sqlIdNameConversion.getPath(id);
        String filePath0 = sqlRoot + "/" + dbs.getName() + "/" + path + ".sql";
		String filePath1 = sqlRoot + "/" + dbs.getName() + "/" + path + ".md";
        InputStream is = this.getFile(filePath0, id);
        if(is==null){
            is = this.getFile(filePath1, id);
            if(is==null){
                return null;
            }
        }
        return is;
    }

	private InputStream getFile(String filePath, String id){
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream is  = null;
		if(loader!=null){
			is = loader.getResourceAsStream(filePath);
			if(is!=null){
				return is;
			}else{
				is = this.getClass().getResourceAsStream(filePath);
				return is;
			}
		}else{
			is = this.getClass().getResourceAsStream(filePath);
			return is;
		}
		
		
	}
	
	
	
	@Override
	public boolean isAutoCheck() {
		return this.autoCheck;
	}
	
	@Override
	public void setAutoCheck(boolean check) {
		this.autoCheck = check;
		
	}
	
	@Override
	public SQLSource getGenSQL(String id) {
		return sqlSourceMap.get(id);
	}
	public DBStyle getDbs() {
		return dbs;
	}
	public void setDbs(DBStyle dbs) {
		this.dbs = dbs;
	}
	@Override
	public String getCharset() {
		
		return charset;
	}
	@Override
	public void setCharset(String charset) {
		this.charset = charset;
		
	}
	
	public String toString(){
		return this.sqlRoot;
	}
	@Override
	public void setSQLIdNameConversion(SQLIdNameConversion sqlIdNc) {
		this.sqlIdNameConversion = sqlIdNc;
		
	}
	@Override
	public void setDbStyle(DBStyle dbStyle){
		this.dbs = dbStyle;
	}
	
}

