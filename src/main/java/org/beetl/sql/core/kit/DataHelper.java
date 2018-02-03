package org.beetl.sql.core.kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wade.framework.data.IDataList;
import com.wade.framework.data.IDataMap;
import com.wade.framework.data.impl.DataArrayList;
import com.wade.framework.data.impl.DataHashMap;

 

/**
 * 数据类型转换工具
 * @Description 数据类型转换工具 
 * @ClassName   DataHelper 
 * @Date        2017年5月27日 下午5:19:52 
 * @Author      yz.teng
 */
public class DataHelper {
    
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
