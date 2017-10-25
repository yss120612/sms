package com.yss1.sms2;




import java.io.IOException;
import java.util.ArrayList;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;


import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;





public class MainController {
	
		private Main mw;
		private ChangeListener<String> lr;
		public void setMainWnd(Main m)
		{
			mw=m;
		}
		FillDBTask st;
		ExcelTask  et;
		SMSTask    smst;
		@FXML
		private ProgressBar progress;

		@FXML
		private Label status_label;
		 
	    @FXML
	    private Button exit;

	    @FXML
	    private Button run;

	    @FXML
	    private Button excel;
	    
	    @FXML
	    private Button sms;
	    
	    @FXML
	    void onExit(ActionEvent event) {
	    	if (st!=null && st.isRunning())
	    	{
	    		st.cancel(true);
	    	}
	    	if (et!=null && et.isRunning())
	    	{
	    		et.cancel(true);
	    	}
	    	if (smst!=null && smst.isRunning())
	    	{
	    		smst.cancel(true);
	    	}
			 System.exit(0);
	    }

	    @FXML
	    void onRun(ActionEvent event) throws InterruptedException {
	    	progress.progressProperty().unbind();
	    	progress.setProgress(0);
	    	lr=new ChangeListener<String>() {
	    		@Override
	    		public void changed(ObservableValue<? extends String> observable,
	    				String oldValue, String newValue) {
//	    				try {
//							//Main.log.writeLog(newValue);
//	    					
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
	    		}
	    	}; 
	    	//status_label.textProperty().unbind();
	    	
	           
	        if (st==null)
	        {
	        	st=new FillDBTask();
	        }
	        progress.progressProperty().bind(st.progressProperty());
	        status_label.textProperty().bind(st.messageProperty());
	        status_label.textProperty().addListener(lr);
	    	run.setDisable(true);
	    	
	    	 // When completed tasks
            st.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, //
            		 new EventHandler<WorkerStateEvent>() {

                @Override
                public void handle(WorkerStateEvent t) {
                    Man man = st.getValue();
                    status_label.textProperty().unbind();
                    status_label.accessibleTextProperty().removeListener(lr);
                    progress.progressProperty().unbind();
                    progress.setProgress(1);
                    status_label.setText("Done. "+man.total+" records processed. tel["+man.processed+"]/err["+man.error+"]");
                    run.setDisable(false);
                    //System.out.println(mans.get(2).name);
                }
            });
	    	
	    	new Thread(st).start();
	    }
		
	    @FXML
	    void onExcel(ActionEvent event) {
	    	progress.progressProperty().unbind();
	    	progress.setProgress(0);
	    	if (et==null)
	        {
	        	et=new ExcelTask();
	        }
	        progress.progressProperty().bind(et.progressProperty());
	        status_label.textProperty().bind(et.messageProperty());
	       // status_label.textProperty().addListener(lr);
	    	excel.setDisable(true);
	    	et.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, //
	           		 new EventHandler<WorkerStateEvent>() {

	               @Override
	               public void handle(WorkerStateEvent t) {
	                   Man man = et.getValue();
	                   status_label.textProperty().unbind();
	                   //status_label.accessibleTextProperty().removeListener(lr);
	                   progress.progressProperty().unbind();
	                   progress.setProgress(1);
	                   status_label.setText("Done. "+man.total+" records processed. "+man.processed+" files created.");
	                   excel.setDisable(false);
	                   //System.out.println(mans.get(2).name);
	               }
	           });
		    	
		    	new Thread(et).start();
	    }

	    @FXML
	    void onSms(ActionEvent event) {
	    	
	    	progress.progressProperty().unbind();
	    	progress.setProgress(0);
	    	if (smst==null)
	        {
	        	smst=new SMSTask();
	        }
	        progress.progressProperty().bind(smst.progressProperty());
	        status_label.textProperty().bind(smst.messageProperty());
	       // status_label.textProperty().addListener(lr);
	    	sms.setDisable(true);
	    	
	    	smst.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, //
           		 new EventHandler<WorkerStateEvent>() {

               @Override
               public void handle(WorkerStateEvent t) {
                   Man man = smst.getValue();
                   status_label.textProperty().unbind();
                   //status_label.accessibleTextProperty().removeListener(lr);
                   progress.progressProperty().unbind();
                   progress.setProgress(1);
                   status_label.setText("Done. "+man.total+" records processed. Error:"+man.error);
                   sms.setDisable(false);
                   //System.out.println(mans.get(2).name);
               }
           });
	    	
	    	new Thread(smst).start();
	    }



}