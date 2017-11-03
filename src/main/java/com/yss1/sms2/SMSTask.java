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

@SuppressWarnings("restriction")
public class SMSTask extends Task<Man> {
	private SerialPort serialPort;
	private HashMap<Integer, String> rns;
	//private ArrayList<Integer> sendOK;
	private static byte msgNo = (byte) 255;
	private static boolean more;
	private ArrayList<String> smsParts;

	@Override
	protected Man call() throws Exception {
		// TODO Auto-generated method stub
		rns = new HashMap<Integer, String>();
		//sendOK = new ArrayList<Integer>();
		Connection conn2 = null;
		Statement stmt1,stmt2 = null;
		ResultSet rs2 = null;
		int rdb = 0, count = 0, err = 0;
		serialPort = new SerialPort("COM9");
//		char ch=0x0D;
//		serialPort.openPort();
//		serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
//		serialPort.addEventListener(portReader, SerialPort.MASK_RXCHAR);
//		serialPort.writeString("AT+CMGD=1,4" + ch);
//		Thread.sleep(10000);
//		serialPort.closePort();
//		if (1==1) return null;
//		
		smsParts = new ArrayList<String>();
		String smsText = "Ваше заявление о перерасчете рассмотрено. Перерасчет невыгоден. ";
		conn2 = DriverManager.getConnection("jdbc:mysql://10.48.0.62:3306/Indicatives", "user", "1111");
		stmt2 = conn2.createStatement();
		rs2 = stmt2.executeQuery("select * from dst_upfr");
		int rn;
		while (rs2.next()) {
			rn = rs2.getInt("dst") - 48000;
			if (rn == 100)
				rn = 106;
			rns.put(rn, rs2.getString("vc_upfr"));
		}
		rs2.close();
		stmt2.close();
		conn2.close();
		
//		smsLargeSend(smsText+ rns.get(2)+"A", "79501293569");
//		Thread.sleep(2000);
//		smsLargeSend((smsText+ rns.get(2)).length()+smsText+ rns.get(2), "79501293569");
//		if (1==1) return null;
		conn2 = DriverManager.getConnection("jdbc:mysql://10.48.0.62:3306/sms", "user", "1111");
		stmt2 = conn2.createStatement();
		String sql = "select count(*) from work_table where state=0 and tel<>''";
		rs2 = stmt2.executeQuery(sql);
		if (rs2.next()) {
			rdb = rs2.getInt(1);
		}
		rs2.close();

		sql = "select * from work_table where state=0 and tel<>'' order by dst,tel";
		try {
			rs2 = stmt2.executeQuery(sql);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		int max_session=0;
		rdb=50;
		stmt1=conn2.createStatement();
		try {
			while (rs2.next()) {
				System.out.println("Посылаем тел:"+rs2.getString("tel"));
				if (smsLargeSend(smsText + rns.get(rs2.getInt("dst")), rs2.getString("tel"))) {
					//sendOK.add(rs2.getInt("id"));
					stmt1.executeUpdate("update work_table set state=2 where id="+rs2.getInt("id"));
					count++;
				} else {
					err++;
				}
				Thread.sleep(3000);
				this.updateMessage((count + err) + " records processed...");
				this.updateProgress(count + err, rdb);
				if (++max_session>rdb) {
					break;
				}
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	
		rs2.close();
	
//		try {
//			sql = "update work_table set state=2 where id in " + sendOK.toString();
//			sql = sql.replace('[', '(').replace(']', ')');
//			stmt2.executeUpdate(sql);
//		} catch (Exception ex) {
//			System.out.println(ex.getMessage());
//			System.out.println(sql);
//		}

		stmt1.close();
		stmt2.close();
		conn2.close();
		char ch=0x0D;
		serialPort.openPort();
		serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
		serialPort.addEventListener(portReader, SerialPort.MASK_RXCHAR);
		serialPort.writeString("AT+CMGD=1,4" + ch);
		Thread.sleep(5000);
		serialPort.removeEventListener();
		serialPort.closePort();
		return new Man(rdb, count, err);
	}

	private boolean makeParts(String text, int one) {
		smsParts.clear();
		int idx = 0;
		while (idx < text.length()) {
			try {
				smsParts.add(text.substring(idx, idx + one >= text.length() ? text.length() : idx + one));
			} catch (Exception Ex) {
				System.out.println(Ex.getMessage());
			}
			idx += one;
		}
		return smsParts.size() > 0;
	}

	private class PortReader implements SerialPortEventListener {

		public void serialEvent(SerialPortEvent event) {
			if (event.isRXCHAR() && event.getEventValue() > 0) {
				try {

					String data = serialPort.readString(event.getEventValue());
					if (!data.isEmpty()) {
						serialPort.purgePort(serialPort.PURGE_RXCLEAR | serialPort.PURGE_TXCLEAR);
					}
					System.out.println("response: " + data);
					if (data.contains("CMGS:")) {
						more = true;
					}
				} catch (SerialPortException ex) {
					System.out.println(ex);
				}
			}
		};

	};

	PortReader portReader = new PortReader();

	public boolean smsLargeSend(String sms, String phone) throws IOException, SerialPortException {
		if (!makeParts(sms, 67)) {
			return false;
		}
		byte length = (byte) smsParts.size();
		byte counter = 0;
		msgNo = (byte) ((msgNo == 255) ? 0 : msgNo + 1);
		if (length > 5) {
			return false;
		}

		serialPort.openPort();
		serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
		serialPort.addEventListener(portReader, SerialPort.MASK_RXCHAR);
		boolean success = true;
		for (String s : smsParts) {
			counter++;
			
			if (!smsSend(s, phone, msgNo, counter, length)) {

				success = false;
				break;
			}
			while (!more) {
				try {
					System.out.println("wait more...");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		serialPort.removeEventListener();
		serialPort.closePort();

		return success;
	}

	public boolean smsSend(String sms, String phone, byte msNo, byte pNo, byte pAll) throws IOException {
		try {
			// 00 SCA
			// 11 PDU-Type – Тип сообщения. Поле флагов (51 для многостраничного)
			// 00 TP- MR – TP-Message-Reference нужно менять для каждого сообщения (кусочка)
			// 0b количество цифр в номере получателя
			// 91 тип номера получателя
			// reverse phone
			// 00 идентификатор протокола всегда 00
			// 08 схема кодирования данных
			// A7 время жизни

			// String message = "0011000B91" + reversePhone(phone) + "0008A7" +
			// StringToUSC2(sms);

			String body = String.format("050003%02X%02X%02X", msgNo, pAll, pNo) + StringToUSC2(sms);
			// String message = String.format("0041%02x0B91", msNo) + reversePhone(phone) +
			// "0008A7"
			String message = String.format("0041%02X0B91", pNo - 1) + reversePhone(phone) + "0008"
					+ String.format("%02X", body.length() / 2) + body;

			char c = 0x0D;
			String str = "AT+CMGF=0" + c;
			serialPort.writeString(str);
			Thread.sleep(1500);

			str = "AT+CMGS=" + getSMSLength(message) + c;
			serialPort.writeString(str);
			//System.out.println("port write:"+str);
			Thread.sleep(1500);

			c = 26;// CTRL+Z
			more = false;
			serialPort.writeString(message + c);
			//System.out.println("port write:"+message);
			Thread.sleep(1500);

			return true;
		} catch (SerialPortException ex) {
			System.out.println(ex.getMessage());
			return false;
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}

	public void smsRead() throws IOException {
		try {
			serialPort.openPort();
			serialPort.addEventListener(portReader);

			serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			char c = 0x0D;// CR
			String cmd = "AT+CMGL=4" + c;
			serialPort.writeString(cmd);
			serialPort.removeEventListener();
			serialPort.closePort();
		} catch (SerialPortException ex) {
			System.out.println(ex);
		}
	}

	private String reversePhone(String phone) {
		phone = phone.replaceAll("^8", "7").replaceAll("\\+", "");
		if (phone.length() < 11 || !phone.matches("\\d+")) {
			throw new NumberFormatException("Номер должен содержать 11 цифр");
		}
		phone += "F";
		String phoneRev = "";
		for (int i = 0; i <= 10; i = i + 2) {
			phoneRev = phoneRev + phone.charAt(i + 1) + phone.charAt(i);
		}
		return phoneRev;
	}

	private String StringToUSC2(String text) throws IOException {
		byte[] msgb = text.getBytes("UTF-16");
		String msgPacked = "";
		for (int i = 2; i < msgb.length; i++) {

			msgPacked += String.format("%02X", msgb[i]);
		}
		return msgPacked;
	}

	private int getSMSLength(String sms) {
		// -1 т.к. без ведушего 00 (смс колл центр)
		return (sms.length() / 2 - 1);
	}

}
