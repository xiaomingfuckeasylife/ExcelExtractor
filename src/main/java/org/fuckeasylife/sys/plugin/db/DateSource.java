package org.fuckeasylife.sys.plugin.db;

import java.sql.Connection;

/**
 * 
 * @author clark
 * 
 * Dec 29, 2016
 */
public interface DateSource {
	
	/**
	 * get connection 
	 * @return
	 */
	public Connection getConnection();
	
	/**
	 * 
	 * @return
	 */
	public boolean close();
}
