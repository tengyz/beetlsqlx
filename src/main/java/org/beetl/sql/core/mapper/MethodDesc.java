package org.beetl.sql.core.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.beetl.sql.core.BeetlSQLException;
import org.beetl.sql.core.SQLManager;
import org.beetl.sql.core.SQLScript;
import org.beetl.sql.core.annotatoin.Param;
import org.beetl.sql.core.annotatoin.RowSize;
import org.beetl.sql.core.annotatoin.RowStart;
import org.beetl.sql.core.annotatoin.Sql;
import org.beetl.sql.core.annotatoin.SqlStatement;
import org.beetl.sql.core.annotatoin.SqlStatementType;
import org.beetl.sql.core.db.KeyHolder;
import org.beetl.sql.core.engine.PageQuery;

/**
 * dao2 参数
 * 
 * @author xiandafu
 *
 */
public class MethodDesc {
	public Map<String, Integer> parasPos = new HashMap<String, Integer>();
	// 0 insert , 1 insert with key holder, 2 select single ,3 select list 4
	// update 5 batchUpdate 6 page query
	public int type = 0;
	public Method method = null;
	// 如果存在翻页，pagger［0］ ＝offet,pagger[1]= size;
	public int[] paggerPos = null;
	// -1 表示返回一个KeyHolder，否则，使用指定位置的参数
	public int keyHolderPos = -1;
	public int mapRootPos = -1;
	
	public String sqlReady = "";
	
	public Class renturnType = Void.class;

	static Map<CallKey, MethodDesc> cache = new HashMap<CallKey, MethodDesc>();
	
	static class CallKey{
		Method m;
		Class entityClass;
		public CallKey(Method m,Class entityClass){
			this.m = m;
			this.entityClass = entityClass;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((entityClass == null) ? 0 : entityClass.hashCode());
			result = prime * result + ((m == null) ? 0 : m.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			
			CallKey other = (CallKey) obj;
			if(other.entityClass==this.entityClass&&this.m.equals(other.m)){
				return true ;
			}else{
				return false;
			}
			
			
			
		}
		
	}

	public static MethodDesc getMetodDesc(SQLManager sm, Class entityClass, Method m, String sqlId) {
		CallKey callKey = new CallKey(m,entityClass);
		MethodDesc desc = cache.get(callKey);
		if (desc != null)
			return desc;
		desc = sm.getMapperConfig().createMethodDesc();
        desc.doParse(sm, entityClass, m, sqlId);
		cache.put(callKey, desc);
		return desc;
	}
	
	
	
	protected void doParse(SQLManager sm, Class entityClass, Method m, String sqlId){
		
		SqlStatement st = (SqlStatement) m.getAnnotation(SqlStatement.class);
		Sql sql =  (Sql) m.getAnnotation(Sql.class);
		if(sql==null&&st==null){
			// 模板
			parse(sm, entityClass, m, sqlId);
		}else if(sql!=null){
			this.sqlReady = sql.value();
			parseSqlReady(sm, entityClass, sql,m,sqlId);
		}else{
			parse(sm, entityClass, m, sqlId);

		}
		
		
	}
	
