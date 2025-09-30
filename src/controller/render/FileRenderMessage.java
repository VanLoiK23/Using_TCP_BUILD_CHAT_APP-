package controller.render;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import controller.ServerAndClientSocket.SocketClient;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import model.ChatMessage;
import model.FileInfo;
import service.RedisUserService;

public class FileRenderMessage {
	class styleDifferenceClass {
		private Pos position;
		private String styleCss;

		styleDifferenceClass(Pos position, String styleCss) {
			this.position = position;
			this.styleCss = styleCss;
		}
	}

	private String lastSenderId;

	public String getLastSenderId() {
		return lastSenderId;
	}

	public void setLastSenderId(String lastSenderId) {
		this.lastSenderId = lastSenderId;
	}

	public void renderFileMessage(ChatMessage chatMessage, boolean isSend, SocketClient socketClient, VBox vboxInScroll,
			RedisUserService redisUserService) {
		FileInfo fileData = socketClient.gson.fromJson(chatMessage.getContent(), FileInfo.class);
		String filename = fileData.getFileName();
		String url = fileData.getUrlUpload();

		Label fileLabel = new Label("📎 " + filename);
		fileLabel.setStyle("-fx-font-weight: bold;");

		ProgressIndicator circleProgress = new ProgressIndicator(0);
		circleProgress.setPrefSize(50, 50);
		Label percentLabel = new Label("0%");
		percentLabel.setStyle("-fx-font-weight: bold;");

		StackPane progressCircle = new StackPane(circleProgress, percentLabel);
		progressCircle.setVisible(false); // ẩn ban đầu

		Button downloadButton = new Button("Tải xuống");
		downloadButton.setStyle("-fx-background-color: transparent; -fx-text-fill: blue; -fx-underline: true;");
		downloadButton.setOnAction(e -> {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle("Chọn thư mục lưu file");
			File folder = chooser.showDialog(vboxInScroll.getScene().getWindow());

			if (folder != null) {
				progressCircle.setVisible(true);
				percentLabel.setText("0%");

				Task<Void> downloadTask = new Task<>() {
					@Override
					protected Void call() throws Exception {
						URL website = new URL(url);
						URLConnection connection = website.openConnection();
						int fileSize = connection.getContentLength();

						try (InputStream in = website.openStream();
								FileOutputStream fos = new FileOutputStream(new File(folder, filename))) {

							byte[] buffer = new byte[4096];
							int bytesRead;
							int totalRead = 0;

							while ((bytesRead = in.read(buffer)) != -1) {
								fos.write(buffer, 0, bytesRead);
								totalRead += bytesRead;
								double progress = (double) totalRead / fileSize;
								updateProgress(progress, 1);
							}
						}

						return null;
					}
				};

				circleProgress.progressProperty().bind(downloadTask.progressProperty());

				downloadTask.progressProperty().addListener((obs, oldVal, newVal) -> {
					int percent = (int) Math.round(newVal.doubleValue() * 100);
					percentLabel.setText(percent + "%");
				});

				downloadTask.setOnSucceeded(ev -> {
					percentLabel.setText("✅");
					try {
						Desktop.getDesktop().open(new File(folder, filename));
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				});

				downloadTask.setOnFailed(ev -> percentLabel.setText("❌"));

				new Thread(downloadTask).start();
			}
		});

		VBox fileBox = new VBox(5, fileLabel, downloadButton, progressCircle);
		fileBox.setPadding(new Insets(5, 10, 5, 10));
		fileBox.setMaxWidth(300);

		Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
		mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT,
				"-fx-background-color: rgb(15,125,242); -fx-background-radius: 20px;"));
		mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT,
				"-fx-background-color: rgb(233,233,235); -fx-background-radius: 20px;"));

		fileBox.setStyle(mapStyleMessenger.get(isSend).styleCss);

		HBox hBox = new HBox(10);
		hBox.setAlignment(mapStyleMessenger.get(isSend).position);
		hBox.setPadding(new Insets(5, 5, 5, 10));

		if (!isSend) {
			ImageView avatar = new ImageView(
					new Image(redisUserService.getCachedAvatar(chatMessage.getSenderId()), true));
			avatar.setFitWidth(30);
			avatar.setFitHeight(30);
			avatar.setClip(new Circle(15, 15, 15));
			hBox.getChildren().addAll(avatar, fileBox);
		} else {
			hBox.getChildren().add(fileBox);
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

	@FXML
	private ProgressIndicator progressCircle;
	
	@FXML
	private FontAwesomeIcon checkIcon;

	@FXML
	private FontAwesomeIcon downloadIcon;

	@FXML
	private StackPane downloadStatus;

	@FXML
    private Label nameText;

	private FileInfo fileInfo;

	@FXML
	private AnchorPane fileFormat;

	public void setFileInfo(FileInfo info) {
		this.fileInfo = info;

		if (nameText != null) {
			nameText.setText(info.getFileName());
			nameText.setMaxWidth(90);
			nameText.setEllipsisString("..."); 
			nameText.setTextOverrun(OverrunStyle.ELLIPSIS);

		}

		if (downloadStatus != null) {
			downloadStatus.setOnMouseClicked(e -> {
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle("Chọn thư mục lưu file");
				File folder = chooser.showDialog(downloadStatus.getScene().getWindow());

				if (folder != null) {
					startDownload(info.getUrlUpload(), info.getFileName(), folder);
				}
			});
		}
	}

	public void setFileInfoLocal(File file) {
		downloadIcon.setIcon(FontAwesomeIcons.FOLDER_OPEN);

		if (nameText != null) {
			nameText.setText(file.getName());
			nameText.setMaxWidth(90);
			nameText.setEllipsisString("..."); 
			nameText.setTextOverrun(OverrunStyle.ELLIPSIS);
		}

		if (downloadStatus != null) {
			downloadStatus.setOnMouseClicked(e -> {
				try {
					Desktop.getDesktop().open(file); // Mở file bằng app mặc định
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			});
		}
	}

	public void startDownload(String url, String filename, File targetFolder) {
		progressCircle.setVisible(true);
		downloadIcon.setVisible(false);
		checkIcon.setVisible(false);

		Task<Void> downloadTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				URL website = new URL(url);
				URLConnection connection = website.openConnection();
				int fileSize = connection.getContentLength();

				try (InputStream in = website.openStream();
						FileOutputStream fos = new FileOutputStream(new File(targetFolder, filename))) {

					byte[] buffer = new byte[4096];
					int bytesRead;
					int totalRead = 0;

					while ((bytesRead = in.read(buffer)) != -1) {
						fos.write(buffer, 0, bytesRead);
						totalRead += bytesRead;
						updateProgress(totalRead, fileSize);
					}
				}

				return null;
			}
		};

		progressCircle.progressProperty().bind(downloadTask.progressProperty());

		downloadTask.setOnSucceeded(ev -> {
			progressCircle.setVisible(false);
			checkIcon.setVisible(true);
			try {
				Desktop.getDesktop().open(new File(targetFolder, filename));
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});

		downloadTask.setOnFailed(ev -> {
			progressCircle.setVisible(false);
			downloadIcon.setVisible(true);
		});

		new Thread(downloadTask).start();
	}

}
