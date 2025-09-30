package controller.LoginAndController;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Packet;
import model.User;
import service.UserService;
import util.PasswordUtil;

public class RegisterController implements Initializable {
	private SocketClient socketClient;

	@FXML
	private DatePicker bod;

	@FXML
	private Circle circleAvatar;

	@FXML
	private VBox containerFIleChoose;

	@FXML
	private TextField email;

	@FXML
	private RadioButton femaleRadio;

	@FXML
	private RadioButton maleRadio;

	@FXML
	private ImageView img;

	@FXML
	private PasswordField password;

	@FXML
	private PasswordField retype;

	@FXML
	private TextField username;

	@FXML
	private Button button;

	@FXML
	private ProgressIndicator progress;

	private ToggleGroup genderGroup;

	private CommonController commonController = new CommonController();

	private File selectedAvatarFile;

	private UserService userService = new UserService();

	private NavigateBetweenLoginRegister parentController;

	public void setParentController(NavigateBetweenLoginRegister controller) {
		this.parentController = controller;
	}

	@FXML
	void create(MouseEvent event) throws IOException {
		if (!email.getText().isEmpty() && !password.getText().isEmpty() && bod.getValue() != null
				&& !retype.getText().isEmpty() && !username.getText().isEmpty()
				&& genderGroup.getSelectedToggle() != null) {

			Pattern pa = Pattern.compile("\\d*");
			Matcher ma = pa.matcher(username.getText());

			if (username.getText().length() < 5 || username.getText().length() > 20) {
				commonController.alertInfo(Alert.AlertType.WARNING, "Cảnh báo!",
						"Username của bạn quá ngắn hoặc quá dài!");
			} else if (ma.matches()) {
				commonController.alertInfo(Alert.AlertType.WARNING, "Lỗi!", "Username của bạn chỉ bao gồm số!");
			} else if (!password.getText().equals(retype.getText())) {
				commonController.alertInfo(Alert.AlertType.WARNING, "Lỗi!", "Password không trùng nhau!");
			} else if (password.getText().length() < 6) {
				commonController.alertInfo(Alert.AlertType.WARNING, "Lỗi!", "Password của bạn quá ngắn!");
			} else if (!commonController.isValidEmail(email.getText())) {
				commonController.alertInfo(Alert.AlertType.WARNING, "Lỗi!", "Email không đúng định dạng!");
			} else {
				// TODO: Add new User
				handleCreateAccountAsync();
			}
		} else {
			commonController.alertInfo(Alert.AlertType.WARNING, "Cảnh báo!", "Vui lòng nhập đầy đủ thông tin!");
		}
	}

