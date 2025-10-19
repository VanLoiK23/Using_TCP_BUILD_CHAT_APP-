package controller.LoginAndController;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import controller.MainController;
import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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

public class LoginController implements Initializable {
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

	private CommonController commonController = new CommonController();;

	private UserService userService = new UserService();

	@FXML
	void submit(MouseEvent event) throws IOException, InterruptedException {
		if (username.getText() != null && password.getText() != null && !username.getText().isEmpty()
				&& !password.getText().isEmpty()) {

//			User userRequest = new User();
//			userRequest.setUsername(username.getText());
//			userRequest.setPassword(password.getText());
//
//			Packet packetRequest = new Packet();
//			packetRequest.setType("LOGIN");
//			packetRequest.setData(userRequest);

			socketClient.sendLogin(username.getText(), password.getText());

			// receive reponse from server chờ 5 s nếu ko có phản hồi
//			Packet packetReponse=socketClient.receivePacket();
			Packet packetReponse;
			if (socketClient.getInstance().responseQueue != null) {

				try {
					packetReponse = socketClient.getInstance().responseQueue.poll(10, TimeUnit.SECONDS);

					if (packetReponse != null) {

						if (packetReponse.getType().equals("LOGIN_RESULT")) {
							User user = socketClient.gson.fromJson(socketClient.gson.toJson(packetReponse.getData()),
									User.class);
//				    User user = (User) packetReponse.getData();
							if (user != null) {
								System.out.println(user);
								commonController.alertInfo(AlertType.CONFIRMATION, "Success!!!!",
										"Bạn đã đăng nhập thành công");
								userService.setUpLogin(user);

								if (user.getRole().equals("admin")) {
									MainController mainController = commonController
											.loaderToResource(event, "Chat/form_Chat").getController();
									mainController.setUser(user);
								} else {
									MainController mainController = commonController
											.loaderToResource(event, "Chat/form_Chat").getController();
									mainController.setUser(user);
								}
							} else {
								commonController.alertInfo(AlertType.WARNING, "Cảnh báo!!!!",
										"Không tìm thấy thông tin tài khoản!");
							}
						} else if (packetReponse.getType().equals("DUPLICATE_LOGIN")) {
							commonController.alertInfo(AlertType.INFORMATION, "Không thể đăng nhập vào tài khoản!!!!",
									"Tài khoản đã được đăng nhập bằng thiết bị khác!");
						}
					} else {
						commonController.alertInfo(AlertType.ERROR, "Cảnh báo!!!!",
								"Không nhận được phản hồi nào từ server!");
					}

					clean();
				} catch (InterruptedException e) {
//					socketClient.reConnectToServer();
					e.printStackTrace();
				}
			}

		} else {
			commonController.alertInfo(Alert.AlertType.WARNING, "Cảnh báo!", "Vui lòng nhập đầy đủ thông tin!");
			clean();
		}
	}

	void clean() {
		username.setText(null);
		password.setText(null);
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		try {
			socketClient = SocketClient.getInstance();
			
//			socketClient.setOnServerDisconnected(reason -> {
//				Platform.runLater(() -> {
//
//					commonController.alertInfo(AlertType.WARNING, "❌ Server disconnected", reason);
//
//					
//					commonController.alertConfirm("Kết nối lại SERVER", "Bạn có chắc muốn kết nối lại với Server hay không?", confirmed -> {
//					    if (confirmed) {
//					    	try {
//								socketClient.reConnectToServer();
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//					    } else {
//					        System.out.println("Người dùng hủy thao tác.");
//					    }
//					});								
//				});
//			});
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

}
