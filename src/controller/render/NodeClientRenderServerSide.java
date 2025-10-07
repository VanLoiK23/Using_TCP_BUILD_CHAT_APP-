package controller.render;

import controller.ServerAndClientSocket.SocketServer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import model.User;
import service.RedisUserService;
import service.UserService;
import util.RedisUtil;

public class NodeClientRenderServerSide {
	private RedisUserService redisUserService = new RedisUserService(RedisUtil.getClient());

	private UserService userService = new UserService();

	@FXML
	private Label IPClient;

	@FXML
	private Circle avatar;

	@FXML
	private Label nameClient;

	private SocketServer client;

	@FXML
	void unConnectThisClient(MouseEvent event) {
		if (client.getUserID() != null) {
			SocketServer.unConnectClient(client.getUserID());
		}
	}

	public void setClient(SocketServer client) {
		if (client != null && client.getUserID() != null) {
			this.client = client;

			String urlAvatar = redisUserService.getCachedAvatar(client.getUserID());
			String userName = redisUserService.getCachedUsername(client.getUserID());

			//chưa được lưu trong redis
			if (urlAvatar == null) {
				User user = userService.getUserById(client.getUserID());

				urlAvatar = user.getAvatarUrl();
				userName = user.getUsername();
			}

			System.out.println("Client id :" + client.getUserID());

			nameClient.setText(userName);
			IPClient.setText(client.getClientIP());

			Image image = new Image(urlAvatar, true);

			image.progressProperty().addListener((obs, oldVal, newVal) -> {
				if (newVal.doubleValue() == 1.0 && !image.isError()) {
					avatar.setFill(new ImagePattern(image));
				}
			});
		}
	}

}
