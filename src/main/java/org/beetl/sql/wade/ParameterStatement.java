package org.beetl.sql.wade;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beetl.sql.wade.util.Parameter;

public class ParameterStatement implements IStatement {
    
    private static Logger log = LogManager.getLogger(ParameterStatement.class);
    
    private PreparedStatement statement;
    
    private String sql;
    
    private Parameter param;
    
    private Parameter[] params;
    
    /**
     * construct function
     * @param conn
     * @param sqlstr
     * @throws Exception
     */
    public ParameterStatement(Connection conn, String sqlstr, Parameter param) throws Exception {
        statement = conn.prepareStatement(sqlstr);
        this.sql = sqlstr;
        this.param = param;
    }
    
    /**
     * construct function
     * @param conn
     * @param sql
     * @param params
     * @throws Exception
     */
    public ParameterStatement(Connection conn, String sqlstr, Parameter[] params) throws Exception {
        statement = conn.prepareStatement(sqlstr);
        this.sql = sqlstr;
        this.params = params;
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
        sqlLog.append("WADE SQL \n┏━━━━━start━━━\n").append("┣ SQL：\t " + sql).append("\n").append("┣ 参数：\t " + param).append("\n");
        
        setParameters(statement, param);
        try {
            ResultSet result = statement.executeQuery();
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
        sqlLog.append("WADE SQL \n┏━━━━━start━━━\n").append("┣ SQL：\t " + sql).append("\n").append("┣ 参数：\t " + param).append("\n");
        
        setParameters(statement, param);
        try {
            int result = statement.executeUpdate();
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
        sqlLog.append("WADE BATCH SQL \n┏━━━━━start━━━\n").append("┣ SQL：\t " + sql).append("\n").append("┣ 参数：\t " + param).append("\n");
        
        if (params.length == 0) {
            return new int[] {};
        }
        
        for (int i = 0; i < params.length; i++) {
            setParameters(statement, params[i]);
            if (log.isDebugEnabled()) {
                sqlLog.append("┣ Batch PARAM=:").append(params[i]).append("\n");
            }
            statement.addBatch();
        }
        
        try {
            int[] result = statement.executeBatch();
            DaoHelper.getExecuteTimeBySql(log, beginTime, sqlLog);
            return result;
        }
        catch (java.sql.SQLException e) {
            log.error("ERROR Batch SQL: " + sql);
            throw e;
        }
    }
    
    /**
     * close statement
     * @throws Exception
     */
    public void close() throws Exception {
        statement.close();
    }
    
    /**
     * set parameters
     * @param statement
     * @param param
     * @throws Exception
     */
    public void setParameters(PreparedStatement statment, Parameter param) throws Exception {
        for (int i = 0; i < param.size(); i++) {
            if (param.get(i) == null) {
                statement.setNull(i + 1, Types.VARCHAR);
            }
            else {
                Object value = param.get(i);
                if (value instanceof ColumnObject) {
                    ColumnObject columnObject = (ColumnObject)value;
                    if (columnObject.getType() == Types.LONGVARCHAR) {
                        statement.setCharacterStream(i + 1,
                                new StringReader(columnObject.getValue().toString()),
                                columnObject.getValue().toString().length());
                    }
                }
                else {
                    statement.setObject(i + 1, value);
                }
            }
        }
    }
    
}