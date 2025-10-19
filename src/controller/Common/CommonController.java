package controller.Common;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
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
	
	public FXMLLoader loaderToResource(Node someNodeInCurrentScene,String pathView) throws IOException {
	    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + pathView + ".fxml"));
	    Parent root = loader.load();

	    Stage stage = (Stage) someNodeInCurrentScene.getScene().getWindow();
	    Scene scene = new Scene(root);
	    scene.setFill(Color.WHITE);
	    stage.setScene(scene);
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
	
	public void alertConfirm(String title, String message, Consumer<Boolean> callback) {
	    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
	    alert.setTitle(title);
	    alert.setHeaderText(null);
	    alert.setContentText(message);

	    Optional<ButtonType> result = alert.showAndWait();
	    boolean confirmed = result.isPresent() && result.get() == ButtonType.OK;
	    callback.accept(confirmed);
	}

	public void onExit(Button btEx) {
	    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
	    alert.setTitle("Xác nhận");
	    alert.setHeaderText("Bạn có chắc muốn hủy tạo nhóm không?");
	    alert.setContentText("Mọi dữ liệu chưa lưu sẽ bị mất.");

	    ButtonType okButton = new ButtonType("Thoát", ButtonBar.ButtonData.OK_DONE);
	    ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
	    alert.getButtonTypes().setAll(okButton, cancelButton);

	    // Hiển thị và lấy kết quả
	    alert.showAndWait().ifPresent(response -> {
	        if (response == okButton) {
	            // Đóng cửa sổ hiện tại
	            Stage stage = (Stage) btEx.getScene().getWindow();
	            stage.close();
	        }
	    });
	}

	
	public boolean isValidEmail(String email) {
	    // Regex chuẩn cho email
	    String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
	    Pattern pattern = Pattern.compile(emailRegex);
	    Matcher matcher = pattern.matcher(email);
	    return matcher.matches();
	}
	
	public boolean isValidNumber(String input) {
	    if (input == null || input.isEmpty()) return false;
	    try {
	        Integer.parseInt(input); 
	        return true;
	    } catch (NumberFormatException e) {
	        return false;
	    }
	}


	public Boolean checkValidTextField(TextField textField) {
		return (textField.getText()!=null && !textField.getText().isEmpty());
	}
}
