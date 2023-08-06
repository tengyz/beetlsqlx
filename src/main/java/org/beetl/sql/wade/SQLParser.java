package org.beetl.sql.wade;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wade.framework.data.IDataMap;

public class SQLParser {
    
    private StringBuffer sqlstr = new StringBuffer();
    
    private IDataMap param;
    
    /**
     * construct function
     * @param param
     * @throws Exception
     */
    public SQLParser(IDataMap param) throws Exception {
        this.param = param;
    }
    
    private List getTokens(String sql) throws Exception {
        class QuoteRange {
            int start;
            
            int length;
            
            String text;
        }
        Pattern patParam = Pattern.compile("(:[\\w]*)");
        Pattern patQuote = Pattern.compile("('[^']*')");
        Matcher matcher;
        List quoteRanges = new ArrayList();
        matcher = patQuote.matcher(sql);
        while (matcher.find()) {
            QuoteRange r = new QuoteRange();
            r.start = matcher.start();
            r.text = matcher.group();
            r.length = r.text.length();
            quoteRanges.add(r);
        }
        matcher = patParam.matcher(sql);
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
            keys.add(key);
        }
        return keys;
    }
    
    /**
     * add sql
     * @param sql
     * @throws Exception
     */
    public void addSQL(String sql) throws Exception {
        List names = getTokens(sql);
        if (names.isEmpty()) {
            sqlstr.append(sql);
        }
        else {
            for (int i = 0; i < names.size(); i++) {
                String name = (String)names.get(i);
                String value = (String)param.get(name);
                if (value == null || "".equals(value.trim()))
                    return;
            }
            sqlstr.append(sql);
        }
    }
    
    /**
     * get sql
     * @return String
     * @throws Exception
     */
    public String getSQL() throws Exception {
        return sqlstr.toString();
    }
    
    /**
     * get param
     * @return IDataMap
     * @throws Exception
     */
    public IDataMap getParam() throws Exception {
        return param;
    }
    
    /**
     * add parser
     * @param parser
     * @throws Exception
     */
    public void addParser(SQLParser parser) throws Exception {
        param.putAll(parser.getParam());
        sqlstr.append(parser.getSQL());
    }
    
}