	private void handleCreateAccountAsync() {
		progress.setPrefSize(40, 40);
		progress.setVisible(true);
		button.setText(null);
		button.setDisable(true);
		button.setOpacity(0.6);

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				try {
					// Tạo user object
					User user = new User();
					String gender = genderGroup.getSelectedToggle().getUserData().toString();
					String urlAvatar = user.getAvatarDefault(gender);

					// Upload ảnh nếu có
					if (selectedAvatarFile != null) {
						urlAvatar = userService.upsertImg(selectedAvatarFile);
					}

					if (urlAvatar != null && !urlAvatar.isEmpty()) {
						user.setUsername(username.getText());
						user.setBod(bod.getValue());
						user.setEmail(email.getText());
						user.setGender(gender);
						user.setAvatarUrl(urlAvatar);
						user.setCreateAt(new Date());
						user.setPassword(PasswordUtil.hashPassword(password.getText()));

						socketClient = SocketClient.getInstance();
						Packet packetRequest = new Packet();
						packetRequest.setType("REGISTER");
						packetRequest.setData(user);

						socketClient.sendPacket(packetRequest);

						Packet packetReponse = socketClient.responseQueue.poll(5, TimeUnit.SECONDS);

						if (packetReponse != null) {

							if (packetReponse.getType().equals("REGISTER_RESULT")) {
								String userID = socketClient.gson
										.fromJson(socketClient.gson.toJson(packetReponse.getData()), String.class);

								Platform.runLater(() -> {
									setDefault();

									if (userID != null) {
										FadeTransition fade = new FadeTransition(Duration.millis(300), button);
										fade.setFromValue(0);
										fade.setToValue(1);
										fade.play();

										commonController.alertInfo(Alert.AlertType.CONFIRMATION, "Thành công!",
												"Bạn đã tạo tài khoản thành công!");

										clean();

										// load login
										parentController.open_signin(null);

									} else {
										commonController.alertInfo(Alert.AlertType.ERROR, "Lỗi!",
												"Không thể tạo tài khoản. Vui lòng thử lại.");
									}
								});
							}
						} else {
							commonController.alertInfo(AlertType.ERROR, "Cảnh báo!!!!",
									"Không nhận được phản hồi nào từ server!");
						}
					} else {
						Platform.runLater(() -> {
							setDefault();

							commonController.alertInfo(Alert.AlertType.WARNING, "Lỗi!",
									"Upload ảnh không thành công! Đã dùng ảnh mặc định.");
						});
					}
				} catch (Exception ex) {
					Platform.runLater(() -> {
						setDefault();

						commonController.alertInfo(Alert.AlertType.ERROR, "Lỗi hệ thống!",
								"Đã xảy ra lỗi không mong muốn.");
					});
					ex.printStackTrace();
				}
				return null;
			}
		};

		new Thread(task).start();
	}

	private void setDefault() {
		progress.setVisible(false);
		button.setText("Create a account");
		button.setOpacity(1);
		button.setDisable(false);
	}

	private void clean() {
		username.setText(null);
		email.setText(null);
		password.setText(null);
		retype.setText(null);
		bod.setValue(null);
		genderGroup.selectToggle(null);
	}

	@FXML
	public void exit(MouseEvent event) {
		Stage stage = (Stage) img.getScene().getWindow();
		stage.close();
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		genderGroup = new ToggleGroup();
		maleRadio.setToggleGroup(genderGroup);
		femaleRadio.setToggleGroup(genderGroup);

		maleRadio.setUserData("male");
		femaleRadio.setUserData("female");

		Button btnChoose = new Button("Chọn ảnh");

		btnChoose.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Chọn ảnh avatar");
			fileChooser.getExtensionFilters()
					.addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

			File file = fileChooser.showOpenDialog(containerFIleChoose.getScene().getWindow());
			if (file != null) {
				selectedAvatarFile = file;

				Image image = new Image(file.toURI().toString());

				// Tạo ImageView chứa ảnh
				ImageView imageView = new ImageView(image);
				imageView.setFitWidth(circleAvatar.getRadius() * 2);
				imageView.setFitHeight(circleAvatar.getRadius() * 2);
				imageView.setPreserveRatio(false); // ép cho vừa khung

				// Tạo mask hình tròn
				Circle clip = new Circle(circleAvatar.getRadius(), circleAvatar.getRadius(), circleAvatar.getRadius());
				imageView.setClip(clip);

				// Render mask thành ảnh mới
				SnapshotParameters parameters = new SnapshotParameters();
				parameters.setFill(Color.TRANSPARENT);
				WritableImage croppedImage = imageView.snapshot(parameters, null);

				circleAvatar.setFill(new ImagePattern(croppedImage));
			}
		});

		// Thêm vào VBox
		containerFIleChoose.getChildren().addAll(btnChoose);

		bod.setDayCellFactory(picker -> new DateCell() {
			@Override
			public void updateItem(LocalDate date, boolean empty) {
				super.updateItem(date, empty);

				LocalDate today = LocalDate.now();
				LocalDate minDate = today.minusYears(5);

				if (empty || date.isAfter(minDate)) {
					setDisable(true);
					setStyle("-fx-background-color: #3c3f41; -fx-text-fill: #888888;"); // dịu, hòa hợp
				}
			}
		});

	}

}
