package controller.render;

import controller.ServerAndClientSocket.SocketServer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import service.RedisUserService;
import util.RedisUtil;

public class NodeClientRenderServerSide {
	private RedisUserService redisUserService = new RedisUserService(RedisUtil.getClient());

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
