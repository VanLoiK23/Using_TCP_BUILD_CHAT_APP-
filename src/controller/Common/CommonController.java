package controller.Common;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class CommonController {

	
	private Stage stage;
	private Scene scene;
	private Parent root;
	
	public FXMLLoader loaderToResource(MouseEvent event,String pathView) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/"+pathView+".fxml"));
		root = loader.load();
		stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		scene = new Scene(root);
		scene.setFill(Color.WHITE);
		stage.setScene(scene);
		// stage.initStyle(StageStyle.DECORATED);
		stage.show();
		stage.centerOnScreen();
		
		return loader;
	}
	
	public void alertInfo(AlertType alertType,String title,String message) {
		Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
	
	public boolean isValidEmail(String email) {
	    // Regex chuẩn cho email
	    String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
	    Pattern pattern = Pattern.compile(emailRegex);
	    Matcher matcher = pattern.matcher(email);
	    return matcher.matches();
	}

}
