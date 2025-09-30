package controller.LoginAndController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import controller.MainController;
import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import model.Packet;
import model.User;
import service.UserService;

public class LoginController {
	private SocketClient socketClient;

	@FXML
	private ImageView img;

	@FXML
	public void exit(MouseEvent event) {
		Stage stage = (Stage) img.getScene().getWindow();
		stage.close();
	}

	@FXML
	private PasswordField password;

	@FXML
	private TextField username;

	private CommonController commonController;

	private UserService userService = new UserService();

	@FXML
	void submit(MouseEvent event) throws IOException, InterruptedException {
		if (username.getText() != null && password.getText() != null && !username.getText().isEmpty()
				&& !password.getText().isEmpty()) {
			
			socketClient = SocketClient.getInstance();

			User userRequest = new User();
			userRequest.setUsername(username.getText());
			userRequest.setPassword(password.getText());

			Packet packetRequest = new Packet();
			packetRequest.setType("LOGIN");
			packetRequest.setData(userRequest);

			socketClient.sendPacket(packetRequest);

			commonController = new CommonController();

			// receive reponse from server chờ 5 s nếu ko có phản hồi
//			Packet packetReponse=socketClient.receivePacket();
			Packet packetReponse = socketClient.responseQueue.poll(5, TimeUnit.SECONDS);
			
			if (packetReponse != null) {

				if (packetReponse.getType().equals("LOGIN_RESULT")) {
					User user = socketClient.gson.fromJson(socketClient.gson.toJson(packetReponse.getData()),
							User.class);
//			    User user = (User) packetReponse.getData();
					if (user != null) {
						System.out.println(user);
						commonController.alertInfo(AlertType.CONFIRMATION, "Success!!!!",
								"Bạn đã đăng nhập thành công");
						userService.setUpLogin(user);

						if (user.getRole().equals("admin")) {
							MainController mainController = commonController.loaderToResource(event, "Chat/form_Chat")
									.getController();
							mainController.setUser(user);
						} else {
							MainController mainController = commonController.loaderToResource(event, "Chat/form_Chat")
									.getController();
							mainController.setUser(user);
						}
					} else {
						commonController.alertInfo(AlertType.WARNING, "Cảnh báo!!!!",
								"Không tìm thấy thông tin tài khoản!");
					}
				}
			}else {
				commonController.alertInfo(AlertType.ERROR, "Cảnh báo!!!!",
						"Không nhận được phản hồi nào từ server!");
			}

			clean();

		} else {
			commonController.alertInfo(Alert.AlertType.WARNING, "Cảnh báo!", "Vui lòng nhập đầy đủ thông tin!");
			clean();
		}
	}

	void clean() {
		username.setText(null);
		password.setText(null);
	}

}
