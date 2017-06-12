package org.fuckeasylife.sys.plugin.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author clark
 *
 * 2017年2月21日
 */
public class Db {
	
	private Config config ;
	
	public Db(Config config){
		this.config = config;
	}
	
	/**
	 * check if table is exist 
	 * @param conn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public synchronized boolean isTableExist( Connection conn,String sql) throws SQLException{
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			rs.next();
			return  rs.getInt(1) > 0;
		}finally{
			config.close(ps, conn);
		}
	}
	
	/**
	 * create table 
	 * @param conn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public synchronized boolean createTable(Connection conn, String sql) throws SQLException{
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			return ps.execute();
		}finally{
			config.close(ps,conn);
		}
	}
	
	/**
	 * insert table 
	 * @param conn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public synchronized int insertTableRecord(Connection conn, String sql) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			return ps.executeUpdate();
		}finally{
			config.close(ps,conn);
		}
	}
	
	
	public synchronized long selectLastPrimaryKey(Connection conn, String sql) throws SQLException{
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getLong(1);
		}finally{
			config.close(ps,conn);
		}
	}
	
	public synchronized Integer getRootTableLastNum(Connection conn , String sql )  throws SQLException{
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1);
		}finally{
			config.close(ps,conn);
		}
	}
	
	
	/**
	 * check if columns exists
	 * @param conn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public synchronized Integer isColumnExist(Connection conn, String sql) throws SQLException{
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1);
		}finally{
			ps.close();
		}
	}
	
	/**
	 * add columns 
	 * @param conn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public synchronized Integer addColumn(Connection conn, String sql) throws SQLException{
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			return ps.executeUpdate();
		}finally{
			ps.close();
		}
	}
	
	/**
	 * return retMap
	 * @param conn
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	public synchronized List<String> getTableInfo(Connection conn , String tableName) throws SQLException{
		List<String> retList = new ArrayList<String>();
		PreparedStatement ps = conn.prepareStatement("select * from " + tableName + " where  1 = 2");
		ResultSet rs = ps.executeQuery();
		ResultSetMetaData rsmd = rs.getMetaData();
		int count = rsmd.getColumnCount();
		for(int i=1;i<=count;i++){
//			String columnTypeName = rsmd.getColumnTypeName(i);
			String columnName = rsmd.getColumnName(i);
			retList.add(columnName);
		}
		return retList;
	}
}
