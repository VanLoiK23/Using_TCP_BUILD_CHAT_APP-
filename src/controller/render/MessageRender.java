package controller.render;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import controller.ServerAndClientSocket.SocketClient;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import model.ChatMessage;
import model.FileInfo;
import model.User;
import service.RedisUserService;

public class MessageRender {

	static class styleDifferenceClass {
		private Pos position;
		private String styleCss;

		styleDifferenceClass(Pos position, String styleCss) {
			this.position = position;
			this.styleCss = styleCss;
		}
	}

	public static Node renderTextMessage(ChatMessage chatMessage, boolean isSend, String lastSenderId,
			RedisUserService redisUserService) throws MalformedURLException {
		String messageContent = chatMessage.getContent();

		Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
		mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT, "-fx-text-fill: rgb(239,242,255);"
				+ "-fx-background-color: rgb(15,125,242);" + "-fx-background-radius: 20px;"));
		mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT,
				"-fx-text-fill: black;" + "-fx-background-color: rgb(233,233,235);" + "-fx-background-radius: 20px;"));

		User userSender = new User();
		if (chatMessage.getSenderId().equals("Server")) {
			userSender.setUsername("Tin nhắn hệ thống");
			userSender.setAvatarUrl("https://i.ibb.co/yBhhZRdB/images.jpg");
		} else {
			userSender.setUsername(redisUserService.getCachedUsername(chatMessage.getSenderId()));
			userSender.setAvatarUrl(redisUserService.getCachedAvatar(chatMessage.getSenderId()));
		}

		Image image = new Image(userSender.getAvatarUrl(), true);
		ImageView avatar = new ImageView();
		avatar.setFitWidth(30);
		avatar.setFitHeight(30);
		avatar.setClip(new Circle(15, 15, 15));
		image.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !image.isError()) {
				avatar.setImage(image);
			}
		});

		Text text = new Text(messageContent);
		if (isSend)
			text.setFill(Color.color(0.934, 0.945, 0.996));

		TextFlow textFlow = new TextFlow(text);
		textFlow.setStyle(mapStyleMessenger.get(isSend).styleCss);
		textFlow.setPadding(new Insets(5, 10, 5, 10));
		textFlow.setMaxWidth(300);

		HBox hBox = new HBox(10);
		hBox.setAlignment(mapStyleMessenger.get(isSend).position);
		hBox.setPadding(new Insets(5, 5, 5, 10));
		hBox.setStyle("-fx-cursor: text;");

		if (isSend) {
			hBox.getChildren().add(textFlow);
		} else {
			hBox.getChildren().addAll(avatar, textFlow);
		}

		VBox messageBox = new VBox(2);
		if (isSend) {
			messageBox.getChildren().add(hBox);
		} else {
			if (!chatMessage.getSenderId().equals(lastSenderId)) {
				Label nameLabel = new Label(userSender.getUsername());
				nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
				messageBox.getChildren().addAll(nameLabel, hBox);
			} else {
				hBox.setStyle("-fx-translate-x:40px");
				hBox.setSpacing(1.0);
				hBox.getChildren().remove(avatar);
				messageBox.getChildren().add(hBox);
			}
		}

		return messageBox;
	}

	public static Node renderSystemNotice(String content) {
		Label noticeLabel = new Label(content);
		noticeLabel.setStyle("-fx-text-fill: gray;" + "-fx-font-style: italic;" + "-fx-font-size: 13px;"
				+ "-fx-background-color: transparent;");

		HBox noticeBox = new HBox(noticeLabel);
		noticeBox.setAlignment(Pos.CENTER);
		noticeBox.setPadding(new Insets(10, 0, 10, 0));

		return noticeBox;
	}

	public static Node renderFileMessage(ChatMessage chatMessage, boolean isSend, String lastSenderId,
			RedisUserService redisUserService, SocketClient socketClient) throws IOException {
		System.out.println("Content chat: "+chatMessage.getContent());
		
		FileInfo fileData = socketClient.gson.fromJson(chatMessage.getContent(), FileInfo.class);

		String fileName = fileData.getUrlUpload();
		String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
		boolean isImage = extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png");

		FXMLLoader loader = new FXMLLoader(MessageRender.class
				.getResource((isImage) ? "/view/component/ShowImages.fxml" : "/view/component/FileBubble.fxml"));
		Node fileBubble = loader.load();

		Map<Boolean, styleDifferenceClass> mapStyleMessenger = Map.of(true,
				new styleDifferenceClass(Pos.CENTER_RIGHT,
						"-fx-background-color: rgb(15,125,242); -fx-background-radius: 20px;"),
				false, new styleDifferenceClass(Pos.CENTER_LEFT,
						"-fx-background-color: rgb(233,233,235); -fx-background-radius: 20px;"));

		FileRenderMessage controller = loader.getController();
		if (isImage) {
			controller.setImageInfo(fileData);
		} else {
			controller.setFileInfo(fileData);
		}

		HBox hBox = new HBox(10);
		hBox.setAlignment(mapStyleMessenger.get(isSend).position);
		hBox.setPadding(new Insets(5, 5, 5, 10));

		if (!isSend) {
			ImageView avatar = new ImageView(
					new Image(redisUserService.getCachedAvatar(chatMessage.getSenderId()), true));
			avatar.setFitWidth(30);
			avatar.setFitHeight(30);
			avatar.setClip(new Circle(15, 15, 15));
			hBox.getChildren().addAll(avatar, fileBubble);
		} else {
			hBox.getChildren().add(fileBubble);
		}

		VBox messageBox = new VBox(2);
		if (!isSend && !chatMessage.getSenderId().equals(lastSenderId)) {
			Label nameLabel = new Label(redisUserService.getCachedUsername(chatMessage.getSenderId()));
			nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
			messageBox.getChildren().addAll(nameLabel, hBox);
		} else {
			messageBox.getChildren().add(hBox);
		}

		return messageBox;
	}

	public static Node renderFileLocal(File file, Boolean isImage) throws IOException {
		FXMLLoader loader = new FXMLLoader(MessageRender.class
				.getResource((isImage) ? "/view/component/ShowImages.fxml" : "/view/component/FileBubble.fxml"));
		Node fileBubble = loader.load();

		FileRenderMessage controller = loader.getController();
		// truyền dữ liệu vào controller
		if (isImage) {
			controller.setImagesLocal(file);
		} else {
			controller.setFileInfoLocal(file);
		}

		HBox hBox = new HBox(fileBubble);
		hBox.setAlignment(Pos.CENTER_RIGHT);
		hBox.setPadding(new Insets(5, 5, 5, 10));

		VBox messageBox = new VBox(hBox);
		
		return messageBox;
	}
}