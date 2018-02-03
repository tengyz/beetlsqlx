package org.beetl.sql.core.kit;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.beetl.core.om.MethodInvoker;
import org.beetl.core.om.ObjectUtil;
import org.beetl.sql.core.annotatoin.Tail;

import com.wade.framework.data.IDataMap;
import com.wade.framework.data.impl.DataHashMap;

 

public class BeanKit {
    private static final Map<Class, Method> tailBeans = new ConcurrentHashMap<Class, Method>();
    
    private static Method NULL = null;
    static {
        try {
            NULL = Object.class.getMethod("toString", new Class[] {});
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static Method getTailMethod(Class type) {
        //如果实现了注解也行@Tail也行
        Method m = tailBeans.get(type);
        if (m != null) {
            if (m == NULL) {
                return null;
            }
            else {
                return m;
            }
            
        }
        else {
            Tail an = getTailAnnotation(type);
            if (an == null) {
                tailBeans.put(type, NULL);
                return null;
            }
            else {
                m = BeanKit.tailMethod(type, an.set());
                if (m == null) {
                    tailBeans.put(type, NULL);
                    return null;
                }
                else {
                    tailBeans.put(type, m);
                    return m;
                }
                
            }
        }
        
    }
    
    private static Tail getTailAnnotation(Class type) {
        if (Object.class.isAssignableFrom(type)) {
            Tail an = (Tail)type.getAnnotation(Tail.class);
            if (an != null) {
                return an;
            }
            else {
                Class parent = type.getSuperclass();
                if (parent == null) {
                    return null;
                }
                return getTailAnnotation(parent);
            }
        }
        else {
            return null;
        }
        
    }
    
    public static PropertyDescriptor[] propertyDescriptors(Class<?> c) throws IntrospectionException {
        
        BeanInfo beanInfo = null;
        beanInfo = Introspector.getBeanInfo(c);
        return beanInfo.getPropertyDescriptors();
        
    }
    
    public static List<Method> getterMethod(Class<?> c) {
        
        PropertyDescriptor[] ps;
        try {
            ps = propertyDescriptors(c);
        }
        catch (IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        List<Method> list = new ArrayList<Method>();
        for (PropertyDescriptor p : ps) {
            if (p.getReadMethod() != null && p.getWriteMethod() != null) {
                list.add(p.getReadMethod());
            }
        }
        return list;
        
    }
    
    public static Method tailMethod(Class type, String name) {
        try {
            Method m = type.getMethod(name, new Class[] {String.class, Object.class});
            return m;
        }
        catch (NoSuchMethodException e) {
            return null;
        }
        catch (SecurityException e) {
            // TODO Auto-generated catch block
            return null;
        }
        
    }
    
    public static Map getMapIns(Class cls) {
        if (cls == Map.class) {
            return new CaseInsensitiveHashMap();
        }
        else {
            try {
                return (Map)cls.newInstance();
            }
            catch (Exception e) {
                return null;
            }
        }
    }
    
    /**
     * 初始化空的IDataMap
     * @param cls
     * @return
     * @Date        2017年6月11日 下午7:25:37 
     * @Author      yz.teng
     */
    public static IDataMap getIDataMapIns(Class cls) {
        if (cls == IDataMap.class) {
            return new DataHashMap();
        }
        else {
            try {
                return (IDataMap)cls.newInstance();
            }
            catch (Exception e) {
                return null;
            }
        }
    }
    
    public static List getListIns(Class list) {
        if (list == List.class) {
            return new ArrayList();
        }
        else {
            try {
                return (List)list.newInstance();
            }
            catch (Exception e) {
                return null;
            }
        }
        
    }
    
    public static Object getBeanProperty(Object o, String attrName) {
        
        try {
            MethodInvoker inv = ObjectUtil.getInvokder(o.getClass(), attrName);
            return inv.get(o);
        }
        catch (Exception ex) {
            throw new RuntimeException("POJO属性访问出错:" + attrName, ex);
        }
    }
    
    public static void setBeanProperty(Object o, Object value, String attrName) {
        
        MethodInvoker inv = ObjectUtil.getInvokder(o.getClass(), attrName);
        inv.set(o, value);
        
    }
    
    public static Object convertValueToRequiredType(Object result, Class<?> requiredType) {
        if (result == null)
            return null;
        Class type = result.getClass();
        if (type == result) {
            //大多数情况，都是这样
            return result;
        }
        if (String.class == requiredType) {
            return result.toString();
        }
        //判断Number对象所表示的类或接口是否与requiredType所表示的类或接口是否相同，或者是否是其超类或者超接口
        else if (Number.class.isAssignableFrom(requiredType)) {
            if (result instanceof Number) {
                return NumberKit.convertNumberToTargetClass(((Number)result), (Class<Number>)requiredType);
            }
            else {
                return NumberKit.parseNumber(result.toString(), (Class<Number>)requiredType);
            }
        }
        else if (requiredType.isPrimitive()) {
            if (result instanceof Number) {
                return NumberKit.convertNumberToTargetClass(((Number)result), requiredType);
            }
        }
        
        throw new IllegalArgumentException("无法转化成期望类型:" + requiredType);
    }
    
    public static Annotation getAnnotation(Class c, Class expect) {
        do {
            Annotation an = c.getAnnotation(expect);
            if (an != null) {
                return an;
            }
            c = c.getSuperclass();
        } while (c != null);
        return null;
        
    }
    
}
