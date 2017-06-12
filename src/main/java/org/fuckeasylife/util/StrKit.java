package org.fuckeasylife.util;
/**
 * 
 * @author clark
 *
 * 2017年2月23日
 */
public final class StrKit<T> {
	
	public static<T> boolean isBlank(T str){
		
		return str == null || "".equals((str+"").trim())|| "null".equals(str) || "nil".equals(str);
	}
	
	public static String discardBlank(String str){
		
		return isBlank(str) ? "" : str.trim();
	}
	
	public static boolean compareStr(String str1 , String str2){
		if(str1 == null){
			return str2 == null;
		}
		if(str2 == null){
			return false;
		}
		return str1.trim().equals(str2.trim());
	}
	
	/**
	 * remove chinese charactor 
	 * @param str
	 * @return
	 */
	public static String rmvChineseChr(String str){
		
		String retStr = "";
		char[] charArr = str.toCharArray();
		for(int i=0;i<charArr.length;i++){
			
			char c = charArr[i];
			
			if((48 <= (int)c && (int)c <= 57) || (65 <= (int)c && (int)c <= 90) || (97 <= (int)c && (int)c <= 122)){
				continue;
			}
			if(c < '\u4e00'|| c > '\u9fa5'){
				continue;
			}
			retStr +=c;
		}
		return retStr;
	}
	
	public static int str2Int(String str){
		char[] charArr = str.toCharArray();
		int index = 0;
		for(int i=0;i<charArr.length;i++){
			
			if(charArr[i] == ' '){
				continue;
			}
			index +=(int)charArr[i];
			
		}
		return index;
	}
	
}
