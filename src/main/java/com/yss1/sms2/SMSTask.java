package com.yss1.sms2;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

//import com.yss1.ssms.SMSSender.PortReader;

import javafx.concurrent.Task;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SMSTask extends Task<Man> {
	private SerialPort serialPort;
	private PortReader portReader;
	private HashMap<Integer,String> rns;
	private ArrayList<Integer> sendOK;
	
	@Override
	protected Man call() throws Exception {
		// TODO Auto-generated method stub
		rns=new HashMap<Integer,String>();
		sendOK=new ArrayList<Integer>();
		Connection conn2 = null;
		Statement stmt2 = null;
		ResultSet rs2 = null;
		int rdb=0,count=0,err=0;
		serialPort = new SerialPort("COM4");
		portReader = new PortReader();
		String smsText="���� ��������� � ����������� �����������. ���������� ���������. ";
		conn2 = DriverManager.getConnection("jdbc:mysql://10.48.0.62:3306/Indicatives", "user", "1111");
		stmt2 = conn2.createStatement();
		rs2 = stmt2.executeQuery("select * from dst_upfr");
		int rn;
		while (rs2.next())
		{
			rn=rs2.getInt(1)-48000;
			if (rn==100) rn=106;
			rns.put(rn, rs2.getString(2));
			//System.out.println(rn+"="+rs2.getString(2));
		}
		rs2.close();
		stmt2.close();
		conn2.close();
		//if (1==1) return new Man(1,1,1);
		
		
		
		conn2 = DriverManager.getConnection("jdbc:mysql://10.48.0.62:3306/sms", "user", "1111");
		stmt2 = conn2.createStatement();
		String sql="select count(*) from work_table where state=0 and tel<>''";
		rs2 = stmt2.executeQuery(sql);
		if (rs2.next()) {
			rdb = rs2.getInt(1);
		}
		rs2.close();
		sql="select count(*) from work_table where state=0 and tel<>'' order by dst";
		rs2 = stmt2.executeQuery(sql);
		
		smsSend(smsText,"79501293569");
		while (rs2.next())
		{
			if (smsSend(smsText+rns.get(rs2.getInt("dst")),rs2.getString("tel")))
			{
				sendOK.add(rs2.getInt("id_process"));
				count++;
			}
			else
			{
				err++;
			}
			Thread.sleep(2000);
			this.updateMessage((count+err) + " records processed...");
			this.updateProgress(count+err, rdb);
			
		}
		
		rs2.close();
		for (int k:sendOK)
		{
			stmt2.executeUpdate("update work_table set state=2 where id_process="+k);
		}
		rs2.close();
		stmt2.close();
		conn2.close();
		return new Man(rdb,count,err);
	}

	
	public boolean smsSend(String sms, String phone) throws IOException {

		// ������� � ����������� ��� �����
		
		try {
			// ��������� ����
			serialPort.openPort();
			
			// ���������� ���������
			serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
			// serialPort.addEventListener(new PortReader(),
			// SerialPort.MASK_RXCHAR);

			// ��������� ���������
			String message = "0011000B91" + reversePhone(phone) + "0008A7" + StringToUSC2(sms);

			// ���������� ������ ����������
			char c = 0x0D;// ������ �������� ������� CR
			String str = "AT+CMGF=0" + c;
			serialPort.writeString(str);
			Thread.sleep(500); // ��-����, ����� ���� ����� ����� ������, �� ��
								// ����������� ������ ��������� � ����������
			// ������� ����
			serialPort.purgePort(serialPort.PURGE_RXCLEAR | serialPort.PURGE_TXCLEAR);
		
			str = "AT+CMGS=" + getSMSLength(message) + c;
			serialPort.writeString(str);
			Thread.sleep(500);
			serialPort.purgePort(serialPort.PURGE_RXCLEAR | serialPort.PURGE_TXCLEAR);
			// 
			c = 26;// ������ CTRL+Z
			serialPort.writeString(message + c);
			Thread.sleep(1000);

			serialPort.closePort();

			return true;
		} catch (SerialPortException ex) {
			System.out.println(ex.getMessage());
			return false;
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	public void smsRead() throws IOException
	{
		try {
		serialPort.openPort();
		serialPort.addEventListener(portReader);
		
		serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
		char c = 0x0D;// ������ �������� ������� CR
		String cmd ="AT+CMGL=4"+c;
		serialPort.writeString(cmd);
		serialPort.removeEventListener();
		serialPort.closePort();
		} catch (SerialPortException ex) {
			//serialPort.closePort();
			System.out.println(ex);
		}
	}
	
	
	private String reversePhone(String phone) {
		//System.out.println("HERE");
		phone=phone.replaceAll("^8", "7").replaceAll("\\+", "");
		if (phone.length() < 11 || !phone.matches("\\d+")) {
			throw new NumberFormatException("����� ������ �������� �� 11 ����");
		}
		phone += "F";
		String phoneRev = "";
		for (int i = 0; i <= 10; i = i + 2) {
			phoneRev = phoneRev + phone.charAt(i + 1) + phone.charAt(i);
		}
		return phoneRev;
	}
	
	private String StringToUSC2(String text) throws IOException {
		String str = "";

		byte[] msgb = text.getBytes("UTF-16");
		// ����������� ����� ���
		String msgPacked = "";
		for (int i = 2; i < msgb.length; i++) {
			String b = Integer.toHexString((int) msgb[i]);
			if (b.length() < 2)
				msgPacked += "0";
			msgPacked += b;
		}

		// ����� ������������� ������ � ������ �������
		String msglenPacked = Integer.toHexString(msgPacked.length() / 2);
		// ���� ����� �������� - ��������� � ����� 0
		if (msglenPacked.length() < 2)
			str += "0";

		// ��������� ������ �� ����� � ������ ���� ������
		str += msglenPacked;
		str += msgPacked;

		str = str.toUpperCase();

		return str;

	}
	
	// �������� ����� ���������
		private int getSMSLength(String sms) {
			return (sms.length() / 2 - 1);
		}

		private class PortReader implements SerialPortEventListener {

			public void serialEvent(SerialPortEvent event) {
				if (event.isRXCHAR() && event.getEventValue() > 0) {
					try {
						// �������� ����� �� ����������, ������������ ������ � �.�.
						String data = serialPort.readString(event.getEventValue());
						// � ����� ���������� ������
						System.out.println("response: " + data);
					} catch (SerialPortException ex) {
						System.out.println(ex);
					}
				}
			}
		}
		
}
