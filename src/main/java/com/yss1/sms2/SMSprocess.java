package com.yss1.sms2;

import java.io.IOException;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException; 

public class SMSprocess {
	public SMSprocess() throws Exception {
		serialPort = new SerialPort("COM4");
		portReader = new PortReader();
		
	}
	
	
	private SerialPort serialPort;
	private PortReader portReader;

	// Функция разворачивания номера в нужном формате
	// Телефон в международном формате имеет 11 символов (79231111111)
	// 11-Нечётное число, поэтому в конце добавляем F
	// И переставляем попарно цифры местами. Этого требует PDU-формат
	
	public static String reversePhone(String phone) {
		phone=phone.trim().replaceAll("[\\s,\\+,\\-,(,)]+", "").replaceAll("^8","7");
		
		if (phone.length() < 11 || !phone.matches("\\d+")) {
			return "";
		}
		
		phone += "F";
		//if (1==1) return phone;
		String phoneRev = "";
		for (int i = 0; i <= 10; i = i + 2) {
			phoneRev = phoneRev + phone.charAt(i + 1) + phone.charAt(i);
		}
		return phoneRev;
	}

	// Функция конвертации текста СМС-ки в USC2 формат вместе с длиной сообщения
	// (Возвращаемое значение <длина пакета><пакет>)
	private static String StringToUSC2(String text) throws IOException {
		String str = "";

		byte[] msgb = text.getBytes("UTF-16");
		// Конвертация самой СМС
		String msgPacked = "";
		for (int i = 2; i < msgb.length; i++) {
			String b = Integer.toHexString((int) msgb[i]);
			if (b.length() < 2)
				msgPacked += "0";
			msgPacked += b;
		}

		// Длина получившегося пакета в нужном формате
		String msglenPacked = Integer.toHexString(msgPacked.length() / 2);
		
		// Если длина нечётная - добавляем в конце 0
		if (msglenPacked.length() < 2)
			str += "0";

		// Формируем строку из длины и самого тела пакета
		str += msglenPacked;
		str += msgPacked;
		str = str.toUpperCase();
		return str;
	}

	// Получить длину сообщения
	private static int getSMSLength(String sms) {
		return (sms.length() / 2 - 1);
	}

	public void smsRead() throws IOException
	{
		try {
		serialPort.openPort();
		serialPort.addEventListener(portReader);
		
		serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
		char c = 0x0D;// Символ перевода каретки CR
		String cmd ="AT+CMGL=4"+c;
		serialPort.writeString(cmd);
		serialPort.removeEventListener();
		serialPort.closePort();
		} catch (SerialPortException ex) {
			//serialPort.closePort();
			System.out.println(ex);
		}
	}
	
	public boolean smsSend(String sms, String phone) throws IOException {

		// Передаём в конструктор имя порта
		
		try {
			// Открываем порт
			String phoneR=reversePhone(phone);
			if (phoneR.isEmpty())
			{
				return false;
			}
			serialPort.openPort();
			serialPort.addEventListener(portReader);
			// Выставляем параметры
			serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
			// serialPort.addEventListener(new PortReader(),
			// SerialPort.MASK_RXCHAR);

			// Формируем сообщение
			String message = "0011000B91" + reversePhone(phone) + "0008A7" + StringToUSC2(sms);

			// Отправляем запрос устройству
			char c = 0x0D;// Символ перевода каретки CR
			String str = "AT+CMGF=0" + c;
			//REMOVE serialPort.writeString(str);
			Thread.sleep(500); // По-идее, здесь надо ждать ответ модема, но мы
								// ограничимся просто ожиданием в полсекунды
			// Очистим порт
			serialPort.purgePort(serialPort.PURGE_RXCLEAR | serialPort.PURGE_TXCLEAR);

			str = "AT+CMGS=" + getSMSLength(message) + c;
			//REMOVE  serialPort.writeString(str);
			Thread.sleep(500);
			serialPort.purgePort(serialPort.PURGE_RXCLEAR | serialPort.PURGE_TXCLEAR);

			c = 26;// Символ CTRL+Z
			//REMOVE serialPort.writeString(message + c);
			Thread.sleep(3000);
			serialPort.removeEventListener();
			serialPort.closePort();
			
			return true;
		} catch (SerialPortException ex) {
			System.out.println(ex);
			return false;
		} catch (InterruptedException e) {
			System.out.println(e);
			return false;
		}

	}

	// Класс считывания ответов. Я решил обойтись без него, но в документации к
	// JSSC всё есть 🙂
	private class PortReader implements SerialPortEventListener {

		public void serialEvent(SerialPortEvent event) {
			if (event.isRXCHAR() && event.getEventValue() > 0) {
				try {
					// Получаем ответ от устройства, обрабатываем данные и т.д.
					String data = serialPort.readString(event.getEventValue());
					// И снова отправляем запрос
					System.out.println("response: " + data);
				} catch (SerialPortException ex) {
					System.out.println(ex);
				}
			}
		}
	}

}
