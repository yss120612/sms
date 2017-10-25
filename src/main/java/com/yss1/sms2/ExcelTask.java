package com.yss1.sms2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import javafx.concurrent.Task;

public class ExcelTask extends Task<Man> {
	HSSFWorkbook workbook=null;
	@Override
	protected Man call() throws Exception {
		// TODO Auto-generated method stub
		
		HSSFSheet sheet=null;
		//System.out.println("AA");
		
		Row row;
	    Cell c;
	    Connection conn2 = null;
		Statement stmt2 = null;
		ResultSet rs2 = null;
		int total=0;
		int current=0;
		int counter=0;
		int files=0;
		int i=0;
		Date dt=new Date();
		SimpleDateFormat sdf=new SimpleDateFormat("dd.MM.yyyy");
		sdf.format(dt);
		
		DriverManager.registerDriver(new com.mysql.jdbc.Driver());
		DriverManager.setLoginTimeout(10);

		try
		{
		conn2 = DriverManager.getConnection("jdbc:mysql://10.48.0.62:3306/sms", "user", "1111");
		}
		catch (SQLException e)
		{
			System.out.println(e.getMessage());
		}

		stmt2 = conn2.createStatement();
		String sql="select count(*) from work_table where state=0 and tel='' order by dst";
		
		rs2 = stmt2.executeQuery(sql);
		
		if (rs2.next())
		{
			total=rs2.getInt(1);
			
		}

		rs2.close();
		sql="select dst,snils from work_table where state=0 and tel='' order by dst";
		
		rs2 = stmt2.executeQuery(sql);
		String S;
		while (rs2.next())
		{
			//System.out.println(i);
			if (current!=rs2.getInt(1))
			{
				files++;
				if (workbook!=null)
				{
				  try (FileOutputStream out = new FileOutputStream(new File("d:\\box\\"+rs2.getInt(1)+"_"+sdf.format(dt)+".xls"))) {
				         workbook.write(out);
				     } catch (IOException e) {
				         e.printStackTrace();
				         System.out.println(e.getMessage());
				     }
				  workbook.close();
				}
				workbook= new HSSFWorkbook();
				sheet= workbook.createSheet("נאימם "+rs2.getInt(1));
				sheet.setColumnWidth(0,4000);
				counter=0;
				current=rs2.getInt(1);
			}
		
	    row = sheet.createRow(counter++);
	    c=row.createCell(0);
	    S=rs2.getString(2);
	    c.setCellValue(S.substring(0,3)+"-"+S.substring(3,6)+"-"+S.substring(6,9)+" "+S.substring(9));
	    i++;
	    if (i % 100 == 0) {
			this.updateMessage(i + " records processed...");
			this.updateProgress(i,total);
		}
	}
		rs2.close();
		sql="update work_table set state=1 where state=0 and tel=''";
		stmt2.executeUpdate(sql);
		stmt2.close();
		conn2.close();
		return new Man(i,files,0);
	}

}
