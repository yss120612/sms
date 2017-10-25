package com.yss1.sms2;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Main extends Application {

	public static Log log;
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		//System.out.println("QQqq");
		log=new Log();
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		//URL url=new URL("wnd_main.fxml");
		//FXMLLoader loader = new FXMLLoader(getClass().getResource("wnd_main.fxml"));
		FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/wnd_main.fxml"));
		//loader.setLocation(location);
		GridPane pane = null;
		try
		{
		pane = (GridPane) loader.load();
		}
		catch (Exception E)
		{
			System.out.println(E.getMessage());
		
			System.exit(0);
		}
		
		MainController controller = loader.getController();
	    controller.setMainWnd(this);
		
		Scene scene = new Scene( pane );
		primaryStage.setScene(scene);
		primaryStage.show();

	}

}
