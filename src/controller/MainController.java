package controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import controller.render.FileRenderMessage;
import javafx.application.Platform;
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

		image.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !image.isError()) {
				avatarUser.setFill(new ImagePattern(image));
			}
		});

		// load message from mongoDB
		try {
			loadChatHistory(user.getIdHex(), "all");
		} catch (IOException e) {
			e.printStackTrace();
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

	private void onSend(String messenger, String receiverId) throws IOException {
		if (messageText.getText() != null && !messageText.getText().isEmpty()) {

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

			socketClient.sendPacket(packetRequest);

			onSendAndReceiveMessenge(chatMessage, true);

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
		userSender.setUsername(redisUserService.getCachedUsername(chatMessage.getSenderId()));
		userSender.setAvatarUrl(redisUserService.getCachedAvatar(chatMessage.getSenderId()));

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

//	public void renderFileMessage(Packet packet) {
//	    FileLinkPacket fileData = gson.fromJson(gson.toJson(packet.getData()), FileLinkPacket.class);
//
//	    String filename = fileData.getFilename();
//	    String url = fileData.getDownloadUrl();
//
//	    // Tạo UI giống Messenger
//	    JLabel fileLabel = new JLabel("📎 " + filename);
//	    JButton downloadButton = new JButton("Tải xuống");
//
//	    downloadButton.addActionListener(e -> {
//	        try {
//	            Desktop.getDesktop().browse(new URI(url));
//	        } catch (Exception ex) {
//	            ex.printStackTrace();
//	        }
//	    });
//
//	    JPanel filePanel = new JPanel();
//	    filePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
//	    filePanel.add(fileLabel);
//	    filePanel.add(downloadButton);
//
//	    chatPanel.add(filePanel); // Thêm vào khung chat
//	    chatPanel.revalidate();
//	    chatPanel.repaint();
//	}

//	public void renderFileMessage(ChatMessage chatMessage, boolean isSend) {
//	    FileInfo fileData = socketClient.gson.fromJson(chatMessage.getContent(), FileInfo.class);
//	    String filename = fileData.getFileName();
//	    String url = fileData.getUrlUpload();
//
//	    Label fileLabel = new Label("📎 " + filename);
//	    fileLabel.setStyle("-fx-font-weight: bold;");
//	    
//	    ProgressIndicator circleProgress = new ProgressIndicator(0);
//	    circleProgress.setPrefSize(50, 50);
//	    Label percentLabel = new Label("0%");
//	    percentLabel.setStyle("-fx-font-weight: bold;");
//	    
//	    StackPane progressCircle = new StackPane(circleProgress, percentLabel);
//	    progressCircle.setVisible(false); // ẩn ban đầu
//
//
//	    Button downloadButton = new Button("Tải xuống");
//	    downloadButton.setStyle("-fx-background-color: transparent; -fx-text-fill: blue; -fx-underline: true;");
//	    downloadButton.setOnAction(e -> {
//	        DirectoryChooser chooser = new DirectoryChooser();
//	        chooser.setTitle("Chọn thư mục lưu file");
//	        File folder = chooser.showDialog(vboxInScroll.getScene().getWindow());
//
//	        if (folder != null) {
//	            progressCircle.setVisible(true);
//	            percentLabel.setText("0%");
//
//	            Task<Void> downloadTask = new Task<>() {
//	                @Override
//	                protected Void call() throws Exception {
//	                    URL website = new URL(url);
//	                    URLConnection connection = website.openConnection();
//	                    int fileSize = connection.getContentLength();
//
//	                    try (InputStream in = website.openStream();
//	                         FileOutputStream fos = new FileOutputStream(new File(folder, filename))) {
//
//	                        byte[] buffer = new byte[4096];
//	                        int bytesRead;
//	                        int totalRead = 0;
//
//	                        while ((bytesRead = in.read(buffer)) != -1) {
//	                            fos.write(buffer, 0, bytesRead);
//	                            totalRead += bytesRead;
//	                            double progress = (double) totalRead / fileSize;
//	                            updateProgress(progress, 1);
//	                        }
//	                    }
//
//	                    return null;
//	                }
//	            };
//
//	            circleProgress.progressProperty().bind(downloadTask.progressProperty());
//
//	            downloadTask.progressProperty().addListener((obs, oldVal, newVal) -> {
//	                int percent = (int) Math.round(newVal.doubleValue() * 100);
//	                percentLabel.setText(percent + "%");
//	            });
//
//	            downloadTask.setOnSucceeded(ev -> {
//	                percentLabel.setText("✅");
//	                try {
//	                    Desktop.getDesktop().open(new File(folder, filename));
//	                } catch (IOException ex) {
//	                    ex.printStackTrace();
//	                }
//	            });
//
//	            downloadTask.setOnFailed(ev -> percentLabel.setText("❌"));
//
//	            new Thread(downloadTask).start();
//	        }
//	    });
//
//	    VBox fileBox = new VBox(5, fileLabel, downloadButton, progressCircle);
//	    fileBox.setPadding(new Insets(5, 10, 5, 10));
//	    fileBox.setMaxWidth(300);
//
//	    Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
//	    mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT, "-fx-background-color: rgb(15,125,242); -fx-background-radius: 20px;"));
//	    mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT, "-fx-background-color: rgb(233,233,235); -fx-background-radius: 20px;"));
//
//	    fileBox.setStyle(mapStyleMessenger.get(isSend).styleCss);
//
//	    HBox hBox = new HBox(10);
//	    hBox.setAlignment(mapStyleMessenger.get(isSend).position);
//	    hBox.setPadding(new Insets(5, 5, 5, 10));
//
//	    if (!isSend) {
//	        ImageView avatar = new ImageView(new Image(redisUserService.getCachedAvatar(chatMessage.getSenderId()), true));
//	        avatar.setFitWidth(30);
//	        avatar.setFitHeight(30);
//	        avatar.setClip(new Circle(15, 15, 15));
//	        hBox.getChildren().addAll(avatar, fileBox);
//	    } else {
//	        hBox.getChildren().add(fileBox);
//	    }
//
//	    VBox messageBox = new VBox(2);
//	    if (!isSend && !chatMessage.getSenderId().equals(lastSenderId)) {
//	        Label nameLabel = new Label(redisUserService.getCachedUsername(chatMessage.getSenderId()));
//	        nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
//	        messageBox.getChildren().addAll(nameLabel, hBox);
//	    } else {
//	        messageBox.getChildren().add(hBox);
//	    }
//
//	    Platform.runLater(() -> vboxInScroll.getChildren().add(messageBox));
//	    lastSenderId = chatMessage.getSenderId();
//	}

	public void renderFileMessage(ChatMessage chatMessage, boolean isSend) throws IOException {
		FileInfo fileData = socketClient.gson.fromJson(chatMessage.getContent(), FileInfo.class);

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/FileBubble.fxml"));
		Node fileBubble = loader.load();

		Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
		mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT,
				"-fx-background-color: rgb(15,125,242); -fx-background-radius: 20px;"));
		mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT,
				"-fx-background-color: rgb(233,233,235); -fx-background-radius: 20px;"));

		FileRenderMessage controller = loader.getController();
		controller.setFileInfo(fileData); // truyền dữ liệu vào controller

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
	void sendMessage(KeyEvent event) throws IOException {
		if (event.getCode().toString().equals("ENTER")) {
			onSend(messageText.getText(), "all");
		}
	}

	public void renderLocalFileMessage(File file) throws IOException {
//	    String filename = file.getName();
//
//	    Label fileLabel = new Label("📎 " + filename);
//	    fileLabel.setStyle("-fx-font-weight: bold;");
//
//	    Button openButton = new Button("Mở file");
//	    openButton.setStyle("-fx-background-color: transparent; -fx-text-fill: blue; -fx-underline: true;");
//	    openButton.setOnAction(e -> {
//	        try {
//	            Desktop.getDesktop().open(file); // Mở file bằng app mặc định
//	        } catch (IOException ex) {
//	            ex.printStackTrace();
//	        }
//	    });
//
//	    VBox fileBox = new VBox(5, fileLabel, openButton);
//	    fileBox.setPadding(new Insets(5, 10, 5, 10));
//	    fileBox.setMaxWidth(300);
//	    fileBox.setStyle("-fx-background-color: rgb(15,125,242); -fx-background-radius: 20px;");
//
//	    HBox hBox = new HBox(fileBox);
//	    hBox.setAlignment(Pos.CENTER_RIGHT);
//	    hBox.setPadding(new Insets(5, 5, 5, 10));
//
//	    VBox messageBox = new VBox(hBox);
//	    vboxInScroll.getChildren().add(messageBox);

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/FileBubble.fxml"));
		Node fileBubble = loader.load();

		FileRenderMessage controller = loader.getController();
		controller.setFileInfoLocal(file); // truyền dữ liệu vào controller

		HBox hBox = new HBox(fileBubble);
		hBox.setAlignment(Pos.CENTER_RIGHT);
		hBox.setPadding(new Insets(5, 5, 5, 10));

		VBox messageBox = new VBox(hBox);
		vboxInScroll.getChildren().add(messageBox);
	}

	@FXML
	void openFileChoosen(MouseEvent event) throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Chọn file để gửi");

		// Tùy chọn định dạng file
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Tất cả file", "*.*"),
				new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg"),
				new FileChooser.ExtensionFilter("Tài liệu", "*.pdf", "*.docx", "*.txt"));

		// Mở hộp thoại
		File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

		if (selectedFile != null) {
			long maxSize = 10 * 1024 * 1024; // 10MB

			if (selectedFile.length() > maxSize) {
				commonController.alertInfo(AlertType.WARNING, "File quá lớn!!!!", "Vui lòng chọn file nhỏ hơn 10MB.");
				return;
			}

			System.out.println("File hợp lệ: " + selectedFile.getAbsolutePath());

			renderLocalFileMessage(selectedFile);

			socketClient.sendFile(selectedFile, user.getIdHex(), "all");

		} else {
			commonController.alertInfo(AlertType.INFORMATION, "Không có file nào được chọn!!!!", "Vui lòng chọn file.");
			System.out.println("Không có file nào được chọn.");
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

	}

	public void loadChatHistory(String userA, String userB) throws IOException {
		List<ChatMessage> messages = socketClient.chatService.getAllMessages();

		System.out.println(messages);
		if (messages != null && !messages.isEmpty()) {
			for (ChatMessage msg : messages) {
				boolean isSend = msg.getSenderId().equals(user.getIdHex());
				if (msg.getType().equalsIgnoreCase("file")) {
					renderFileMessage(msg, isSend);
				} else {
					onSendAndReceiveMessenge(msg, isSend);
				}
			}
		}
	}
}
