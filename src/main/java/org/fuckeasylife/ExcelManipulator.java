package org.fuckeasylife;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.fuckeasylife.sys.plugin.db.Config;
import org.fuckeasylife.sys.plugin.db.Db;
import org.fuckeasylife.sys.plugin.db.dialect.MysqlDialect;
import org.fuckeasylife.util.Const;
import org.fuckeasylife.util.StrKit;

import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
/**
 * 
 * @author clark
 *
 * @2017年6月7日上午9:12:16
 * 
 * <p>
 *     all in one . one in all . support Row Recognition .  auto table insert . column adding . 
 * the ultimate excel handle. 
 * </p>
 */
public class ExcelManipulator {

	/**
	 * process excel 
	 * @param stream
	 * @param params tableName 
	 * @return
	 */
	public boolean process(InputStream stream, String ... params){
		try {
			if(conn == null){
				init();
			}
			Workbook wb = WorkbookFactory.create(stream);
			int numberSheets = wb.getNumberOfSheets();
			for(int i = 0;i<numberSheets ; i++){
				Sheet sheet = wb.getSheetAt(i);
				doAlgorithm(sheet,params[0],true);
			}
			
			for(int i = 0;i<numberSheets ; i++){
				Sheet sheet = wb.getSheetAt(i);
				doAlgorithm(sheet,params[0],false);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}finally{
			destroy();
		}
		
		return true;
	}
	
	/**
	 * using algorithm
	 * @param sheet
	 * @param tableName
	 * @throws PinyinException 
	 */
	private void doAlgorithm(Sheet sheet , String tableName,boolean ddl) throws PinyinException{
		
		Integer headRow[] = searchHead(sheet , 20);
		
		assamble(sheet,headRow , tableName,ddl);
		
	}
	
	/**
	 * DB configuration
	 */
	private Config config = new Config(null);
	
	/**
	 * interact with database
	 */
	private Db db = new Db(config);
	
	private Connection conn = null ;
	
	private void init() throws SQLException{
		
		conn = config.getConnection();
		
	}
	
	private void destroy(){
		try {
			if(conn.getAutoCommit() == false){
				conn.commit();
			}
			conn.setAutoCommit(true);
			config.close(conn);
		} catch (SQLException e) {
		}
		
	}
	/**
	 * mysql dialect 
	 */
	private MysqlDialect dialect = new MysqlDialect();
	
	/**
	 * assamble data 
	 * @param sheet
	 * @param headRow
	 * @param tableName
	 * @throws PinyinException 
	 */
	private void assamble(Sheet sheet , Integer headRow[] , String tableName , boolean ddl) throws PinyinException{
	
		try {
			if(ddl){
				ddl( conn , sheet , headRow[0] , tableName);
			}
			if(ddl == false){
				conn.setAutoCommit(false);
				dml( conn , sheet, headRow,tableName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if(ddl == false){
					conn.rollback();
				}
			} catch (SQLException e1) {
			}
			throw new RuntimeException("Excel assamble error : " + e.getMessage());
		}
	}
	
	/**
	 * ddl processing 
	 * @param conn
	 * @param sheet
	 * @param headRow
	 * @param tableName
	 * @throws SQLException
	 * @throws PinyinException 
	 */
	private void ddl(Connection conn , Sheet sheet , Integer headRow , String tableName) throws SQLException, PinyinException{
		Row row = sheet.getRow(headRow);
		if(db.isTableExist(conn, dialect.isTableExist(tableName))){
			checkColumn(conn,tableName,sheet,headRow);
		}else{
			db.createTable(conn, dialect.createTableDialect(tableName, row));
		};
	}
	
	
	private void dml(Connection conn , Sheet sheet , Integer headRow[], String tableName) throws SQLException{
		int last = sheet.getLastRowNum();
		Map<String,Object> map = new HashMap<String,Object>();
		for(int i=headRow[0]+1 ; i<=last ; i++ ){
			Row row = sheet.getRow(i);
			if(!rowRecognition(row,headRow[1],map)){
				continue;
			}
			db.insertTableRecord(conn, dialect.insertTableDialect(tableName, row, conn,map));
		}
	}
	
	@SuppressWarnings("unchecked")
	private boolean rowRecognition(Row row , int length , Map<String,Object> rcgMap){
		boolean recognized = true;
		
		if(row == null || row.getPhysicalNumberOfCells() != length){
			return false;
		};
		
		if(StrKit.isBlank(rcgMap.get(Const.MAPPED_TYPE))){
			return recognized;
		}
		
		Iterator<Cell> it = row.iterator();
		int type = -1;
		int unMatchedCount = 0;
		int index = 0;
		while(it.hasNext()){
			Cell cell = it.next();
			type = cell.getCellType();
			if(!((Map<Integer,Set<Integer>>)rcgMap.get(Const.MAPPED_TYPE)).get(index).contains(type)){
				unMatchedCount++;
			}
			index++;
		}
		if(unMatchedCount / Double.valueOf(length) > 1/2.0){
			recognized = false;
		}
		return recognized;
	}
	
	private void checkColumn(Connection conn,String tableName,Sheet sheet , Integer headRow) throws PinyinException, SQLException{
		Row row = sheet.getRow(headRow);
		Iterator<Cell> rowIt = row.iterator();
		while(rowIt.hasNext()){
			Cell cell = rowIt.next();
			String columnName =  PinyinHelper.convertToPinyinString(StrKit.rmvChineseChr((cell.getStringCellValue().trim())),"_", PinyinFormat.WITHOUT_TONE); 
			dialect.checkColumn(columnName, tableName, "", conn);
		}
	}
	
	/**
	 * 
	 * @param sheet
	 * @param searchNum the number of rows that will be searched for heads
	 * @return the head row number 
	 */
	private Integer[] searchHead(Sheet sheet , int searchNum){
		Integer[] ret = new Integer[2];
		List<Integer> headList = new ArrayList<Integer>();
		Iterator<Row> itRow = sheet.rowIterator();
		while(itRow.hasNext()){
			Row row = itRow.next();
			int rowNum = row.getRowNum() ;
			if(rowNum >= searchNum){
				break;
			}; 
			Iterator<Cell> itCell = row.cellIterator();
			int type = -1;
			boolean isHead = true;
			int columnLength = 0;
			while(itCell.hasNext()){
				columnLength++;
				Cell cell = itCell.next();
				if(Cell.CELL_TYPE_BLANK == cell.getCellType()){
					isHead =false;
					break;
				}
				if(type == -1){
					type = cell.getCellType();
					continue;
				}
				if(Cell.CELL_TYPE_STRING == cell.getCellType()){
					String val = cell.getStringCellValue();
					if(StrKit.isBlank(val)){
						type = Cell.CELL_TYPE_BLANK;
					}
				}
				if( type != cell.getCellType()){
					isHead = false;
					break;
				}
			}
			
			if(isHead){
				if(contains(headList,columnLength)){
					continue;
				}else{
					headList.add(columnLength);
					ret[0] = rowNum;
					ret[1] = columnLength;
				}
			}
		}
		
		return ret;
	}
	
	private boolean contains(List<Integer> list , Integer val ){
		for(int i=0;i<list.size();i++){
			if(val == list.get(i)){
				return true;
			};
		}
		return false;
	}

	public static void main(String[] args) {
		
		String fullFilePath = "C:\\Users\\clark\\Desktop\\test.xlsx";
		
		try {
			new ExcelManipulator().process(new FileInputStream(new File(fullFilePath)), "excel_test");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
}
