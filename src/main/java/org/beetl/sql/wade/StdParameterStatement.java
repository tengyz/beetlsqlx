package org.beetl.sql.wade;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.wade.framework.data.IDataList;
import com.wade.framework.data.IDataMap;

public class StdParameterStatement implements IStatement {
    
    private static Logger log = LogManager.getLogger(StdParameterStatement.class);
    
    private PreparedStatement statement;
    
    private String sql;
    
    private IDataMap param;
    
    private IDataList params;
    
    private String[] names;
    
    /**
     * construct function
     * @param conn
     * @param sqlstr
     * @throws Exception
     */
    public StdParameterStatement(Connection conn, String sqlstr, IDataMap param) throws Exception {
        preprocStatement(sqlstr);
        statement = conn.prepareStatement(sql);
        this.param = param;
    }
    
    /**
     * construct function
     * @param conn
     * @param sqlstr
     * @param params
     * @throws Exception
     */
    public StdParameterStatement(Connection conn, String sqlstr, IDataList params) throws Exception {
        preprocStatement(sqlstr);
        statement = conn.prepareStatement(sql);
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
        
        setParameters(statement, names, param, sqlLog);
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
        
        setParameters(statement, names, param, sqlLog);
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
        
        if (params.size() == 0) {
            return new int[] {};
        }
        
        for (int i = 0; i < params.size(); i++) {
            setParameters(statement, names, (IDataMap)params.get(i), sqlLog);
            if (log.isDebugEnabled()) {
                sqlLog.append("┣ Batch PARAM=:").append(params.get(i)).append("\n");
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
    public void setParameters(PreparedStatement statment, String[] names, IDataMap param, StringBuilder sqlLog) throws Exception {
        for (int i = 0; i < names.length; i++) {
            Object value = param.get(names[i]);
            if (value == null) {
                statement.setNull(i + 1, Types.VARCHAR);
                if (log.isDebugEnabled()) {
                    sqlLog.append("┣ [" + (i + 1) + "]BINDING").append("[" + names[i] + "] null").append("\n");
                }
            }
            else {
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
                if (log.isDebugEnabled()) {
                    sqlLog.append("┣ [" + (i + 1) + "]BINDING").append("[" + names[i] + "] [" + value + "]").append("\n");
                }
            }
        }
    }
    
    /**
     * preproc statement
     * @param sqlstr
     * @return String
     * @throws Exception
     */
    private void preprocStatement(String sqlstr) throws Exception {
        class QuoteRange {
            int start;
            
            int length;
            
            String text;
        }
        Pattern patParam = Pattern.compile("(:[a-zA-Z_0-9\\$]*)");
        Pattern patQuote = Pattern.compile("('[^']*')");
        Matcher matcher;
        List quoteRanges = new ArrayList();
        matcher = patQuote.matcher(sqlstr);
        while (matcher.find()) {
            QuoteRange r = new QuoteRange();
            r.start = matcher.start();
            r.text = matcher.group();
            r.length = r.text.length();
            quoteRanges.add(r);
        }
        matcher = patParam.matcher(sqlstr);
        List keys = new ArrayList();
        while (matcher.find()) {
            String key = matcher.group().substring(1);
            if (!quoteRanges.isEmpty()) {
                boolean skip = false;
                int pos = matcher.start();
                Iterator it = quoteRanges.iterator();
                while (it.hasNext()) {
                    QuoteRange r = (QuoteRange)it.next();
                    if (pos >= r.start && pos < r.start + r.length) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }
            }
            sqlstr = sqlstr.replaceFirst(":" + key.replaceAll("\\Q$\\E", "\\\\\\$"), "?");
            keys.add(key);
        }
        this.sql = sqlstr;
        this.names = (String[])keys.toArray(new String[0]);
    }
    
}