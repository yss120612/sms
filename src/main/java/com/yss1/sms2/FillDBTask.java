package com.yss1.sms2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
//import  COM.ibm.db2.jdbc.app;

import javafx.concurrent.Task;

public class FillDBTask extends Task<Man> {

	@SuppressWarnings("restriction")
	@Override
	protected Man call() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
		// Connection con = null;
		// DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
		// Class.forName("COM.ibm.db2.jdbc.app.DB2Driver");

		String sql = "SELECT count(*)  from KS_PER_SH WHERE gr4=1 AND otkaz=1";
		String sql2;
		// Class.forName("com.ibm.db2.jcc.DB2Driver");
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn2 = null;
		Statement stmt2 = null;
		ResultSet rs2 = null;
		int ri = 0, ru = 0, rc = 0, rp = 0, rdb = 0;
		String tel;
		try {
			// Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			DriverManager.registerDriver(new com.mysql.jdbc.Driver());
			conn = DriverManager.getConnection("jdbc:mysql://10.48.0.62:3306/Indicatives", "user", "1111");
			conn2 = DriverManager.getConnection("jdbc:mysql://10.48.0.62:3306/sms", "user", "1111");
			stmt = conn.createStatement();
			stmt2 = conn2.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				rdb = rs.getInt(1);
			}
			rs.close();

			sql = "SELECT id,dst,d_comming,d_completion,id_process,tel,cnils  from KS_PER_SH WHERE gr4=1 AND otkaz=1";
			rs = stmt.executeQuery(sql);

			while (rs.next()) {
				tel = "";
				if (rs.getString("tel") != null) {
					tel = getTel(rs.getString("tel"));
					if (!tel.isEmpty()) {
						ri++;
					} else {
						rc++;
					}
				}
				rp++;
				if (rp % 100 == 0) {
					this.updateMessage(rp + " records processed...");
					this.updateProgress(rp, rdb);
				}
				sql2 = "select dst,snils,tel,state,datein,dateend,id_process from work_table where id_process="
						+ rs.getInt("id_process");

				rs2 = stmt2.executeQuery(sql2);
				sql2 = "";
				if (rs2.next()) {//правим тел. только если не уведомляли
					if (rs2.getInt("state") < 1 && !tel.equals(rs2.getString("tel"))) {
						sql2 = "update work_table set  tel='" + tel + "' where id_process=" + rs.getInt("id_process");

					} else {

					}
				} else {//не нашли - вставим новенького
					sql2 = "insert into work_table (dst,snils,tel,state,datein,dateend,id_process) values ("
							+ rs.getInt("dst") + ",'" + rs.getString("cnils") + "','" + tel + "',0,'" + rs.getDate("d_comming") + "','"
							+ rs.getDate(4) + "'," + rs.getInt("id_process") + ")";
				}
				rs2.close();
				if (!sql2.isEmpty()) {
					stmt2.execute(sql2);
				}

			}
		} catch (SQLException se) {
			System.out.println(se.getMessage());
		} finally {
			if (rs != null && !rs.isClosed())
				rs.close();
			if (stmt != null && !stmt.isClosed())
				stmt.close();
			if (conn != null && !conn.isClosed())
				conn.close();
			if (rs2 != null && !rs2.isClosed())
				rs2.close();
			if (stmt2 != null && !stmt2.isClosed())
				stmt2.close();
			if (conn2 != null && !conn2.isClosed())
				conn2.close();
		}

		System.out.println("Tel found:" + ri + " total :" + rp + " no tel:" + rc);

		return new Man(rp, ri, rc);
	}

	private String getTel(String tel) {
		String rtel = tel.replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\-", "").replaceAll("\\,", " ")
				.replaceAll("\\s+", " ");
		// System.out.println("rtel=" + rtel + " tel=" + tel);
		int idx = 0;
		boolean success = false;
		if (rtel.matches("(.*)\\+79(.*)")) {
			idx = rtel.indexOf("+79");
			if (idx >= 0 && rtel.length() > idx + 12) {
				rtel = rtel.substring(idx, idx + 12);
			}
			success = (rtel.length() == 12 && rtel.matches("\\+\\d{11,11}"));

			// System.out.println("found+7:"+rtel+ " success="+success);
		} else if (rtel.matches("(.*)89(.*)")) {
			idx = rtel.indexOf("89");
			if (idx >= 0 && rtel.length() > idx + 11) {
				rtel = rtel.substring(idx, idx + 11);
			}
			success = (rtel.length() == 11 && rtel.matches("\\d{11,11}"));
			//
		}

		if (success)

		{
			return rtel;
		} else {
			return "";
		}
	}

}
