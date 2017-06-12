import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.fuckeasylife.ExcelManipulator;

public class AppTest {
	public static void main(String[] args) {
		
		ExcelManipulator manipulator = new ExcelManipulator();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File("C:/Users/clark/workspace/target/service/ExcelExtractor/src/main/resources/test.xlsx"));
			manipulator.process(fis, "Mytest");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}finally{
			try {
				fis.close();
			} catch (IOException e) {
			}
		}
		
//		File fileB = new File(AppTest.class.getResource("").getPath());
//
//		System.out.println("fileB path: " + fileB);
//		
//		System.out.println("user.dir path: " + System.getProperty("user.dir"));


	}
}
