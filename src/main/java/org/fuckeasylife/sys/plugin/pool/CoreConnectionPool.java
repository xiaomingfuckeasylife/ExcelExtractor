package org.fuckeasylife.sys.plugin.pool;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.fuckeasylife.sys.plugin.db.SqlReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author clark
 *
 * 2017年4月19日
 * 
 * A class for pre-allocating, recycling, and managing JDBC connections.
 * <p>
 * It uses threads for opening a new connection. When no connection
 * available it will wait until a connection is released.
 * 
 * by the way fuck C3p0
 * 
 */
public class CoreConnectionPool implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(CoreConnectionPool.class);

	// ---------------------------------------------------------------- properties

	private static final String DEFAULT_VALIDATION_QUERY = "select 1";

	private String driver;
	private String url;
	private String user;
	private String password;
	private int maxConnections = 10;
	private int minConnections = 3;
	private boolean waitIfBusy;
	private boolean validateConnection = true;
	private long validationTimeout = 18000000L;		// 5 hours
	private String validationQuery;

	public String getDriver() {
		return driver;
	}

	/**
	 * Specifies driver class name.
	 */
	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * Specifies JDBC url.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	/**
	 * Specifies db username.
	 */
	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * Specifies db password.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * Sets max number of connections.
	 */
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public int getMinConnections() {
		return minConnections;
	}

	/**
	 * Sets minimum number of open connections.
	 */
	public void setMinConnections(int minConnections) {
		this.minConnections = minConnections;
	}

	public boolean isWaitIfBusy() {
		return waitIfBusy;
	}

	/**
	 * Sets if pool should wait for connection to be freed when none
	 * is available. If wait for busy is <code>false</code>
	 * exception will be thrown when max connection is reached.
	 */
	public void setWaitIfBusy(boolean waitIfBusy) {
		this.waitIfBusy = waitIfBusy;
	}

	public long getValidationTimeout() {
		return validationTimeout;
	}

	/**
	 * Specifies number of milliseconds from connection creation
	 * when connection is considered as opened and valid.
	 */
	public void setValidationTimeout(long validationTimeout) {
		this.validationTimeout = validationTimeout;
	}

	public String getValidationQuery() {
		return validationQuery;
	}

	/**
	 * Specifies query to be used for validating connections.
	 * If set to <code>null</code> validation will be performed
	 * by invoking <code>Connection#isClosed</code> method.
	 */
	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	/**
	 * Sets default validation query (select 1);
	 */
	public void setDefaultValidationQuery() {
		this.validationQuery = DEFAULT_VALIDATION_QUERY;
	}

	public boolean isValidateConnection() {
		return validateConnection;
	}

	/**
	 * Specifies if connections should be validated before returned.
	 */
	public void setValidateConnection(boolean validateConnection) {
		this.validateConnection = validateConnection;
	}

	// ---------------------------------------------------------------- init

	private ArrayList<ConnectionData> availableConnections, busyConnections;
	private boolean connectionPending;
	private boolean initialised;

	/**
	 * initialize 
	 */
	public synchronized void init() {
		if (initialised) {
			return;
		}
		if (log.isInfoEnabled()) {
			log.info("Core connection pool initialization");
		}
		try {
			Class.forName(driver);
		}
		catch (ClassNotFoundException cnfex) {
			throw new RuntimeException("Database driver not found: " + driver, cnfex);
		}

		if (minConnections > maxConnections) {
			minConnections = maxConnections;
		}
		availableConnections = new ArrayList<>(maxConnections);
		busyConnections = new ArrayList<>(maxConnections);

		for (int i = 0; i < minConnections; i++) {
			try {
				Connection conn = DriverManager.getConnection(url, user, password); 
				availableConnections.add(new ConnectionData(conn));
			} catch (SQLException sex) {
				throw new RuntimeException("No database connection", sex);
			}
		}
		initialised = true;
	}

	// ---------------------------------------------------------------- get/close

	/**
	 * retrieve a connection
	 */
	public synchronized Connection getConnection() {
		if (availableConnections == null) {
			throw new RuntimeException("Connection pool is not initialized");
		}
		if (!availableConnections.isEmpty()) {
			int lastIndex = availableConnections.size() - 1;
			ConnectionData existingConnection = availableConnections.get(lastIndex);
			availableConnections.remove(lastIndex);
			
			// If conn on available list is closed (e.g., it timed out), then remove it from available list
			// and repeat the process of obtaining a conn. Also wake up threads that were waiting for a
			// conn because maxConnection limit was reached.
			long now = System.currentTimeMillis();
			boolean isValid = isConnectionValid(existingConnection, now);
			if (!isValid) {
				if (log.isDebugEnabled()) {
					log.debug("Pooled connection not valid, resetting");
				}

				notifyAll();				 // freed up a spot for anybody waiting
				return getConnection();
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Returning valid pooled connection");
				}

				busyConnections.add(existingConnection);
				existingConnection.lastUsed = now;
				return existingConnection.connection;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("No more available connections");
		}

		// no available connections
		if (((availableConnections.size() + busyConnections.size()) < maxConnections) && !connectionPending) {
			makeBackgroundConnection();
		} else if (!waitIfBusy) {
			throw new RuntimeException("Connection limit reached: " + maxConnections);
		}
		// wait for either a new conn to be established (if you called makeBackgroundConnection) or for
		// an existing conn to be freed up.
		try {
			wait();
		} catch (InterruptedException ie) {
			// ignore
		}
		// someone freed up a conn, so try again.
		return getConnection();
	}

	/**
	 * Checks if existing connection is valid and available. It may happens
	 * that if connection is not used for a while it becomes inactive,
	 * although not technically closed.
	 */
	private boolean isConnectionValid(ConnectionData connectionData, long now) {
		if (!validateConnection) {
			return true;
		}
		
		if (now < connectionData.lastUsed + validationTimeout) {
			return true;
		}

		Connection conn = connectionData.connection;

		if (validationQuery == null) {
			try {
				return !conn.isClosed();
			} catch (SQLException sex) {
				return false;
			}
		}
		
		boolean valid = true;
		Statement st = null;
		try {
			st = conn.createStatement();
			st.execute(validationQuery);
		} catch (SQLException sex) {
			valid = false;
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (SQLException ignore) {
				}
			}
		}
		return valid;
	}

	/**
	 * You can't just make a new conn in the foreground when none are
	 * available, since this can take several seconds with a slow network
	 * conn. Instead, start a thread that establishes a new conn,
	 * then wait. You get woken up either when the new conn is established
	 * or if someone finishes with an existing conn.
	 */
	private void makeBackgroundConnection() {
		connectionPending = true;
		Thread connectThread = new Thread(this);
		connectThread.start();
	}

	public void run() {
		try {
			Connection connection = DriverManager.getConnection(url, user, password);
			synchronized(this) {
				availableConnections.add(new ConnectionData(connection));
				connectionPending = false;
				notifyAll();
			}
		} catch (Exception ex) {
			// give up on new conn and wait for existing one to free up.
		}
	}

	public synchronized void closeConnection(Connection connection) {
		ConnectionData connectionData = new ConnectionData(connection);
		// remove the last busy Connection
		busyConnections.remove(connectionData);
		availableConnections.add(connectionData);
		notifyAll();		// wake up threads that are waiting for a conn
	}


	// ---------------------------------------------------------------- close

	/**
	 * Close all the connections. Use with caution: be sure no connections are in
	 * use before calling. Note that you are not <i>required</i> to call this
	 * when done with a ConnectionPool, since connections are guaranteed to be
	 * closed when garbage collected. But this method gives more control
	 * regarding when the connections are closed.
	 */
	public synchronized void close() {
		if (log.isInfoEnabled()) {
			log.info("Core connection pool shutdown");
		}
		closeConnections(availableConnections);
		availableConnections = new ArrayList<>(maxConnections);
		closeConnections(busyConnections);
		busyConnections = new ArrayList<>(maxConnections);
	}

	private void closeConnections(ArrayList<ConnectionData> connections) {
		try {
			for (ConnectionData connectionData : connections) {
				Connection connection = connectionData.connection;
				if (!connection.isClosed()) {
					connection.close();
				}
			}
		} catch (SQLException sex) {
			// Ignore errors; garbage collect anyhow
		}
	}

	// ---------------------------------------------------------------- conn data

	/**
	 * Connection data with last used timestamp.
	 */
	class ConnectionData {
		final Connection connection;
		long lastUsed;

		ConnectionData(Connection connection) {
			this.connection = connection;
			this.lastUsed = System.currentTimeMillis();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			
			ConnectionData that = (ConnectionData) o;
			// in case of the nested proxy issue
			return connection.equals(that.connection.getClass().toString().indexOf("$Proxy") == -1 ? that.connection : ((SqlReporter)Proxy.getInvocationHandler(that.connection)).getOriginalObject());
		}

		@Override
		public int hashCode() {
			return connection.hashCode();
		}
	}

	// ---------------------------------------------------------------- stats

	/**
	 * Returns connection stats.
	 */
	public synchronized SizeSnapshot getConnectionsCount() {
		return new SizeSnapshot(availableConnections.size(), busyConnections.size());
	}

	/**
	 * Just a statistic class.
	 */
	public static class SizeSnapshot {
		final int totalCount;
		final int availableCount;
		final int busyCount;

		SizeSnapshot(int availableCount, int busyCount) {
			this.totalCount = availableCount + busyCount;
			this.availableCount = availableCount;
			this.busyCount = busyCount;
		}

		/**
		 * Returns total number of connections.
		 */
		public int getTotalCount() {
			return totalCount;
		}

		/**
		 * Returns number of available connections.
		 */
		public int getAvailableCount() {
			return availableCount;
		}

		/**
		 * Returns number of busy connections.
		 */
		public int getBusyCount() {
			return busyCount;
		}

		@Override
		public String toString() {
			return "Connections count: {total=" + totalCount +
					", available=" + availableCount +
					", busy=" + busyCount + '}';
		}
	}

}