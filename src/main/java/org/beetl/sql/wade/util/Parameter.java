package org.beetl.sql.wade.util;

import java.util.ArrayList;
import java.util.List;

public class Parameter {
    
    private List list = new ArrayList();
    
    /**
     * default construct function
     * @throws Exception
     */
    public Parameter() throws Exception {
        super();
    }
    
    /**
     * construct function
     * @param params
     * @throws Excetion
     */
    public Parameter(Object[] params) throws Exception {
        for (int i = 0; i < params.length; i++)
            list.add(params[i]);
    }
    
    /**
     * get value
     * @param index
     * @return Object
     * @throws Exception
     */
    public Object get(int index) throws Exception {
        return list.get(index);
    }
    
    /**
     * add value
     * @param value
     * @throws Exception
     */
    public void add(Object value) throws Exception {
        list.add(value);
    }
    
    /**
     * add value
     * @param index
     * @param value
     * @throws Exception
     */
    public void add(int index, Object value) throws Exception {
        list.add(index, value);
    }
    
    /**
     * add param
     * @param param
     * @throws Exception
     */
    public void addAll(Parameter param) throws Exception {
        for (int i = 0; i < param.size(); i++)
            list.add(param.get(i));
    }
    
    /**
     * get size
     * @return 
     * @throws Exception
     */
    public int size() throws Exception {
        return list.size();
    }
    
    /**
     * to string
     * @return String
     * @throws Exception
     */
    public String toString() {
        return list.toString();
    }
    
}
