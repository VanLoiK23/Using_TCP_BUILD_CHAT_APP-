package controller.LoginAndController;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NavigateBetweenLoginRegister implements Initializable {
	@FXML
	private VBox vbox;

	private Parent fxml;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		animationInterface(vbox.getLayoutX() * 21, "Login");
	}

	private void animationInterface(Double x, String view) {
	    TranslateTransition t = new TranslateTransition(Duration.seconds(1), vbox);
	    t.setToX(x);
	    t.play();

	    t.setOnFinished(e -> {
	        try {
	            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginAndRegister/" + view + ".fxml"));
	            Parent pane = loader.load();

	            // Nếu là Register thì truyền controller cha
	            if (view.equals("Register")) {
	                RegisterController registerController = loader.getController();
	                registerController.setParentController(this);
	            }

	            vbox.getChildren().clear();
	            vbox.getChildren().add(pane);

	        } catch (IOException ex) {
	            ex.printStackTrace();
	            System.out.println("Error loading " + view + ".fxml: " + ex.getMessage());
	        }
	    });
	}

	@FXML
	public void open_signin(ActionEvent e1) {
		animationInterface(vbox.getLayoutX() * 21, "Login");
	}

	@FXML
	public void open_signup(ActionEvent e1) {
		animationInterface(0.0, "Register");
	}
}
