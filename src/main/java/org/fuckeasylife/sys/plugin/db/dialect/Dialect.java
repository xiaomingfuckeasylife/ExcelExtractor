package org.fuckeasylife.sys.plugin.db.dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;

import com.github.stuxuhai.jpinyin.PinyinException;
/**
 * 
 * @author clark
 *
 * 2017年2月21日
 * 
 * dialect interface 
 */
public interface Dialect {
	
	
	/**
	 * is table exist 
	 * @param tableName
	 * @return
	 */
	String isTableExist(String tableName);
	
	/**
	 * select the last primary key 
	 * @param tableName
	 * @return
	 */
	String selectLastPrimaryKey(String tableName);
	
	/**
	 * check column if not exist add to the table 
	 * @param name
	 * @param tableName
	 * @param value
	 * @param conn
	 */
	void checkColumn(String name , String tableName , String value, Connection conn)  throws SQLException;
	
	/**
	 * create excel table 
	 * @param tableName
	 * @param row
	 * @return
	 * @throws PinyinException
	 */
	String createTableDialect(String tableName,Row row) throws PinyinException;
	
	/**
	 * 
	 * @param tableName
	 * @param row
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	String insertTableDialect(String tableName,Row row , Connection conn , Map<String,Object> retMap) throws SQLException;
}
