package org.beetl.sql.wade;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleStatement implements IStatement {
    
    private static Logger log = LogManager.getLogger(SimpleStatement.class);
    
    private Statement statement;
    
    private String sql;
    
    private String[] sqls;
    
    /**
     * construct function
     * @param conn
     * @param sqlstr
     * @throws Exception
     */
    public SimpleStatement(Connection conn, String sqlstr) throws Exception {
        statement = conn.createStatement();
        this.sql = sqlstr;
    }
    
    /**
     * construct function
     * @param conn
     * @param sqlsstr
     * @throws Exception
     */
    public SimpleStatement(Connection conn, String[] sqlsstr) throws Exception {
        statement = conn.createStatement();
        this.sqls = sqlsstr;
    }
    
    /**
     * get sql
     * @return String
     * @throws Exception
     */
    public String getSql() throws Exception {
        return sql;
    }
    
    /**
     * get statement
     * @return Statement
     * @throws Exception
     */
    public Statement getStatement() throws Exception {
        return statement;
    }
    
    /**
     * execute query
     * @return ResultSet
     * @throws Exception
     */
    public ResultSet executeQuery() throws Exception {
        long beginTime = System.currentTimeMillis();
        StringBuilder sqlLog = new StringBuilder();
        sqlLog.append("WADE SQL \n┏━━━━━start━━━\n").append("┣ SQL：\t " + sql).append("\n");
        
        try {
            ResultSet result = statement.executeQuery(sql);
            DaoHelper.getExecuteTimeBySql(log, beginTime, sqlLog);
            return result;
        }
        catch (java.sql.SQLException e) {
            log.error("ERROR SQL: " + sql);
            throw e;
        }
    }
    
    /**
     * execute update
     * @return int
     * @throws Exception
     */
    public int executeUpdate() throws Exception {
        long beginTime = System.currentTimeMillis();
        StringBuilder sqlLog = new StringBuilder();
        sqlLog.append("WADE SQL \n┏━━━━━start━━━\n").append("┣ SQL：\t " + sql).append("\n");
        try {
            int result = statement.executeUpdate(sql);
            DaoHelper.getExecuteTimeBySql(log, beginTime, sqlLog);
            return result;
        }
        catch (java.sql.SQLException e) {
            log.error("ERROR SQL: " + sql);
            throw e;
        }
    }
    
    /**
     * execute batch
     * @return int[]
     * @throws Exception
     */
    public int[] executeBatch() throws Exception {
        long beginTime = System.currentTimeMillis();
        StringBuilder sqlLog = new StringBuilder();
        sqlLog.append("WADE BATCH SQL \n┏━━━━━start━━━\n").append("┣ SQL：\t " + sql).append("\n");
        
        for (int i = 0; i < sqls.length; i++) {
            if (log.isDebugEnabled()) {
                sqlLog.append("┣ Batch SQL=:").append(sqls[i]).append("\n");
            }
            statement.addBatch(sqls[i]);
        }
        
        int[] result = statement.executeBatch();
        DaoHelper.getExecuteTimeBySql(log, beginTime, sqlLog);
        return result;
    }
    
    /**
     * close statement
     * @throws Exception
     */
    public void close() throws Exception {
        statement.close();
    }
    
}