package org.fuckeasylife.sys.plugin.db;

import javax.sql.DataSource;
/**
 * 
 * @author clark
 *
 * 2017年2月20日
 * 
 * provide dataSource 
 */
public interface IDataSourceProvider {
	/**
	 * get DataSource 
	 * @return
	 */
	DataSource getDataSource();
}
