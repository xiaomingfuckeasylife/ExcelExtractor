package org.fuckeasylife.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author clark
 * 
 * 2017年3月7日
 * 
 */
public interface Const {
	
	/**
	 * change pboc app_info status error 
	 */
	String CDE_RUN_ERROR_PBOC_STATUS_UPDATE_ERROR = "update AppInfo record Status to 1 error : ";
	
	/**
	 * insert additional_info error 
	 */
	String CDE_RUN_ERROR_PBOC_INSERT_ADDITIANAL_INFO_ERROR = "PBOC报告未填写插入错误 : ";
	
	/**
	 * allocation ca error 
	 */
	String CDE_RUN_ERROR_PBOC_ALLOCATE_CA_ERROR = "分配CA工单错误 : ";
	
	/**
	 * return projects errors
	 */
	String CDE_RUN_ERROR_RETURN_PROJECTS_ERROR = "withPboc method error : ";
	
	/**
	 * excel reading error 
	 */
	String CDE_RUN_ERROR_EXCEL_READING_ERROR = "Excel reading Error : " ;
	
	@SuppressWarnings("unchecked")
	List<String> COMPANY_TYPE_LIST = new ArrayList(){
		{
			add("国有企业");
			add("集体企业");
			add("全民企业");
			add("分公司");
			add("外国企业");
			add("合作社");
			add("个体工商户");
			add("上市公司");
		}
	};
	
	/**
	 * Processing error , tolerable . 
	 */
	Map<String,String> PROCESS_ERROR = new HashMap<String,String>(){
		{
			put("001", "excel processing");
		}
	};
	
	String MAPPED_TYPE = "MAPPED_TYPE";
}
