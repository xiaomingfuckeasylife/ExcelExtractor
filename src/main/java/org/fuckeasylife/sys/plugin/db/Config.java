package org.fuckeasylife.sys.plugin.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.fuckeasylife.sys.plugin.db.dialect.Dialect;
import org.fuckeasylife.sys.plugin.db.dialect.MysqlDialect;
import org.fuckeasylife.sys.plugin.pool.CoreConnectionPool;
import org.fuckeasylife.util.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author clark
 *
 * 2017年2月20日
 * 
 * DataSource Config
 */
public class Config {
	/**
	 * Each Thread should have their own connection for the thread safety issue.
	 */
	private final ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();
	
	private Logger logger = LoggerFactory.getLogger(Config.class);
	/**
	 *  property reader
	 */
	private static PropertiesLoader loader = new PropertiesLoader("db.properties");
	
	private static CoreConnectionPool pool = new CoreConnectionPool();
	
	static {
		pool.setDriver(loader.getProperty("jdbc.driver"));
		pool.setUser(loader.getProperty("jdbc.username"));
		pool.setPassword(loader.getProperty("jdbc.password"));
		pool.setUrl(loader.getProperty("jdbc.url"));
		pool.setWaitIfBusy(true);
		pool.init();
	}
	
	String name;
	Dialect dialect;
	boolean showSql = true;
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public boolean isShowSql() {
		return showSql;
	}

	public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}
	
	/**
	 * Constructor with name and dataSource
	 */
	public Config(String name) {
		this(name, new MysqlDialect());
	}
	
	/**
	 * Constructor with name, dataSource and dialect
	 */
	public Config(String name , Dialect dialect) {
		if(dialect == null){
			throw new IllegalArgumentException("dialect can not be null");
		}
		this.name = name;
		this.dialect = dialect;
	}
	
	public final void setThreadLocalConnection(Connection connection) {
		threadLocal.set(connection);
	}
	
	public final void removeThreadLocalConnection() {
		threadLocal.remove();
	}
	
	/**
	 * Get Connection. Support transaction 
	 */
	public final Connection getConnection() throws SQLException {
		logger.debug("" + pool.getConnectionsCount());
		Connection conn = threadLocal.get();
		if (conn != null){
			return conn;
		}else {
			conn = pool.getConnection();
			setThreadLocalConnection(conn);
		}
		return showSql && conn.getClass().toString().indexOf("$Proxy") == -1 ? new SqlReporter(conn).getConnection() : conn;
	}
	
	public final Connection getCdeConnection() throws SQLException{
		logger.debug("" + pool.getConnectionsCount());
		Connection conn = pool.getConnection();
		return showSql && conn.getClass().toString().indexOf("$Proxy") == -1  ? new SqlReporter(conn).getConnection() : conn;
	}
	
	/**
	 * Close ResultSet、Statement、Connection
	 * ThreadLocal support declare transaction.
	 */
	public final void close(ResultSet rs, Statement st, Connection conn) {
		if (rs != null) {try {rs.close();} catch (SQLException e) {throw new RuntimeException(e.getMessage(), e);}}
		if (st != null) {try {st.close();} catch (SQLException e) {throw new RuntimeException(e.getMessage(), e);}}
		// only if threadLocal contains no connection then close connection.
		if (threadLocal.get() == null) {	// in transaction if conn in threadlocal
			if (conn != null) {try {conn.close();}
			catch (SQLException e) {throw new RuntimeException(e);}}
		}
	}
	
	public final void close(Statement st, Connection conn) {
		if (st != null) {try {st.close();} catch (SQLException e) {throw new RuntimeException(e.getMessage(), e);}}
		// only if threadLocal contains no connection then close connection.
		if (threadLocal.get() == null) {	// in transaction if conn in threadlocal
			if (conn != null) {try {conn.close();}
			catch (SQLException e) {throw new RuntimeException(e);}}
		}
	}
	
	public final void close() {
		logger.debug("" + pool.getConnectionsCount());
		// only if threadLocal contains no connection then close connection.
//		if (threadLocal.get() == null)		// in transaction if conn in threadlocal
//			if (conn != null)
//				try {conn.close();} catch (SQLException e) {throw new RuntimeException(e);}
		pool.close();
	}
	

	public final void close(Connection conn) {
		logger.debug("" + pool.getConnectionsCount());
		// only if threadLocal contains no connection then close connection.
//		if (threadLocal.get() == null)		// in transaction if conn in threadlocal
//			if (conn != null)
//				try {conn.close();} catch (SQLException e) {throw new RuntimeException(e);}
	
		pool.closeConnection(conn);
	}
	
}
