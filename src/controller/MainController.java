package controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import controller.render.FileRenderMessage;
import controller.render.MessageRender;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import model.ChatMessage;
import model.FileInfo;
import model.Packet;
import model.User;
import service.RedisUserService;
import service.UserService;
import util.RedisUtil;

public class MainController implements Initializable {

	private UserService userService = new UserService();
	private RedisUserService redisUserService = new RedisUserService(RedisUtil.getClient());
	private User user;
	private String activeSelect;
	private SocketClient socketClient;
	private CommonController commonController;

	private void setActiveSelect(String activeSelect) {
		this.activeSelect = activeSelect;
	}

	private String lastSenderId = null;

	@FXML
	private Circle avatarUser;

	@FXML
	private StackPane chat_all;

	@FXML
	private StackPane chat_group;

	@FXML
	private StackPane chat_message;

	@FXML
	private StackPane closeSearch;

	@FXML
	private StackPane clear;

	@FXML
	private AnchorPane container_chat;

	@FXML
	private Text lastMessage;

	@FXML
	private TextField messageText;

	@FXML
	private ScrollPane scrollMessage;

	@FXML
	private TextField searchTextFiled;

	@FXML
	private Text statusActive;

	@FXML
	private Text textUserOrGroup;

	@FXML
	private Text userName;

	@FXML
	private VBox vboxInScroll;

