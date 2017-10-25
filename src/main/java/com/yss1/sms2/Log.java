package com.yss1.sms2;


import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
private FileWriter logFile;
public Log() throws IOException
{
	logFile=new FileWriter("\\sms.log",true);
}

public void writeLog(String ws) throws IOException
{
	SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy' 'hh:mm:ss");
	logFile.write(formatForDateNow.format(new Date())+" "+ws+"\n");
	logFile.flush();
	
}

@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		logFile.close();
		super.finalize();
	}

}
