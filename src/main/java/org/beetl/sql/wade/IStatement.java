package org.beetl.sql.wade;

import java.sql.ResultSet;
import java.sql.Statement;

public interface IStatement {
    
    /**
     * get sql
     * @return String
     * @throws Exception
     */
    public String getSql() throws Exception;
    
    /**
     * get statement
     * @return Statement
     * @throws Exception
     */
    public Statement getStatement() throws Exception;
    
    /**
     * execute query
     * @return ResultSet
     * @throws Exception
     */
    public ResultSet executeQuery() throws Exception;
    
    /**
     * execute update
     * @return int
     * @throws Exception
     */
    public int executeUpdate() throws Exception;
    
    /**
     * execute batch
     * @return int[]
     * @throws Exception
     */
    public int[] executeBatch() throws Exception;
    
    /**
     * close statement
     * @throws Exception
     */
    public void close() throws Exception;
    
}