	protected void parseSqlReady(SQLManager sm, Class entityClass, Sql sql,Method m,String sqlId) {
		
		Class stRetType = sql.returnType();
		//确定type  2（单选），3（多选），4 更新
		SqlStatementType sqlType = sql.type();
		
		if (sqlType == SqlStatementType.AUTO) {
			type = getTypeBySql(sqlReady);
			if(type==-1){
				throw new BeetlSQLException(BeetlSQLException.UNKNOW_MAPPER_SQL_TYPE, sqlId+" 请指定Sql类型");
			}else if(type==0){
				type = 4;// 认为update
			}
		
		} else if (sqlType == SqlStatementType.SELECT) {
			type = 2;
		} else {
			type = 4;
		}
		
		Class methodRetType = m.getReturnType();
		if (type==2&&List.class.isAssignableFrom(methodRetType)) {
			type = 3;
			stRetType = (Class) ((ParameterizedType) m.getGenericReturnType())
					.getActualTypeArguments()[0];

		}
	
		
		//确定查询返回需要映射类型
		if(type==2||type==3||type==6){
			this.getSelectRenturnType(methodRetType, stRetType, entityClass);
		}
			
		
		
	}
	protected void parse(SQLManager sm, Class entityClass, Method m, String sqlId) {

		SqlStatement st = (SqlStatement) m.getAnnotation(SqlStatement.class);
		String params = null;
		Class stRetType = Void.class;
		// 先初步判断 sql 类型
		type = 0;
		if (st != null) {
			params = st.params();
			SqlStatementType sqlType = st.type();
			if (sqlType == SqlStatementType.AUTO) {
				type = getTypeBySqlId(sm, sqlId);
			} else if (sqlType == SqlStatementType.INSERT) {
				type = 0;
			} else if (sqlType == SqlStatementType.SELECT) {
				type = 2;
			} else {
				type = 4;
			}
			
			Class c = st.returnType();
			
			if(c!=Void.class){
				stRetType = c;
			}
		} else {
			type = getTypeBySqlId(sm, sqlId);

		}
		
		if(params!=null&&params.length()!=0){
			this.parseParams(sqlId, params, m);
		}else{
			this.parseAnnotation(sqlId, m);
		}

		Class methodRetType = m.getReturnType();
		if (type == 0) {
			if (KeyHolder.class.isAssignableFrom(methodRetType)) {
				type = 1;
				keyHolderPos = -1;
			}
			return;
		} else if (type == 2) {
			if (List.class.isAssignableFrom(methodRetType)) {
				type = 3;
				Type type = m.getGenericReturnType();
				if(type instanceof ParameterizedType ){
					stRetType = (Class) ((ParameterizedType) m.getGenericReturnType())
							.getActualTypeArguments()[0];
				}else{
					stRetType = entityClass;
				}
			
			}
		}
		
		//确定查询返回需要映射类型
		if(type==2||type==3||type==6){
			this.getSelectRenturnType(methodRetType, stRetType, entityClass);
		}
		
		
	}

	protected void parseAnnotation(String sqlId, Method m) {
		// 纪录错误位置
		LinkedHashMap<Integer, String> errorPara = new LinkedHashMap<Integer, String>();
		Annotation[][] parameterAnnotations = m.getParameterAnnotations();
		Class[] paraTypes = m.getParameterTypes();
		for (int argIndex = 0; argIndex < parameterAnnotations.length; argIndex++) {
			int length = parameterAnnotations[argIndex].length;
			if (length == 0) {
				Class cls = paraTypes[argIndex];
				if (KeyHolder.class.isAssignableFrom(cls)) {
					if (type == 0) {
						type = 1;
						keyHolderPos = argIndex;

					} else {
						errorPara.put(argIndex, "出现KeyHolder，但操作类型是" + getTypeDesc(type));
					}
					continue;
				}
				
				if(PageQuery.class.isAssignableFrom(cls)){
					type = 6 ;// page query
					break;
				}
				

				if (Map.class.isAssignableFrom(cls)) {
					if (!this.parasPos.containsKey("_root")) {
						mapRootPos = argIndex;

					} else {
						errorPara.put(argIndex, "该参数没有用@Param，但已经有一个Pojo或者Map");
					}
					continue;

				}

				if (List.class.isAssignableFrom(cls)) {
					if (type == 4) {
						type = 5; // batch update

					} else {
						errorPara.put(argIndex, "只有批量更新语句才允许List参数");
					}
					continue;
				}

				if (cls.isArray() && Map.class.isAssignableFrom(cls.getComponentType())) {
					if (type == 4) {
						type = 5; // batch update
					} else {
						errorPara.put(argIndex, "只有批量更新语句才允许Map<String,Object>参数");
					}
					continue;
				}
				
				
				Package pkg = cls.getPackage();
				if (pkg == null) {
					errorPara.put(argIndex, "没有申明params的参数");
					continue;
				}

				String pkgName = pkg.getName();
				if (pkgName.startsWith("java")) {
					errorPara.put(argIndex, "没有申明params的参数");
					continue;
				}

				if (mapRootPos != -1) {
					// 已经有map参数了，不能与pojo并存
					errorPara.put(argIndex, "该参数没有用@Param，但已经有一个Pojo或者Map");
				} else {
					// pojo
					if (this.parasPos.containsKey("_root")) {
						int pos = this.parasPos.get("_root");
						errorPara.put(argIndex, "该参数没有用@Param，但已经有一个Pojo或者Map");
					} else {
						this.parasPos.put("_root", argIndex);

					}
				}

			} else {
				for (int annIndex = 0; annIndex < length; annIndex++) {
					Annotation paramAnn = parameterAnnotations[argIndex][annIndex];
					// Param注解.
					if (paramAnn instanceof Param) {
						Param param = (Param) paramAnn;
						parasPos.put(param.value(), argIndex);

					} else if (paramAnn instanceof RowStart) {
						if (paggerPos == null) {
							paggerPos = new int[2];
						}
						paggerPos[0] = argIndex;
					} else if (paramAnn instanceof RowSize) {
						if (paggerPos == null) {
							paggerPos = new int[2];
						}
						paggerPos[1] = argIndex;
					} else {
						errorPara.put(argIndex, "不能识别的注解" + paramAnn.getClass());
					}
				}
			}

		}

		// 错误检查
		if (errorPara.size() != 0) {
			throw new BeetlSQLException(BeetlSQLException.ERROR_MAPPER_PARAMEER,
					sqlId + "接口参数如下位置" + errorPara + "定义错误，无法映射");

		}
		if (type == 5 && paraTypes.length != 1) {
			throw new BeetlSQLException(BeetlSQLException.ERROR_MAPPER_PARAMEER, sqlId + "批量更新只允许一个List<?> 或者Map[]参数");
		}
		
		if(this.mapRootPos!=-1){
			this.parasPos.put("_root", mapRootPos);
		}
		

	}

