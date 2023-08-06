package org.beetl.sql.wade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.beetl.sql.wade.util.Parameter;

import com.wade.framework.data.IDataList;
import com.wade.framework.data.IDataMap;
import com.wade.framework.data.impl.DataArrayList;
import com.wade.framework.data.impl.DataHashMap;

/**
 * dao工具类
 * @Description dao工具类 
 * @ClassName   DaoHelper 
 * @Date        2018年3月11日 下午5:56:43 
 * @Author      yz.teng
 */
public class DaoHelper {
    
    /**
     * get execute time by sql
     * 
     * @param log
     * @param beginTime
     * @throws Exception
     */
    public static void getExecuteTimeBySql(Logger log, long beginTime) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("SQL execute time: " + ((double)(System.currentTimeMillis() - beginTime) / (double)1000) + "s");
        }
    }
    
    /**
     * get execute time by sql
     * 
     * @param log
     * @param beginTime
     * @throws Exception
     */
    public static void getExecuteTimeBySql(Logger log, long beginTime, StringBuilder sqlLog) throws Exception {
        if (log.isDebugEnabled()) {
            sqlLog.append("┣ 时间：\t ")
                    .append(((double)(System.currentTimeMillis() - beginTime) / (double)1000))
                    .append("s")
                    .append("\n")
                    .append("┗━━━━━end━━━")
                    .append("\n");
            log.debug(sqlLog.toString());
            
        }
    }
    
    /**
     * get count sql
     * 
     * @param sql
     * @return String
     */
    public static String getCountSql1(String sql) throws Exception {
        Pattern pattern = Pattern.compile("(?i)(?u)(union|join|group by)");
        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            pattern = Pattern.compile("(?i)(?u)(select.*?from)");
            matcher = pattern.matcher(sql);
            if (matcher.find()) {
                String subsql = matcher.group();
                Pattern subpattern = Pattern.compile("(?i)(?u)(/\\*.*?\\*/)");
                Matcher submatcher = subpattern.matcher(subsql);
                
                StringBuffer specstr = new StringBuffer();
                while (submatcher.find()) {
                    specstr.append(submatcher.group());
                }
                
                sql = matcher.replaceFirst("select " + specstr + " count(*) from");
                pattern = Pattern.compile("(?i)(?u)(order by.*$)");
                matcher = pattern.matcher(sql);
                return matcher.replaceAll("");
            }
        }
        return "select count(*) from (" + sql + ")";
    }
    
    public static String getCountSql(String s) {
        StringBuffer sbf = new StringBuffer();
        sbf.append("SELECT COUNT(1) FROM (");
        sbf.append(s);
        sbf.append(")");
        String sf = sbf.toString();
        StringBuffer sb = new StringBuffer();
        String[] delims = {" ", "\t", "\n", "\r", "\f", ",", "(+)", "||", "**", "<=", ">=", "!=", "<>", ":=", "<", ">", "=", "+", "-", "*", "/", "(",
                ")"};
        List tokens = tokenize(s, delims);
        String token;
        
        if ((tokens.size() < 4) || (tokens.get(0).toString().compareToIgnoreCase("SELECT") != 0)
                || (tokens.get(1).toString().compareToIgnoreCase("DISTINCT") == 0)) {
            return sf;
        }
        
        int i = 0;
        int len = tokens.size();
        int subLevel = 0;
        int state = 0;
        int idxFrom = -1;
        int idxWhere = -1;
        int idxGroup = -1;
        int idxOrder = -1;
        int idxUnion = -1;
        while (i < len) {
            token = (String)tokens.get(i);
            if (token.equals("(")) {
                subLevel++;
            }
            else if (token.equals(")")) {
                subLevel--;
            }
            else if ((state == 0) && (subLevel == 0) && (token.compareToIgnoreCase("FROM") == 0)) {
                idxFrom = i;
                state = 1;
            }
            else if ((state == 1) && (subLevel == 0) && (token.compareToIgnoreCase("WHERE") == 0)) {
                idxWhere = i;
            }
            else if ((state == 1) && (subLevel == 0) && (token.compareToIgnoreCase("ORDER") == 0)) {
                idxOrder = i;
            }
            else if ((state == 1) && (subLevel == 0) && (token.compareToIgnoreCase("GROUP") == 0)) {
                idxGroup = i;
            }
            else if ((state == 1) && (subLevel == 0) && (token.compareToIgnoreCase("UNION") == 0)) {
                idxUnion = i;
            }
            else if ((state == 0) && token.equals("?")) {
                // like such case:
                // SELECT ? stat_month,user_id,......
                // fallback
                return sf;
            }
            i++;
        }
        if ((idxFrom < 0) || (idxGroup > 0) || (idxUnion > 0)) {
            return sf;
        }
        sb.append("SELECT ");
        for (i = 0; i < idxFrom; i++) {
            token = (String)tokens.get(i);
            if (token.startsWith("/*")) {
                sb.append(" ");
                sb.append(token);
            }
        }
        sb.append(" COUNT(1)");
        for (i = idxFrom; i < len; i++) {
            token = (String)tokens.get(i);
            if (!token.startsWith("("))
                sb.append(" ");
            sb.append(token);
        }
        return sb.toString();
    }
    
    static void checkAddString(List ls, Object s) {
        if (s.toString().trim().length() > 0) {
            ls.add(s.toString().trim());
        }
    }
    
    static List tokenize(String s, String[] delims) {
        List ls = new ArrayList();
        StringBuffer sb = new StringBuffer();
        StringBuffer sbRemark = new StringBuffer();
        StringBuffer sbQuote = new StringBuffer();
        boolean quote = false;
        boolean remark = false;
        int len = s.length();
        int i = 0;
        String delim = "";
        while (i < len) {
            char c = s.charAt(i);
            if (quote) {
                if (c == '\'') {
                    if (i + 1 < len) {
                        char cn = s.charAt(i + 1);
                        if (cn == '\'') {
                            sbQuote.append(c);
                            sbQuote.append(cn);
                            i++;
                        }
                        else {
                            sbQuote.append(c);
                            checkAddString(ls, sbQuote);
                            sbQuote = new StringBuffer();
                            quote = false;
                        }
                    }
                    else {
                        sbQuote.append(c);
                        checkAddString(ls, sbQuote);
                        sbQuote = new StringBuffer();
                        quote = false;
                    }
                }
                else {
                    sbQuote.append(c);
                }
            }
            else {
                if (remark) {
                    if (c == '*') {
                        if (i + 1 < len) {
                            char cn = s.charAt(i + 1);
                            if (cn == '/') {
                                sbRemark.append(c);
                                sbRemark.append(cn);
                                checkAddString(ls, sbRemark);
                                sbRemark = new StringBuffer();
                                remark = false;
                                i++;
                            }
                            else {
                                sbRemark.append(c);
                            }
                        }
                        else {
                            sbRemark.append(c);
                        }
                    }
                    else {
                        sbRemark.append(c);
                    }
                }
                else {
                    if (c == '\'') {
                        checkAddString(ls, sb);
                        sb = new StringBuffer();
                        sbQuote.append(c);
                        quote = true;
                    }
                    else if ((c == '/') && (i + 1 < len) && (s.charAt(i + 1) == '*')) {
                        checkAddString(ls, sb);
                        sb = new StringBuffer();
                        sbRemark.append("/*");
                        remark = true;
                        i++;
                    }
                    else {
                        int sep = 0;
                        for (int j = 0; j < delims.length; j++) {
                            if (s.substring(i).startsWith(delims[j])) {
                                sep = delims[j].length();
                                delim = delims[j];
                                break;
                            }
                        }
                        if (sep > 0) {
                            checkAddString(ls, sb);
                            checkAddString(ls, delim);
                            sb = new StringBuffer();
                            i += sep - 1;
                        }
                        else {
                            sb.append(c);
                        }
                    }
                }
            }
            i++;
        }
        checkAddString(ls, sb);
        if (quote) {
            throw new RuntimeException("quoted string not properly terminated");
        }
        if (remark) {
            throw new RuntimeException("remark not properly terminated");
        }
        return ls;
    }
    
    /**
     * get paging sql
     * 
     * @param sql
     * @param param
     * @param start
     * @param end
     * @return String
     */
    public static String getPagingSql(String sql, Parameter param, int start, int end) throws Exception {
        StringBuffer str = new StringBuffer();
        
        switch (5) {
            case 1:
                str.append("select * from (select row_.*, rownum rownum_ from (");
                str.append(sql);
                str.append(") row_ where rownum <= ?) where rownum_ > ?");
                param.add(String.valueOf(end));
                param.add(String.valueOf(start));
                break;
            case 2:
                str.append(sql);
                str.append(" limit ?, ?");
                param.add(String.valueOf(start));
                param.add(String.valueOf(end));
                break;
        }
        
        return str.toString();
    }
    
    /**
     * get paging sql
     * 
     * @param sql
     * @param param
     * @param start
     * @param end
     * @return String
     * @throws Exception
     */
    public static String getPagingSql(String sql, IDataMap param, int start, int end) throws Exception {
        StringBuffer str = new StringBuffer();
        
        switch (5) {
            case 1:
                str.append("select * from (select row_.*, rownum rownum_ from (");
                str.append(sql);
                str.append(") row_ where rownum <= :MAX_NUM) where rownum_ > :MIN_NUM");
                break;
            case 5:
                str.append(sql);
                str.append(" limit :MIN_NUM, :MAX_NUM");
                break;
        }
        
        param.put("MIN_NUM", String.valueOf(start));
        param.put("MAX_NUM", String.valueOf(end));
        
        return str.toString();
    }
    
    /**
     * get objects by insert
     * 
     * @param conn
     * @param table_name
     * @param data
     * @return Object[]
     * @throws Exception
     */
    public static Object[] getObjectsByInsert(Connection conn, String table_name, IDataMap data) throws Exception {
        StringBuffer namestr = new StringBuffer();
        StringBuffer valuestr = new StringBuffer();
        Parameter param = new Parameter();
        
        IColumn[] columns = getColumns(conn, table_name);
        for (int i = 0; i < columns.length; i++) {
            Object[] colobjs = getObjectsByColumn(columns[i], data);
            
            namestr.append(colobjs[0] + ",");
            valuestr.append("?" + ",");
            param.add(colobjs[1]);
        }
        
        return new Object[] {getInsertSql(table_name, trimSuffix(namestr.toString(), ","), trimSuffix(valuestr.toString(), ",")), param};
    }
    
    /**
     * get objects by column
     * 
     * @param column
     * @param data
     * @return Object[]
     * @throws Exception
     */
    public static Object[] getObjectsByColumn(IColumn column, IDataMap data) throws Exception {
        String columnName = column.getColumnName();
        Object columnValue = data.get(columnName);
        
        if (isDatetimeColumn(column.getColumnType())) {
            if (columnValue != null) {
                if ("".equals(columnValue)) {
                    columnValue = null;
                }
                else {
                    columnValue = encodeTimestamp(columnValue.toString());
                }
            }
        }
        else if (column.getColumnType() == Types.LONGVARCHAR) {
            if (columnValue != null && !"".equals(columnValue)) {
                columnValue = new ColumnObject(columnName, columnValue, Types.LONGVARCHAR);
            }
        }
        
        return new Object[] {columnName, columnValue};
    }
    
    /**
     * is datetime column
     * 
     * @return boolean
     * @throws Exception
     */
    public static boolean isDatetimeColumn(int columnType) throws Exception {
        return columnType == Types.DATE || columnType == Types.TIME || columnType == Types.TIMESTAMP;
    }
    
    /**
     * encode timestamp
     * @param timeStr
     * @return Timestamp
     * @throws Exception
     */
    public static Timestamp encodeTimestamp(String timeStr) throws Exception {
        String format = getTimestampFormat(timeStr);
        return encodeTimestamp(format, timeStr);
    }
    
    /**
     * get timestamp format
     * @param value
     * @return String
     */
    public static String getTimestampFormat(String value) {
        switch (value.length()) {
            case 4:
                return "yyyy";
            case 6:
                return "yyyyMM";
            case 7:
                return "yyyy-MM";
            case 8:
                return "yyyyMMdd";
            case 10:
                return "yyyy-MM-dd";
            case 13:
                return "yyyy-MM-dd HH";
            case 16:
                return "yyyy-MM-dd HH:mm";
            case 19:
                return "yyyy-MM-dd HH:mm:ss";
            case 21:
                return "yyyy-MM-dd HH:mm:ss.S";
        }
        return null;
    }
    
    /**
     * encode timestamp
     * @param format
     * @param timeStr
     * @return Timestamp
     * @throws Exception
     * modified by caom on 08.7.28, check timeStr is null
     */
    public static Timestamp encodeTimestamp(String format, String timeStr) throws Exception {
        if (null == timeStr || "".equals(timeStr))
            return null;
        if (format.length() != timeStr.length())
            format = getTimestampFormat(timeStr);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return new Timestamp(sdf.parse(timeStr).getTime());
    }
    
    /**
     * get insert sql
     * 
     * @param table_name
     * @param namestr
     * @param valuestr
     * @return String
     */
    public static String getInsertSql(String table_name, String namestr, String valuestr) {
        StringBuffer str = new StringBuffer();
        str.append("insert into " + table_name);
        str.append("(" + namestr + ")");
        str.append(" values ");
        str.append("(" + valuestr + ")");
        return str.toString();
    }
    
    /**
     * trim suffix
     * @param str
     * @param suffix
     * @return String
     */
    public static String trimSuffix(String str, String suffix) {
        return str.endsWith(suffix) ? str.substring(0, str.length() - 1) : str;
    }
    
    /**
     * get the columns
     * 
     * @param conn
     * @param table_name
     * @return IColumn[]
     * @throws Exception
     */
    public static IColumn[] getColumns(Connection conn, String table_name) throws Exception {
        IDataMap columns = getColumnsByData(conn, table_name);
        List keys = Arrays.asList(getPrimaryKeys(conn, table_name));
        
        String[] colnames = columns.getNames();
        IColumn[] IColumns = new IColumn[colnames.length];
        for (int i = 0; i < colnames.length; i++) {
            IColumn column = (IColumn)columns.get(colnames[i]);
            column.setKey(keys.contains(column.getColumnName()) ? true : false);
            IColumns[i] = column;
        }
        
        return IColumns;
    }
    
    /**
     * get columns by data
     * 
     * @param conn
     * @param table_name
     * @return IData
     * @throws Exception
     */
    public static IDataMap getColumnsByData(Connection conn, String table_name) throws Exception {
        //        ICache cache = EHCacheManager.getInstance().getCache(ICacheManager.SYS_COLUMNS_CACHE);
        //        if (cache != null) {
        //            ICacheElement element = cache.get(table_name);
        //            if (element != null)
        //                return (IData)element.getValue();
        //        }
        
        IDataMap columns = getColumnsByResult(conn, table_name);
        
        //        if (cache != null) {
        //            cache.put(table_name, columns);
        //        }
        
        return columns;
    }
    
    /**
     * get columns by result
     * 
     * @param conn
     * @param table_name
     * @return IData
     * @throws Exception
     */
    public static IDataMap getColumnsByResult(Connection conn, String table_name) throws Exception {
        IDataMap columns = new DataHashMap();
        PreparedStatement statement = conn.prepareStatement("select * from " + table_name.toUpperCase() + " where 1 = 0");
        ResultSetMetaData metaData = statement.executeQuery().getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            IColumn column = new ColumnInfo();
            column.setColumnName(metaData.getColumnName(i).toUpperCase());
            column.setColumnType(metaData.getColumnType(i));
            column.setColumnDesc(metaData.getColumnLabel(i));
            column.setColumnSize(metaData.getColumnDisplaySize(i));
            column.setDecimalDigits(metaData.getScale(i));
            column.setNullable(metaData.isNullable(i) == ResultSetMetaData.columnNoNulls ? false : true);
            
            columns.put(column.getColumnName(), column);
        }
        statement.close();
        
        return columns;
    }
    
    /**
     * get primary keys
     * 
     * @param conn
     * @param table_name
     * @param columns
     * @return String[]
     * @throws Exception
     */
    public static String[] getPrimaryKeys(Connection conn, String table_name) throws Exception {
        //        ICache cache = EHCacheManager.getInstance().getCache(ICacheManager.SYS_KEYS_CACHE);
        //        if (cache != null) {
        //            ICacheElement element = cache.get(table_name);
        //            if (element != null)
        //                return (String[])element.getValue();
        //        }
        
        IDataMap columns = getColumnsByData(conn, table_name);
        
        List keys = new ArrayList();
        
        ResultSet rs = conn.getMetaData().getPrimaryKeys(null, "%", table_name.toUpperCase());
        while (rs.next()) {
            String column_name = rs.getString("COLUMN_NAME").toUpperCase();
            if (columns.containsKey(column_name) && !keys.contains(column_name))
                keys.add(column_name);
        }
        rs.close();
        
        String[] primaryKeys = (String[])keys.toArray(new String[0]);
        
        //        if (cache != null) {
        //            cache.put(table_name, primaryKeys);
        //        }
        
        return primaryKeys;
    }
    
    /**
     * get objects by insert
     * 
     * @param conn
     * @param table_name
     * @param dataset
     * @return Object[]
     * @throws Exception
     */
    public static Object[] getObjectsByInsert(Connection conn, String table_name, IDataList dataset) throws Exception {
        StringBuffer namestr = new StringBuffer();
        StringBuffer valuestr = new StringBuffer();
        IColumn[] columns = getColumns(conn, table_name);
        
        Parameter[] params = new Parameter[dataset.size()];
        for (int i = 0; i < params.length; i++) {
            IDataMap data = (IDataMap)dataset.get(i);
            params[i] = new Parameter();
            
            for (int j = 0; j < columns.length; j++) {
                Object[] colobjs = getObjectsByColumn(columns[j], data);
                
                if (i == 0) {
                    namestr.append(colobjs[0] + ",");
                    valuestr.append("?" + ",");
                }
                params[i].add(colobjs[1]);
            }
        }
        
        return new Object[] {getInsertSql(table_name, trimSuffix(namestr.toString(), ","), trimSuffix(valuestr.toString(), ",")), params};
    }
    
    /**
     * get objects by update
     * 
     * @param conn
     * @param table_name
     * @param data
     * @param keys
     * @param values
     * @return Object[]
     * @throws Exception
     */
    public static Object[] getObjectsByUpdate(Connection conn, String table_name, IDataMap data, String[] keys, String[] values) throws Exception {
        StringBuffer setstr = new StringBuffer();
        Parameter param = new Parameter();
        
        IColumn[] columns = getColumns(conn, table_name);
        for (int i = 0; i < columns.length; i++) {
            Object[] colobjs = getObjectsByColumn(columns[i], data);
            
            setstr.append(colobjs[0] + " = " + "?" + ",");
            param.add(colobjs[1]);
        }
        
        Object[] keyobjs = getObjectsByKeys(conn, table_name, keys, values == null ? data : getDataByKeys(keys, values));
        
        param.addAll((Parameter)keyobjs[1]);
        
        return new Object[] {getUpdateSql(table_name, trimSuffix(setstr.toString(), ","), (String)keyobjs[0]), param};
    }
    
    /**
     * get update sql
     * 
     * @param table_name
     * @param setstr
     * @param keystr
     * @return String
     */
    public static String getUpdateSql(String table_name, String setstr, String keystr) {
        StringBuffer str = new StringBuffer();
        str.append("update " + table_name + " set ");
        str.append(setstr);
        str.append(" where ");
        str.append(keystr);
        return str.toString();
    }
    
    /**
     * get object by keys
     * 
     * @param conn
     * @param table_name
     * @param keys
     * @param data
     * @return Object[]
     * @throws Exception
     */
    public static Object[] getObjectsByKeys(Connection conn, String table_name, String[] keys, IDataMap data) throws Exception {
        if (keys == null)
            keys = getPrimaryKeys(conn, table_name);
        
        IDataMap columns = getColumnsByData(conn, table_name);
        
        StringBuffer sqlstr = new StringBuffer();
        Parameter param = new Parameter();
        
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            Object value = data.get(key);
            
            IColumn column = (IColumn)columns.get(key.toUpperCase());
            sqlstr.append(" and " + (isDatetimeColumn(column.getColumnType()) ? getTocharSql(key, value.toString()) : key) + " = " + "?");
            param.add(value);
        }
        
        return new Object[] {trimPrefix(sqlstr.toString(), " and "), param};
    }
    
    /**
     * trim prefix
     * @param str
     * @param suffix
     * @return String
     */
    public static String trimPrefix(String str, String prefix) {
        return str.startsWith(prefix) ? str.substring(prefix.length()) : str;
    }
    
    /**
     * get tochar sql
     * 
     * @param value
     * @param format
     * @return String
     * @throws Exception
     */
    public static String getTocharSql(String value, String format) throws Exception {
        String dformat = getDateFormat(format);
        return "to_char(" + value + ", '" + dformat + "')";
    }
    
    /**
     * get date format
     * 
     * @param value
     * @return String
     * @throws Exception
     */
    public static String getDateFormat(String value) throws Exception {
        switch (5) {
            case 1:
                switch (value.length()) {
                    case 4:
                        return "yyyy";
                    case 6:
                        return "yyyymm";
                    case 7:
                        return "yyyy-mm";
                    case 8:
                        return "yyyymmdd";
                    case 10:
                        return "yyyy-mm-dd";
                    case 13:
                        return "yyyy-mm-dd hh24";
                    case 16:
                        return "yyyy-mm-dd hh24:mi";
                    case 19:
                        return "yyyy-mm-dd hh24:mi:ss";
                }
            case 5:
                switch (value.length()) {
                    case 4:
                        return "%Y";
                    case 6:
                        return "%Y%m";
                    case 7:
                        return "%Y-%m";
                    case 8:
                        return "%Y%m%d";
                    case 10:
                        return "%Y-%m-%d";
                    case 13:
                        return "%Y-%m-%d %H";
                    case 16:
                        return "%Y-%m-%d %H:%i";
                    case 19:
                        return "%Y-%m-%d %H:%i:%s";
                }
        }
        return null;
    }
    
    /**
     * get data by keys
     * 
     * @param keys
     * @param value
     * @return IData
     * @throws Exception
     */
    public static IDataMap getDataByKeys(String[] keys, String[] values) throws Exception {
        IDataMap data = new DataHashMap();
        for (int i = 0; i < keys.length; i++) {
            data.put(keys[i], values[i]);
        }
        return data;
    }
    
    /**
     * get objects by delete
     * 
     * @param conn
     * @param table_name
     * @param data
     * @param keys
     * @return Object[]
     * @throws Exception
     */
    public static Object[] getObjectsByDelete(Connection conn, String table_name, IDataMap data, String[] keys) throws Exception {
        Object[] keyobjs = getObjectsByKeys(conn, table_name, keys, data);
        return new Object[] {getDeleteSql(table_name, (String)keyobjs[0]), (Parameter)keyobjs[1]};
    }
    
    /**
     * get objects by delete
     * 
     * @param conn
     * @param table_name
     * @param dataset
     * @param keys
     * @return Object[]
     * @throws Exception
     */
    public static Object[] getObjectsByDelete(Connection conn, String table_name, IDataList dataset, String[] keys) throws Exception {
        StringBuffer keysql = new StringBuffer();
        
        Parameter[] params = new Parameter[dataset.size()];
        for (int i = 0; i < params.length; i++) {
            Object[] keyobjs = getObjectsByKeys(conn, table_name, keys, (IDataMap)dataset.get(i));
            
            if (i == 0)
                keysql.append(keyobjs[0]);
            params[i] = (Parameter)keyobjs[1];
        }
        
        return new Object[] {getDeleteSql(table_name, keysql.toString()), params};
    }
    
    /**
     * get delete sql
     * 
     * @param table_name
     * @param setstr
     * @param keystr
     * @return String
     */
    public static String getDeleteSql(String table_name, String keystr) {
        StringBuffer str = new StringBuffer();
        str.append("delete from " + table_name);
        str.append(" where ");
        str.append(keystr);
        return str.toString();
    }
    
    /**
     * get objects by query
     * 
     * @param conn
     * @param table_name
     * @param data
     * @param keys
     * @return Object[]
     * @throws Exception
     */
    public static Object[] getObjectsByQuery(Connection conn, String table_name, IDataMap data, String[] keys) throws Exception {
        Object[] keyobjs = getObjectsByKeys(conn, table_name, keys, data);
        return new Object[] {getQuerySql(table_name, (String)keyobjs[0]), (Parameter)keyobjs[1]};
    }
    
    /**
     * get query sql
     * 
     * @param table_name
     * @param setstr
     * @param keystr
     * @return String
     */
    public static String getQuerySql(String table_name, String keystr) {
        StringBuffer str = new StringBuffer();
        str.append("select * from " + table_name);
        str.append(" where ");
        str.append(keystr);
        return str.toString();
    }
    
    public static <K, V> Map<K, V> getMap(List<?> list, int index) {
        if ((list != null) && (list.size() > index)) {
            Object obj = list.get(index);
            if ((obj instanceof Map)) {
                return (Map)obj;
            }
        }
        return null;
    }
    
    /**
     * list转IDataset
     * 
     * @param IDataList
     * @return
     */
    public static IDataList trans2IDataset(List<?> list) {
        IDataList ds = new DataArrayList();
        
        for (int i = 0; i < list.size(); i++) {
            Map o = getMap(list, i);
            ds.add(trans2IData(o));
        }
        return ds;
    }
    
    /**
     * data转Map
     * 
     * @param Map
     * @return
     */
    public static Map<String, Object> trans2Map(IDataMap data) {
        Map map = new HashMap();
        for (String name : data.getNames()) {
            Object obj = data.get(name);
            if (obj == null)
                map.put(name, null);
            else if ((obj instanceof String))
                map.put(name, obj);
            else if ((obj instanceof IDataMap))
                map.put(name, trans2Map((IDataMap)obj));
            else if ((obj instanceof Map))
                map.put(name, (Map)obj);
            else if ((obj instanceof IDataList))
                map.put(name, trans2List((IDataList)obj));
            else if ((obj instanceof List))
                map.put(name, (List)obj);
            else {
                map.put(name, obj.toString());
            }
        }
        return map;
    }
    
    /**
     * map转IData
     * 
     * @param IDataMap
     * @return
     */
    public static IDataMap trans2IData(Map<?, ?> map) {
        IDataMap data = new DataHashMap();
        for (Map.Entry entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            
            if (value != null) {
                if ((value instanceof IDataMap))
                    data.put(key, (IDataMap)value);
                else if ((value instanceof IDataList))
                    data.put(key, (IDataList)value);
                else if ((value instanceof Map))
                    data.put(key, trans2IData((Map)value));
                else if ((value instanceof List))
                    data.put(key, trans2IDataset((List)value));
                else
                    data.put(key, value);
            }
        }
        return data;
    }
    
    /**
     * IDataset转List
     * 
     * @param List
     * @return
     */
    public static List<Map<String, Object>> trans2List(IDataList ds) {
        List list = new ArrayList();
        for (int i = 0; i < ds.size(); i++) {
            Map o = trans2Map(ds.getData(i));
            list.add(o);
        }
        return list;
    }
    
}