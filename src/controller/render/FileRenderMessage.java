package controller.render;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.FileInfo;

public class FileRenderMessage {

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

	@FXML
	private ImageView imageView;

	public void setImagesLocal(File file) {
		if (file == null || !file.exists()) {
			System.err.println("File không tồn tại hoặc null: " + file);
			return;
		}

		String lowerName = file.getName().toLowerCase();

		// Chỉ xử lý nếu là ảnh
		if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {

			imageView.setImage(new Image(file.toURI().toString()));
			
			Rectangle clip = new Rectangle(imageView.getFitWidth(), imageView.getFitHeight());
			clip.setArcWidth(30); 
			clip.setArcHeight(30); 
			imageView.setClip(clip);
			
			imageView.setVisible(true);

			// Thêm sự kiện click
			imageView.setOnMouseClicked(e -> {
				try {
					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().open(file);
					} else {
						System.err.println("Desktop API không được hỗ trợ trên hệ điều hành này.");
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			});
		} else {
			System.err.println("File không phải ảnh hợp lệ: " + file.getName());
		}
	}

	public void setImageInfo(FileInfo info) {
		this.fileInfo = info;
		String lowerName = info.getFileName().toLowerCase();

		if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
			// load ảnh từ Cloudinary
			Image image = new Image(info.getUrlUpload(), true);
			image.progressProperty().addListener((obs, oldVal, newVal) -> {
				if (newVal.doubleValue() == 1.0 && !image.isError()) {
					imageView.setImage(image);

					Rectangle clip = new Rectangle(imageView.getFitWidth(), imageView.getFitHeight());
					clip.setArcWidth(15);
					clip.setArcHeight(15);
					imageView.setClip(clip);

					imageView.setVisible(true);
				}
			});

			// click để mở ảnh
			imageView.setOnMouseClicked(e -> {
				try {
					 showImagePopup(info.getUrlUpload());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
		} else {
			System.err.println("File không phải ảnh hợp lệ: " + info.getFileName());
		}
	}
	
	public void showImagePopup(String imageUrl) {
	    Image image = new Image(imageUrl, true);
	    ImageView imageView = new ImageView(image);
	    imageView.setPreserveRatio(true);
	    imageView.setFitWidth(600);
	    imageView.setFitHeight(600);
	    
	    imageView.setOnScroll(event -> {
	        double delta = event.getDeltaY();
	        double scale = (delta > 0) ? 1.1 : 0.9;
	        imageView.setFitWidth(imageView.getFitWidth() * scale);
	        imageView.setFitHeight(imageView.getFitHeight() * scale);
	    });

//	    Button closeButton = new Button("X");
//	    closeButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
//	    closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());

	    VBox content = new VBox(20, imageView);
	    content.setAlignment(Pos.CENTER);
	    content.setPadding(new Insets(20));

	    StackPane root = new StackPane(content);
	    root.setStyle("-fx-background-color: rgba(0,0,0,0.8);");

	    Scene scene = new Scene(root);
	    Stage stage = new Stage();
	    stage.setTitle("Xem ảnh");
	    stage.setScene(scene);
	    stage.initModality(Modality.APPLICATION_MODAL);
	    stage.show();
	}

}
