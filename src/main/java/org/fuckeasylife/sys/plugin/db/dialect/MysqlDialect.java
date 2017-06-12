package org.fuckeasylife.sys.plugin.db.dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.fuckeasylife.sys.plugin.db.Db;
import org.fuckeasylife.util.Const;
import org.fuckeasylife.util.DateUtils;
import org.fuckeasylife.util.PropertiesLoader;
import org.fuckeasylife.util.StrKit;

import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
/**
 * 
 * @author clark
 *
 * 2017年2月21日
 * 
 * MYSQL implementation of Dialect 
 */
public class MysqlDialect implements Dialect {
	
	private static Map<String,String> exclutiveMap = new HashMap<String,String>(){
		{
			put("nuonuo_responsemessage_response", "nuonuo_responsemessage_message");
		}
	};
	
	private Db db = new Db(null);
	
	
	public void checkColumn(String name , String tableName , String value, Connection conn) throws SQLException{
		
		String checkColumnSql = "select (case when exists(SELECT COLUMN_NAME FROM information_schema.`COLUMNS` where TABLE_NAME = '"+tableName+"' and column_name = '"+name+"') then 0 else 1 end ) from dual";
		
		String length = "";
		
		if(value.length() > 128){
			length = " text";
		}else{
			length = " varchar("+ 384 +")";
		}
		
		if(0 == db.isColumnExist(conn, checkColumnSql)){
			return ;
		};
		
		String alterSql = "alter table " + tableName + " add  " + name + length;
		
		db.addColumn(conn, alterSql);
		
	}
	
	public String selectLastPrimaryKey(String tableName) {
		// add 2017/03/17
		String replaceTable = exclutiveMap.get(tableName.toLowerCase());
		if(replaceTable != null){
			tableName = replaceTable;
		}
		return "select id from " + tableName + " order by id desc limit 1";
	}

	
	//-------------------------------------------------------------------------------------------  excel
	/**
	 * create excel table 
	 * @param tableName
	 * @param row
	 * @return
	 * @throws PinyinException
	 */
	public String createTableDialect(String tableName,Row row) throws PinyinException {
		
		Iterator<Cell> it =  row.iterator();
		StringBuilder sb = new StringBuilder();
		sb.append("create table " + tableName + " ( id int primary key auto_increment , ");
		while(it.hasNext()){
			Cell cell = it.next();
			String columnStr = PinyinHelper.convertToPinyinString(StrKit.rmvChineseChr(cell.getStringCellValue().trim()),"_", PinyinFormat.WITHOUT_TONE);
			sb.append(columnStr).append(" varchar("+ 384 +") ,");
		}
		return sb.substring(0, sb.toString().length()-1)+")";
	}
	
	private List<String> tableMeta ; 
	/**
	 * 
	 * @param tableName
	 * @param row
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public String insertTableDialect(String tableName,Row row , Connection conn,Map<String,Object> retMap) throws SQLException{
		Iterator<Cell> it =  row.iterator();
		StringBuilder prefix = new StringBuilder();
		prefix.append("insert into " + tableName + "(");
		if(tableMeta == null){
			tableMeta = db.getTableInfo(conn,tableName);
		}
		Iterator<String> itVal = tableMeta.iterator();
		while(itVal.hasNext()){
			String val = itVal.next();
			if("ID".equals(val.toUpperCase())){
				continue;
			}
			if(itVal.hasNext()){
				prefix.append(val+ ",");
			}else{
				prefix.append(val + ")");
			}
		}
		
		StringBuilder suffix  = new StringBuilder();
		suffix.append("values (");
		@SuppressWarnings("unchecked")
		Map<Integer,Set<Integer>> listMap = (Map<Integer,Set<Integer>>) retMap.get(Const.MAPPED_TYPE);
		if(listMap == null){
			listMap = new HashMap<Integer,Set<Integer>>();
			retMap.put(Const.MAPPED_TYPE,listMap);
		}
		int index = 0;
		while(it.hasNext()){
			Cell cell = it.next();
			int type = cell.getCellType(); 
			switch (type) {
			case Cell.CELL_TYPE_BLANK:
				suffix.append("'',");
				break;
			case Cell.CELL_TYPE_NUMERIC:
				if(HSSFDateUtil.isCellDateFormatted(cell)){
					suffix.append("'" +DateUtils.formatDate(cell.getDateCellValue(),"yyyy-MM-dd") +"'");
				}else{
					suffix.append( "'"+cell.getNumericCellValue()+"'");
				};
				break;
			default:
				String value = cell.getStringCellValue().trim();
				suffix.append( "'"+value+"'");
				if(StrKit.isBlank(value)){
					type = Cell.CELL_TYPE_BLANK;
				}
				break;
			}
			if(it.hasNext()){
				suffix.append(",");
			}else{
				suffix.append(")");
			}
			// stole type 
			Set<Integer> typeSet = listMap.get(index);
			if(typeSet == null){
				typeSet = new HashSet<>();
			}
			typeSet.add(type);
			listMap.put(index, typeSet);
			index++;
		}
		
		return prefix.toString() + suffix.toString();
	}

	@Override
	public String isTableExist(String tableName) {
		return "select count(*) from information_schema.`TABLES` where TABLE_NAME = '" +  tableName +"'";
	}
	
}