	public void setUser(User user) {
		this.user = user;

		userName.setText(user.getUsername());

		Image image = new Image(user.getAvatarUrl(), true);

		image.errorProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal) {
				System.out.println("❌ Lỗi tải ảnh: " + user.getAvatarUrl());
			}
		});

		image.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !image.isError()) {
				Platform.runLater(() -> {
					avatarUser.setFill(new ImagePattern(image));
				});
			}
		});

		// load message from mongoDB
		if (user != null) {
			Task<Void> loadTask = new Task<>() {
				@Override
				protected Void call() throws Exception {
					loadChatHistory(user.getIdHex(), "all");
					return null;
				}
			};

			new Thread(loadTask).start();
		}

	}

	@FXML
	void clearTextSearch(MouseEvent event) {
		searchTextFiled.setText(null);
	}

	@FXML
	void closeSearch(MouseEvent event) {

	}

	@FXML
	void clickChat(MouseEvent event) {
		setActiveSelect("message");
		assignActiveSelect(getActiveSelect());

	}

	@FXML
	void clickChatAll(MouseEvent event) {
		setActiveSelect("all");
		assignActiveSelect(getActiveSelect());
	}

	@FXML
	void clickChatGroup(MouseEvent event) {
		setActiveSelect("group");
		assignActiveSelect(getActiveSelect());

	}

	private void onSend(String messenger, String receiverId) throws IOException, InterruptedException {
		if (commonController.checkValidTextField(messageText)) {

			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSenderId(user.getIdHex());
			chatMessage.setReceiverId(receiverId);
			chatMessage.setContent(messenger);
			chatMessage.setType("text");
			chatMessage.setRead(false);
			chatMessage.setTimestamp(LocalDateTime.now());
//
//			out.println(gson.toJson(chatMessage));
//			out.flush();

			Packet packetRequest = new Packet();
			packetRequest.setType("MESSAGE");
			packetRequest.setData(chatMessage);

			if (socketClient.sendPacket(packetRequest)) {
				onSendAndReceiveMessenge(chatMessage, true);
			} else {
				commonController.alertInfo(AlertType.WARNING, "Establieshed to server is fail", "Can't send message");
			}

			messageText.clear();
		}
	}

	class styleDifferenceClass {
		private Pos position;
		private String styleCss;

		styleDifferenceClass(Pos position, String styleCss) {
			this.position = position;
			this.styleCss = styleCss;
		}
	}

	// tách ra MessageRender
	public void onSendAndReceiveMessenge(ChatMessage chatMessage, Boolean isSend) throws MalformedURLException {

		String messageContent = chatMessage.getContent();

		Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
		mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT, "-fx-text-fill: rgb(239,242,255);"
				+ "-fx-background-color: rgb(15,125,242);" + "-fx-background-radius: 20px;"));
		mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT,
				"-fx-text-fill: black;" + "-fx-background-color: rgb(233,233,235);" + "-fx-background-radius: 20px;"));

		// fetch from cache tang toc do
		User userSender = new User();
		if (chatMessage.getSenderId().equals("Server")) {
			userSender.setUsername("Tin nhắn hệ thống");
			userSender.setAvatarUrl("https://i.ibb.co/yBhhZRdB/images.jpg");
		} else {
			userSender.setUsername(redisUserService.getCachedUsername(chatMessage.getSenderId()));
			userSender.setAvatarUrl(redisUserService.getCachedAvatar(chatMessage.getSenderId()));
		}

		// Avatar hình tròn

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

		// Bong bóng tin nhắn
		Text text = new Text(messageContent);

		// dinh dang kieu tin nhan
		if (isSend) {
			text.setFill(Color.color(0.934, 0.945, 0.996));
		}
		TextFlow textFlow = new TextFlow(text);
		textFlow.setStyle(mapStyleMessenger.get(isSend).styleCss);
		textFlow.setPadding(new Insets(5, 10, 5, 10));
		textFlow.setMaxWidth(300);

		// HBox chứa avatar + tin nhắn
		HBox hBox = new HBox(10);
		hBox.setAlignment(mapStyleMessenger.get(isSend).position);
		hBox.setPadding(new Insets(5, 5, 5, 10));
		hBox.setStyle("-fx-cursor: text;");

		if (isSend) {
			hBox.getChildren().addAll(textFlow);
		} else {
			hBox.getChildren().addAll(avatar, textFlow); // avatar bên trái
		}

		// VBox chứa tên + HBox
		VBox messageBox = new VBox(2);

		if (isSend) {
			messageBox.getChildren().addAll(hBox);

//			messageBox.setOnMouseClicked(event -> {
//			    messageBox.setStyle("-fx-translate-y:10px;-fx-text-fill:orange");
//			    messageBox.
//			});
			vboxInScroll.getChildren().add(messageBox);
		} else {
			// Tên người gửi
			// nếu trùng người gửi thì ko hiển thị tên và avatar
			System.out.println("lastSender: " + lastSenderId);
			System.out.println("SenderID message: " + chatMessage.getSenderId());
			if (!chatMessage.getSenderId().equals(lastSenderId)) {
				Label nameLabel = new Label(userSender.getUsername());
				nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
				messageBox.getChildren().addAll(nameLabel, hBox);
			} else {
				hBox.setStyle("-fx-translate-x:40px");
				hBox.setSpacing(1.0);
				hBox.getChildren().remove(avatar); // ẩn avatar
				messageBox.getChildren().add(hBox); // không có tên
			}

//			Label nameLabel = new Label(userSender.getUsername());
//			nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
//
//			messageBox.getChildren().addAll(nameLabel, hBox);

			Platform.runLater(() -> vboxInScroll.getChildren().add(messageBox));
		}

		lastSenderId = chatMessage.getSenderId();
	}

	public void renderFileMessage(ChatMessage chatMessage, boolean isSend) throws IOException {
		FileInfo fileData = socketClient.gson.fromJson(chatMessage.getContent(), FileInfo.class);

		String fileName = fileData.getUrlUpload();
		String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
		boolean isImage = extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png");
		
		FXMLLoader loader = new FXMLLoader(getClass()
				.getResource((isImage) ? "/view/component/ShowImages.fxml" : "/view/component/FileBubble.fxml"));
		Node fileBubble = loader.load();

		Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
		mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT,
				"-fx-background-color: rgb(15,125,242); -fx-background-radius: 20px;"));
		mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT,
				"-fx-background-color: rgb(233,233,235); -fx-background-radius: 20px;"));

		FileRenderMessage controller = loader.getController();
		// truyền dữ liệu vào controller
		if (isImage) {
			controller.setImageInfo(fileData);
		} else {
			controller.setFileInfo(fileData);
		}

