# ExcelExtractor
extract data from ExcelExtractor once for all , contains all kinds of feature including auto table creation , row recognition and so on. 

### The Altimate Excel data extract tools . 

#### what this tools can do ?

* first .  auto recognize the excel head which will be used as the table header . 

* second . auto add column if the header add a new column . 

* third .  auto table creation . insertion . 

* four .   auto row recognition . remove wired row . recognise which row is the real row that need to be save. 

#### how to use it ? 

```java
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
```

#### which database is it support ？ 

now it only support mysql . but you can extend more as you wish . 

