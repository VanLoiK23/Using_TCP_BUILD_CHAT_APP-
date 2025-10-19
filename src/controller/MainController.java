package controller;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import controller.render.FileRenderMessage;
import controller.render.InfoGroup;
import controller.render.ListGroupToJoin;
import controller.render.ListUserController;
import controller.render.MessageRender;
import controller.render.NodeClientRenderClientSide;
import controller.render.NodeUserInListUser;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
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
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import model.ChatContext;
import model.ChatMessage;
import model.ConversationType;
import model.FileInfo;
import model.Group;
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

	private String receiverId = null;

	private String multicastIP = null;

	@FXML
	private Circle avatarGroup;

	@FXML
	private Circle avatarUser;

	@FXML
	private StackPane chat_all;

	@FXML
	private StackPane chat_group;

	@FXML
	private StackPane chat_message;

	@FXML
	private StackPane listGroup;

	@FXML
	private StackPane closeSearch;

	@FXML
	private StackPane clear;

	@FXML
	private AnchorPane container_chat;

	@FXML
	private TextField messageText;

	@FXML
	private ScrollPane scrollMessage;

	@FXML
	private ScrollPane scrollPaneListUser_Group;

	@FXML
	private VBox vboxInScrollPaneListUser_Group;

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

	private Map<String, VBox> chatBoxes = new HashMap<>(); // key = groupId/userId/community
	private ChatContext currentChatContext;

	public void setUser(User user) {
		this.user = user;

		userName.setText(user.getUsername());

		Image image = new Image(user.getAvatarUrl(), true);

		image.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !image.isError()) {
				avatarUser.setFill(new ImagePattern(image));
			}
		});

		// load message from mongoDB
		if (user != null) {
//			Task<Void> loadTask = new Task<>() {
//				@Override
//				protected Void call() throws Exception {
//					loadChatHistory(user.getIdHex(), "all");
//					return null;
//				}
//			};
//
//			new Thread(loadTask).start();
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
	void clickChat(MouseEvent event) throws IOException {
//		reset();
		vboxInScrollPaneListUser_Group.getChildren().clear();

		setActiveSelect("message");
		assignActiveSelect(getActiveSelect());

		loadPrivateHistory();
	}

	@FXML
	void clickChatAll(MouseEvent event) {
		vboxInScrollPaneListUser_Group.getChildren().clear();

		reset();

		setActiveSelect("all");
		assignActiveSelect(getActiveSelect());
		switchConversation(new ChatContext(ConversationType.COMMUNITY), "Nhóm cộng đồng",
				"https://i.ibb.co/bgHSQX6K/download.png");
	}

	@FXML
	void clickChatGroup(MouseEvent event) throws IOException {
//		reset();
		vboxInScrollPaneListUser_Group.getChildren().clear();

		setActiveSelect("group");
		assignActiveSelect(getActiveSelect());

		loadGroupHistory(true);

	}

	@FXML
	void clickListGroup(MouseEvent event) throws IOException {
		vboxInScrollPaneListUser_Group.getChildren().clear();

		setActiveSelect("listGroup");
		assignActiveSelect(getActiveSelect());

		loadGroupHistory(false);
	}

	@FXML
	void clickAddGroup(MouseEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/listClient.fxml"));

		Parent root = loader.load();

		ListUserController listUserController = loader.getController();
		listUserController.setUserCreate(user);

		Stage stage = new Stage();
		Scene scene = new Scene(root);
		stage.initStyle(StageStyle.UNDECORATED);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setScene(scene);
		stage.show();
	}

	@FXML
	void infoClick(MouseEvent event) throws IOException {
		if (currentChatContext.getType().equals(ConversationType.GROUP)) {
			if (currentChatContext.getTargetId() != null && !currentChatContext.getTargetId().isEmpty()) {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/InfoOfGroup.fxml"));

				Parent root = loader.load();

				InfoGroup infoGroup = loader.getController();
				infoGroup.setUp(socketClient.groupService.getGroupById(currentChatContext.getTargetId()));

				Scene scene = new Scene(root);
				Stage stage = new Stage();
				stage.setTitle("Xem thông tin nhóm");
				stage.setScene(scene);
				stage.initModality(Modality.APPLICATION_MODAL);
				stage.show();
			}
		}
	}

	private List<User> allUsers = new ArrayList<>();
	private List<User> filteredUsers = new ArrayList<>();

	@FXML
	void openSearch(MouseEvent event) throws IOException {

		vboxInScrollPaneListUser_Group.getChildren().clear();

//		setActiveSelect("message");
//		assignActiveSelect(getActiveSelect());

		allUsers = userService.getAllUser();

		allUsers.remove(user);

		renderUserList(allUsers);
	}

	private void performSearch(String keyword) throws IOException {
		if (keyword == null || keyword.trim().isEmpty()) {
			filteredUsers = new ArrayList<>(allUsers);
		} else {
			String lowerKeyword = keyword.toLowerCase();
			filteredUsers = allUsers.stream().filter(u -> u.getUsername().toLowerCase().contains(lowerKeyword)
					|| u.getEmail().toLowerCase().contains(lowerKeyword)).collect(Collectors.toList());
		}

		renderUserList(filteredUsers);
	}

	private void renderUserList(List<User> userList) throws IOException {
		List<Node> render = new ArrayList<Node>();

		for (User us : userList) {

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/nodeUserInListUser.fxml"));

			Node node = loader.load();

			NodeUserInListUser controller = loader.getController();
			controller.setUpForDisplay(us);

			node.setOnMouseClicked(e -> {
				switchConversation(new ChatContext(ConversationType.PRIVATE, us.getIdHex()), us.getUsername(),
						us.getAvatarUrl());				
				
			});

			render.add(node);
		}

		Platform.runLater(() -> {
			vboxInScrollPaneListUser_Group.getChildren().addAll(render);
		});
	}

//	@FXML
//	void searchSubmit(KeyEvent event) {
//
//	}

	private void onSend(String messenger, String chatType, String groupId, String receiverId, String multiCastIP)
			throws IOException, InterruptedException {
		if (commonController.checkValidTextField(messageText)) {

			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSenderId(user.getIdHex());
			chatMessage.setReceiverId(receiverId);
			chatMessage.setGroupId(groupId);
			chatMessage.setChatType(chatType);
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

			if (chatType.equalsIgnoreCase("group")) {
				socketClient.sendMessageGroup(packetRequest, InetAddress.getByName(multiCastIP));

				onSendAndReceiveMessage(chatMessage, true);
			} else if (chatType.equalsIgnoreCase("private")) {
				packetRequest.setType("MESSAGE_PRIVATE");

				if (socketClient.sendPacket(packetRequest)) {
					onSendAndReceiveMessage(chatMessage, true);
				}
			} else {
				if (socketClient.sendPacket(packetRequest)) {
					onSendAndReceiveMessage(chatMessage, true);
				} else {
					commonController.alertInfo(AlertType.WARNING, "Establieshed to server is fail",
							"Can't send message");
				}
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

	public void onSendAndReceiveMessage(ChatMessage chatMessage, boolean isSend) throws MalformedURLException {
		String key;
		if (chatMessage.getChatType().equals("community")) {
			key = "community";
		} else if (chatMessage.getChatType().equals("group")) {
			key = chatMessage.getGroupId();
		} else {
			key = chatMessage.getReceiverId().equals(user.getIdHex()) ? chatMessage.getSenderId()
					: chatMessage.getReceiverId();
		}

		VBox vbox = chatBoxes.computeIfAbsent(key, k -> new VBox(5));
		Node messageNode = MessageRender.renderTextMessage(chatMessage, isSend, lastSenderId, redisUserService);

		Platform.runLater(() -> vbox.getChildren().add(messageNode));
	}

	public void switchConversation(ChatContext context, String nameGroup_User, String imgGr) {
		textUserOrGroup.setText(nameGroup_User);
		Image imageGroup = new Image(imgGr, true);

		imageGroup.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !imageGroup.isError()) {
				avatarGroup.setFill(new ImagePattern(imageGroup));
			}
		});

		this.currentChatContext = context;
		VBox vbox = chatBoxes.computeIfAbsent(getKeyFromContext(context), k -> new VBox(5));

		// ✅ Dùng VBox thật, không dùng vbox.getChildren()
		vboxInScroll.getChildren().setAll(vbox);
	}

	private String getKeyFromContext(ChatContext context) {
		if (context.getType() == ConversationType.COMMUNITY)
			return "community";
		if (context.getType() == ConversationType.GROUP)
			return context.getTargetId();
		return context.getTargetId();
	}

	// tách ra MessageRender
//	public void onSendAndReceiveMessenge(ChatMessage chatMessage, Boolean isSend) throws MalformedURLException {
//
//		String messageContent = chatMessage.getContent();
//
//		Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
//		mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT, "-fx-text-fill: rgb(239,242,255);"
//				+ "-fx-background-color: rgb(15,125,242);" + "-fx-background-radius: 20px;"));
//		mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT,
//				"-fx-text-fill: black;" + "-fx-background-color: rgb(233,233,235);" + "-fx-background-radius: 20px;"));
//
//		// fetch from cache tang toc do
//		User userSender = new User();
//		if (chatMessage.getSenderId().equals("Server")) {
//			userSender.setUsername("Tin nhắn hệ thống");
//			userSender.setAvatarUrl("https://i.ibb.co/yBhhZRdB/images.jpg");
//		} else {
//			userSender.setUsername(redisUserService.getCachedUsername(chatMessage.getSenderId()));
//			userSender.setAvatarUrl(redisUserService.getCachedAvatar(chatMessage.getSenderId()));
//		}
//
//		// Avatar hình tròn
//
//		Image image = new Image(userSender.getAvatarUrl(), true);
//
//		ImageView avatar = new ImageView();
//		avatar.setFitWidth(30);
//		avatar.setFitHeight(30);
//		avatar.setClip(new Circle(15, 15, 15));
//
//		image.progressProperty().addListener((obs, oldVal, newVal) -> {
//			if (newVal.doubleValue() == 1.0 && !image.isError()) {
//				avatar.setImage(image);
//			}
//		});
//
//		// Bong bóng tin nhắn
//		Text text = new Text(messageContent);
//
//		// dinh dang kieu tin nhan
//		if (isSend) {
//			text.setFill(Color.color(0.934, 0.945, 0.996));
//		}
//		TextFlow textFlow = new TextFlow(text);
//		textFlow.setStyle(mapStyleMessenger.get(isSend).styleCss);
//		textFlow.setPadding(new Insets(5, 10, 5, 10));
//		textFlow.setMaxWidth(300);
//
//		// HBox chứa avatar + tin nhắn
//		HBox hBox = new HBox(10);
//		hBox.setAlignment(mapStyleMessenger.get(isSend).position);
//		hBox.setPadding(new Insets(5, 5, 5, 10));
//		hBox.setStyle("-fx-cursor: text;");
//
//		if (isSend) {
//			hBox.getChildren().addAll(textFlow);
//		} else {
//			hBox.getChildren().addAll(avatar, textFlow); // avatar bên trái
//		}
//
//		// VBox chứa tên + HBox
//		VBox messageBox = new VBox(2);
//
//		if (isSend) {
//			messageBox.getChildren().addAll(hBox);
//
////			messageBox.setOnMouseClicked(event -> {
////			    messageBox.setStyle("-fx-translate-y:10px;-fx-text-fill:orange");
////			    messageBox.
////			});
//			vboxInScroll.getChildren().add(messageBox);
//		} else {
//			// Tên người gửi
//			// nếu trùng người gửi thì ko hiển thị tên và avatar
//			System.out.println("lastSender: " + lastSenderId);
//			System.out.println("SenderID message: " + chatMessage.getSenderId());
//			if (!chatMessage.getSenderId().equals(lastSenderId)) {
//				Label nameLabel = new Label(userSender.getUsername());
//				nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
//				messageBox.getChildren().addAll(nameLabel, hBox);
//			} else {
//				hBox.setStyle("-fx-translate-x:40px");
//				hBox.setSpacing(1.0);
//				hBox.getChildren().remove(avatar); // ẩn avatar
//				messageBox.getChildren().add(hBox); // không có tên
//			}
//
////			Label nameLabel = new Label(userSender.getUsername());
////			nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
////
////			messageBox.getChildren().addAll(nameLabel, hBox);
//
//			Platform.runLater(() -> vboxInScroll.getChildren().add(messageBox));
//		}
//
//		lastSenderId = chatMessage.getSenderId();
//	}

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
		receiverId = null;
		multicastIP = null;
	}

	@FXML
	void sendMessage(KeyEvent event) throws IOException, InterruptedException {
		if (event.getCode().toString().equals("ENTER")) {
			switch (currentChatContext.getType()) {
			case COMMUNITY: {
				onSend(messageText.getText(), "community", null, null, null);
				break;
			}
			case GROUP: {
				onSend(messageText.getText(), "group", currentChatContext.getTargetId(), null, multicastIP);
				break;
			}
			case PRIVATE: {
				onSend(messageText.getText(), "private", null, currentChatContext.getTargetId(), null);
				break;
			}
			default:
				throw new IllegalArgumentException("Unexpected value: ");
			}
		}
	}

	public void renderLocalFileMessage(File file, Boolean isImage) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass()
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

				renderLocalFileMessage(selectedFile, isImage);
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
			listGroup.getStyleClass().remove("active");
		} else if (select.equals("all")) {
			chat_message.getStyleClass().remove("active");
			chat_all.getStyleClass().add("active");
			chat_group.getStyleClass().remove("active");
			listGroup.getStyleClass().remove("active");
		} else if (select.equals("listGroup")) {
			listGroup.getStyleClass().add("active");
			chat_all.getStyleClass().remove("active");
			chat_group.getStyleClass().remove("active");
			chat_message.getStyleClass().remove("active");
		} else {
			chat_message.getStyleClass().remove("active");
			chat_all.getStyleClass().remove("active");
			chat_group.getStyleClass().add("active");
			listGroup.getStyleClass().remove("active");
		}
	}

	private String getActiveSelect() {
		return activeSelect;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// assign ban đầu ở broadcast
		setActiveSelect("all");
		assignActiveSelect(getActiveSelect());
		switchConversation(new ChatContext(ConversationType.COMMUNITY), "Nhóm cộng đồng",
				"https://i.ibb.co/bgHSQX6K/download.png");

		clear.setVisible(false);

		searchTextFiled.textProperty().addListener((obs, oldText, newText) -> {
			if (newText == null || newText.isEmpty()) {
				clear.setVisible(false);
			} else {
				clear.setVisible(true);
			}

			try {
				performSearch(newText);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
					if (!chatMessage.getSenderId().equals(user.getIdHex())) {
						onSendAndReceiveMessage(chatMessage, false);
					}
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

									Platform.runLater(() -> {
										try {
											commonController.loaderToResource(container_chat,
													"LoginAndRegister/FormLoginAndRegister");
//											socketClient.reConnectToServer();

										} catch (IOException e) {
											e.printStackTrace();
										}
									});
								}
							});

				});
			});
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void loadChatHistory(String userA, String userB) throws IOException {
		List<ChatMessage> messages = socketClient.chatService.getMessagesBetween(userA, userB);

		System.out.println(messages);
		if (messages != null && !messages.isEmpty()) {

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

	private void loadGroupHistory(Boolean isSelectMyGroup) throws IOException {
		List<Group> groups = socketClient.groupService.getGroupOfUser(user.getIdHex());

		List<Group> allGroups = new ArrayList<Group>();

		if (!isSelectMyGroup) {
			allGroups = socketClient.groupService.getAllGroups();

			groups = allGroups.stream().filter(group -> !group.getMembers().contains(user.getIdHex()))
					.collect(Collectors.toList());
		}

		if (groups != null && !groups.isEmpty()) {

			List<Node> rendered = new ArrayList<>();

			for (Group group : groups) {
				final Group currentGroup = group;

				FXMLLoader loader = new FXMLLoader(
						getClass().getResource((isSelectMyGroup) ? "/view/component/clientNodeClientSide.fxml"
								: "/view/component/listGroupToJoin.fxml"));

				Node node = loader.load();

				if (isSelectMyGroup) {
					socketClient.joinGroup(InetAddress.getByName(currentGroup.getMulticastIP()));

					NodeClientRenderClientSide nodeController = loader.getController();

					nodeController.setOnClick(groupSelect -> {
						if (groupSelect != null) {
							reset();

							switchConversation(new ChatContext(ConversationType.GROUP, groupSelect.getIdHex()),
									groupSelect.getGroupName(), groupSelect.getImageGroup());

							multicastIP = groupSelect.getMulticastIP();

						}
					});

					nodeController.setUp(currentGroup, null,
							socketClient.redisMessageService.getLatestMessage(currentGroup.getIdHex()));

					// adjusted group

					ContextMenu menu = new ContextMenu();
					BooleanProperty isMuteGroup = new SimpleBooleanProperty(
							socketClient.isMuteGroup(InetAddress.getByName(currentGroup.getMulticastIP())));

					MenuItem muteItem = new MenuItem();
					muteItem.textProperty()
							.bind(Bindings.when(isMuteGroup).then("Nhận tin nhắn").otherwise("Không nhận tin nhắn"));
					MenuItem leaveItem = new MenuItem("Rời nhóm");

					muteItem.setOnAction(e -> {
						try {
							InetAddress groupAddr = InetAddress.getByName(currentGroup.getMulticastIP());

							if (!isMuteGroup.get()) {
								// isActive->isDisable
								socketClient.MuteGroup(groupAddr, false);
								isMuteGroup.set(true);
								Platform.runLater(() -> commonController.alertInfo(AlertType.INFORMATION, "Thành công",
										"Đã tắt nhận tin nhắn từ nhóm!"));
							} else {
								socketClient.joinGroup(groupAddr);
								isMuteGroup.set(false);
								Platform.runLater(() -> commonController.alertInfo(AlertType.INFORMATION, "Thành công",
										"Đã bật lại nhận tin nhắn từ nhóm!"));
							}

						} catch (Exception ex) {
							ex.printStackTrace();
						}
					});
					leaveItem.setOnAction(e -> {
						commonController.alertConfirm("Rời nhóm?",
								"Bạn có muốn rời khỏi nhóm " + currentGroup.getGroupName() + " ?", confirmed -> {
									if (confirmed) {

										Packet packetRequest = new Packet();
										packetRequest.setType("LEAVE_GROUP");
										packetRequest.setData(currentGroup);

										try {
											System.out.println("Kết quả gửi requet "
													+ socketClient.getInstance().sendPacket(packetRequest));
										} catch (IOException | InterruptedException e1) {
											e1.printStackTrace();
											return;
										}

										// Chờ phản hồi trong luồng riêng
										new Thread(() -> {
											try {
												Packet packetResponse = socketClient.getInstance().responseLeftGRQueue
														.poll(5, TimeUnit.SECONDS);

												if (packetResponse != null) {
													if (packetResponse.getData() instanceof Boolean) {
														if ((boolean) packetResponse.getData()) {
															Platform.runLater(() -> commonController.alertInfo(
																	AlertType.INFORMATION, "Successful!",
																	"Rời nhóm thành công!"));
															
															Platform.runLater(() -> {
																FadeTransition fade = new FadeTransition(
																		Duration.millis(300), node);
																fade.setFromValue(1.0);
																fade.setToValue(0.0);
																fade.setOnFinished(event -> {
																	vboxInScrollPaneListUser_Group.getChildren()
																			.remove(node);
																});
																fade.play();
															});

															try {
																socketClient.MuteGroup(InetAddress.getByName(
																		currentGroup.getMulticastIP()), true);
															} catch (UnknownHostException e1) {
																// TODO Auto-generated catch block
																e1.printStackTrace();
															} catch (IOException e1) {
																// TODO Auto-generated catch block
																e1.printStackTrace();
															}
														}
													}
												} else {
													Platform.runLater(() -> commonController.alertInfo(AlertType.ERROR,
															"Cảnh báo!!!!", "Không nhận được phản hồi nào từ server!"));
												}

											} catch (InterruptedException e1) {
												e1.printStackTrace();
											}
										}).start();

									} else {
										System.out.println("Người dùng hủy thao tác.");
									}
								});

					});

					menu.getItems().addAll(muteItem, leaveItem);

					node.setOnMouseEntered(e -> {
						if (!menu.isShowing()) {
//							menu.show(node, e.getScreenX(), e.getScreenY());

							menu.show(node, Side.RIGHT, 0, 0);

						}
					});

					node.setOnMouseExited(e -> {
						PauseTransition delay = new PauseTransition(Duration.millis(700));
						delay.setOnFinished(ev -> {
							if (!menu.isShowing())
								return;
							menu.hide();
						});
						delay.play();
					});

				} else {
					ListGroupToJoin nodeController = loader.getController();

					nodeController.setOnClick(groupSelect -> {
						if (groupSelect != null) {
							commonController.alertConfirm("Tham gia vào nhóm?",
									"Bạn có muốn tham gia vào nhóm " + groupSelect.getGroupName() + " ?", confirmed -> {
										if (confirmed) {
											Packet packetRequest = new Packet();
											packetRequest.setType("JOIN_GROUP");
											packetRequest.setData(groupSelect);

											try {
												System.out.println("Kết quả gửi requet "
														+ socketClient.getInstance().sendPacket(packetRequest));
											} catch (IOException | InterruptedException e) {
												e.printStackTrace();
												return;
											}

											// Chờ phản hồi trong luồng riêng
											new Thread(() -> {
												try {
													Packet packetResponse = socketClient
															.getInstance().responseJoinGRQueue
															.poll(5, TimeUnit.SECONDS);

													if (packetResponse != null) {
														if (packetResponse.getData() instanceof Boolean) {
															if ((boolean) packetResponse.getData()) {
																Platform.runLater(() -> commonController.alertInfo(
																		AlertType.INFORMATION, "Successful!",
																		"Join nhóm thành công!"));

																Platform.runLater(() -> {
																	FadeTransition fade = new FadeTransition(
																			Duration.millis(300), node);
																	fade.setFromValue(1.0);
																	fade.setToValue(0.0);
																	fade.setOnFinished(event -> {
																		vboxInScrollPaneListUser_Group.getChildren()
																				.remove(node);
																	});
																	fade.play();
																});

																socketClient.joinGroup(InetAddress
																		.getByName(groupSelect.getMulticastIP()));
															}
														}
													} else {
														Platform.runLater(() -> commonController.alertInfo(
																AlertType.ERROR, "Cảnh báo!!!!",
																"Không nhận được phản hồi nào từ server!"));
													}

												} catch (InterruptedException e) {
													e.printStackTrace();
												} catch (UnknownHostException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												} catch (IOException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
											}).start();

										} else {
											System.out.println("Người dùng hủy thao tác.");
										}
									});
						}
					});

					nodeController.setUp(currentGroup);
				}

				rendered.add(node);
			}

			Platform.runLater(() -> vboxInScrollPaneListUser_Group.getChildren().addAll(rendered));
		}
	}

	private void loadPrivateHistory() throws IOException {
		allUsers = userService.getAllUser();
		allUsers.remove(user);

		List<User> usersSendOrReceiveMessage = new ArrayList<User>();

		for (User us : allUsers) {
			List<ChatMessage> messages = socketClient.chatService.getMessagesBetween(user.getIdHex(), us.getIdHex());

			if (messages != null && !messages.isEmpty()) {

				// load message
//				List<Node> rendered = new ArrayList<>();
//				String lastSenderId = null;
//
//				for (ChatMessage msg : messages) {
//					boolean isSend = msg.getSenderId().equals(user.getIdHex());
//					Node node = msg.getType().equalsIgnoreCase("file")
//							? MessageRender.renderFileMessage(msg, isSend, lastSenderId, redisUserService, socketClient)
//							: MessageRender.renderTextMessage(msg, isSend, lastSenderId, redisUserService);
//					lastSenderId = msg.getSenderId();
//					rendered.add(node);
//				}
//
//				Platform.runLater(() -> vboxInScroll.getChildren().addAll(rendered));

				usersSendOrReceiveMessage.add(us);
			}
		}

		if (usersSendOrReceiveMessage != null && !usersSendOrReceiveMessage.isEmpty()) {
			List<Node> renderList=new ArrayList<Node>();
			for (User us : usersSendOrReceiveMessage) {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/clientNodeClientSide.fxml"));

				Node node = loader.load();

				NodeClientRenderClientSide nodeClientRenderClientSide = loader.getController();

				nodeClientRenderClientSide.setUp(null, us,
						socketClient.redisMessageService.getLatestMessage(us.getIdHex()));

				
				nodeClientRenderClientSide.setOnClickPrivate(userSelect -> {
					if (userSelect != null) {
						reset();
						
						switchConversation(new ChatContext(ConversationType.PRIVATE, userSelect.getIdHex()), userSelect.getUsername(),
								userSelect.getAvatarUrl());
						
					}
				});

				renderList.add(node);
			}
			
			Platform.runLater(() -> vboxInScrollPaneListUser_Group.getChildren().addAll(renderList));
		}

	}
}