//	    Platform.runLater(() -> vboxInScroll.getChildren().add(fileBubble));

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

		Platform.runLater(() -> vboxInScroll.getChildren().add(messageBox));
		lastSenderId = chatMessage.getSenderId();

	}

	private void reset() {
		lastSenderId = null;
		vboxInScroll.getChildren().clear();
	}

	public void actionForMessageBox() {

	}

	@FXML
	void selecteUserOrGroup(MouseEvent event) {

	}

	@FXML
	void infoClick(MouseEvent event) {

	}

	@FXML
	void openSearch(MouseEvent event) {

	}

	@FXML
	void searchSubmit(KeyEvent event) {

	}

	@FXML
	void sendMessage(KeyEvent event) throws IOException, InterruptedException {
		if (event.getCode().toString().equals("ENTER")) {
			onSend(messageText.getText(), "all");
		}
	}

	public void renderLocalFileMessage(File file, Boolean isImage) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass()
				.getResource((isImage) ? "/view/component/ShowImages.fxml" : "/view/component/FileBubble.fxml"));

		Node fileBubble = loader.load();

		FileRenderMessage controller = loader.getController();
		// truyền dữ liệu vào controller
		if(isImage) {
			controller.setImagesLocal(file);
		}else {
			controller.setFileInfoLocal(file);
		}

		HBox hBox = new HBox(fileBubble);
		hBox.setAlignment(Pos.CENTER_RIGHT);
		hBox.setPadding(new Insets(5, 5, 5, 10));

		VBox messageBox = new VBox(hBox);
		vboxInScroll.getChildren().add(messageBox);
	}

	@FXML
	void openFileChoosen(MouseEvent event) throws IOException, InterruptedException {
		openFileOrImageChoosen(event, false);
	}

	@FXML
	void openImages(MouseEvent event) throws IOException, InterruptedException {
		openFileOrImageChoosen(event, true);
	}

	private void openFileOrImageChoosen(MouseEvent event, Boolean isImage) throws IOException, InterruptedException {
		String word = (isImage) ? "Ảnh" : "File";
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Chọn " + ((isImage) ? "ảnh" : "file") + " để gửi");

		// Tùy chọn định dạng file
		List<FileChooser.ExtensionFilter> filters = new ArrayList<>();

		if (isImage) {
		    filters.add(new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg"));
		} else {
		    filters.add(new FileChooser.ExtensionFilter("Tất cả file", "*.*"));
			filters.add(new FileChooser.ExtensionFilter("Tài liệu", "*.pdf", "*.docx", "*.txt"));
		}

		fileChooser.getExtensionFilters().addAll(filters);

		// Mở hộp thoại
		File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

		if (selectedFile != null) {
			long maxSize = 10 * 1024 * 1024; // 10MB

			if (selectedFile.length() > maxSize) {

				commonController.alertInfo(AlertType.WARNING, word + " quá lớn!!!!",
						"Vui lòng chọn " + word + " nhỏ hơn 10MB.");
				return;
			}

			System.out.println(word + " hợp lệ: " + selectedFile.getAbsolutePath());

			if (socketClient.checkRunningServer()) {
				socketClient.sendFile(selectedFile, user.getIdHex(), "all");

				renderLocalFileMessage(selectedFile,isImage);
			} else {
				commonController.alertInfo(AlertType.WARNING, "Establieshed to server is fail", "Can't send " + word);
				socketClient.reConnectToServer();
			}
		} else {
			commonController.alertInfo(AlertType.INFORMATION, "Không có " + word + " nào được chọn!!!!",
					"Vui lòng chọn " + word + ".");
		}
	}

	private void assignActiveSelect(String select) {
		if (select.equals("message")) {
			chat_message.getStyleClass().add("active");
			chat_all.getStyleClass().remove("active");
			chat_group.getStyleClass().remove("active");
		} else if (select.equals("all")) {
			chat_message.getStyleClass().remove("active");
			chat_all.getStyleClass().add("active");
			chat_group.getStyleClass().remove("active");
		} else {
			chat_message.getStyleClass().remove("active");
			chat_all.getStyleClass().remove("active");
			chat_group.getStyleClass().add("active");
		}
	}

	private String getActiveSelect() {
		return activeSelect;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// assign ban đầu ở broadcast
		textUserOrGroup.setText("Nhóm cộng đồng");
		setActiveSelect("all");

		assignActiveSelect(getActiveSelect());
		avatarUser.setVisible(false);

		clear.setVisible(false);

		searchTextFiled.textProperty().addListener((obs, oldText, newText) -> {
			if (newText == null || newText.isEmpty()) {
				clear.setVisible(false);
			} else {
				clear.setVisible(true);
			}
		});

		// always scroll to end
		vboxInScroll.heightProperty().addListener((obs, oldVal, newVal) -> {
			scrollMessage.setVvalue(1.0);
		});
		scrollMessage.setFitToWidth(true);
		commonController = new CommonController();

		try {
			socketClient = SocketClient.getInstance();

			socketClient.setMessageHandler(chatMessage -> {
				try {
					onSendAndReceiveMessenge(chatMessage, false);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					System.out.println("Can't receive message");
				}
			});

			socketClient.setFileMessageHandler(chatMessage -> {
				try {
					renderFileMessage(chatMessage, false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("File :" + chatMessage);
			});

			socketClient.setOnServerDisconnected(reason -> {
				Platform.runLater(() -> {

					commonController.alertInfo(AlertType.WARNING, "❌ Server disconnected", reason);

					commonController.alertConfirm("Mất kết nối với SERVER",
							"Bạn có chắc muốn đăng nhập lại với Server hay không?", confirmed -> {
								if (confirmed) {

									Platform.runLater(() -> {
										try {
											commonController.loaderToResource(container_chat,
													"LoginAndRegister/FormLoginAndRegister");
											socketClient.reConnectToServer();

										} catch (IOException e) {
											e.printStackTrace();
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									});

								} else {
									System.out.println("Người dùng hủy thao tác.");
								}
							});

				});
			});
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void loadChatHistory(String userA, String userB) throws IOException {
		List<ChatMessage> messages = socketClient.chatService.getMessagesBetween(userA, userB);

		System.out.println(messages);
		if (messages != null && !messages.isEmpty()) {
//			for (ChatMessage msg : messages) {
//				boolean isSend = msg.getSenderId().equals(user.getIdHex());
//				if (msg.getType().equalsIgnoreCase("file")) {
//					Platform.runLater(() -> {
//						try {
//							renderFileMessage(msg, isSend);
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					});
//				} else {					
//					Platform.runLater(() -> {
//						try {
//							onSendAndReceiveMessenge(msg, isSend);
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					});
//				}
//			}

			List<Node> rendered = new ArrayList<>();
			String lastSenderId = null;

			for (ChatMessage msg : messages) {
				boolean isSend = msg.getSenderId().equals(user.getIdHex());
				Node node = msg.getType().equalsIgnoreCase("file")
						? MessageRender.renderFileMessage(msg, isSend, lastSenderId, redisUserService, socketClient)
						: MessageRender.renderTextMessage(msg, isSend, lastSenderId, redisUserService);
				lastSenderId = msg.getSenderId();
				rendered.add(node);
			}

			Platform.runLater(() -> vboxInScroll.getChildren().addAll(rendered));
		}
	}
}
