package org.beetl.sql.ext;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beetl.sql.core.Interceptor;
import org.beetl.sql.core.InterceptorContext;
import org.beetl.sql.core.engine.SQLParameter;

/** 用来统计sql执行时间
 * @author joelli
 *
 */
public class TimeStatInterceptor implements Interceptor {
    private static Logger log = LogManager.getLogger(TimeStatInterceptor.class);
    
    List<String> excludes = null;
    
    long max;
    
    public TimeStatInterceptor(long max) {
        this(Collections.<String> emptyList(), max);
    }
    
    public TimeStatInterceptor(List<String> excludes, long max) {
        this.excludes = excludes;
    }
    
    @Override
    public void before(InterceptorContext ctx) {
        if (excludes.contains(ctx.getSqlId()))
            return;
        ctx.put("stat.time", System.currentTimeMillis());
        
    }
    
    @Override
    public void after(InterceptorContext ctx) {
        if (excludes.contains(ctx.getSqlId()))
            return;
        long end = System.currentTimeMillis();
        long start = (Long)ctx.get("stat.time");
        if ((end - start) > max) {
            print(ctx.getSqlId(), ctx.getSql(), ctx.getParas(), (end - start));
        }
        
    }
    
    protected void print(String sqlId, String sql, List<SQLParameter> paras, long time) {
        if (log.isDebugEnabled()) {
            log.debug("sqlId=" + sqlId + " time:" + time);
            log.debug("=====================");
            log.debug(sql);
        }
    }
    
    @Override
    public void exception(InterceptorContext ctx, Exception ex) {
        
    }
    
}