	private void parseParams(String sqlId, String params,Method m) {
		Class[] paraTypes = m.getParameterTypes();
		String[] paraNames = params.split(",");
		if (paraTypes.length != paraNames.length) {
			throw new BeetlSQLException(BeetlSQLException.ERROR_MAPPER_PARAMEER, sqlId + "接口参数申明错误，跟@params不一致");

		}
		this.parasPos.clear(); // 配置以SqlStatment为准
		for (int i = 0; i < paraNames.length; i++) {
			String str = paraNames[i].trim();
			if (str.equals("_st")) {
				if (paggerPos == null) {
					paggerPos = new int[2];
				}
				paggerPos[0] = i;
			} else if (str.equals("_sz")) {
				if (paggerPos == null) {
					paggerPos = new int[2];
				}
				paggerPos[1] = i;
			} else  {
				parasPos.put(str, i);
			} 

		}

	}

	private int getTypeBySql(String sql) {
		
		String sqlType = getFirstToken(sql);
		
		if (sqlType.equals("select")) {
			return 2;
		} else if (sqlType.equals("insert")) {
			return 0;
		} else if (sqlType.equals("delete")) {
			return 4;
		} else if (sqlType.equals("update")) {
			return 4;
		} else if(sqlType.equals("create")){
			return 4;
		}else if(sqlType.equals("drop")){
			return 4;
		}
		else {
			return -1; //unknow
		}
	}
	
	private static String getFirstToken(String sql){
		boolean start = false;
		int startIndex = 0;
		for(int i=0;i<sql.length();i++){
			char c = sql.charAt(i);
			if(!start){
				if(!isSpecialChar(c)){
					start = true;
					startIndex = i;
					
				}
				continue;
			}
			
			if(isSpecialChar(c)){
				return sql.substring(startIndex,i).toLowerCase();
			}
			
		}
		return "";
	}
	
	private static boolean isSpecialChar(char c){
		return c==' '||c=='\t'||c=='\r'||c=='\n';
	}
	
	
	protected int getTypeBySqlId(SQLManager sm, String sqlId) {
		String sql = null;
		SQLScript script = sm.getScript(sqlId);
		sql = script.getSql();
		int ret = getTypeBySql(sql);
		if(ret==-1){
			throw new BeetlSQLException(BeetlSQLException.UNKNOW_MAPPER_SQL_TYPE, sqlId+" 请指定Sql类型");
		}else{
			return ret;
		}
		
		
	}

	private String getTypeDesc(int type) {
		switch (type) {
		case 0:
		case 1:
			return "insert";
		case 2:
		case 3:
			return "select";
		case 4:
		case 5:
			return "update/delete";
		default: {
			throw new IllegalArgumentException("unknow type:" + type);
		}
		}
	}
	
	protected void getSelectRenturnType(Class methodRetType,Class annotationType,Class entity){
		if(annotationType!=Void.class){
			//注解总是优先
			this.renturnType = annotationType;
			return ;
		}
		
		if(this.type==3||type==6){
			this.renturnType = entity;
		}else if(this.type==2){
			this.renturnType = methodRetType;
		}
		

	}

}
