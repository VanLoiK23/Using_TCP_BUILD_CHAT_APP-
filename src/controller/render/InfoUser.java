package controller.render;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Packet;
import model.User;
import service.UserService;
import util.PasswordUtil;

public class InfoUser implements Initializable {

	@FXML
	private DatePicker bod;

	@FXML
	private TextField email;

	@FXML
	private RadioButton femaleRadio;

	@FXML
	private Text labelSelectSex;

	@FXML
	private RadioButton maleRadio;

	@FXML
	private Text name;

	@FXML
	private TextField new_password;

	@FXML
	private TextField sex;

	@FXML
	private Button submitButton;

	@FXML
	private TextField username;

	@FXML
	private Button btnChoose;

	@FXML
	private Circle circleAvatar;

	private File selectedAvatarFile;
	private ToggleGroup genderGroup;
	private Boolean isEdit;
	private User currentUser;

	private CommonController commonController = new CommonController();
	private UserService userService = new UserService();
	private SocketClient socketClient;

	private Consumer<User> onCallBack;

	public void setCallBack(Consumer<User> onCallBack) {
		this.onCallBack = onCallBack;
	}

	public void setUpUser(User user, Boolean isEdit) {
		this.currentUser = user;
		this.isEdit = isEdit;

		setVisible(isEdit);

		name.setText(user.getUsername());
		username.setText(user.getUsername());

		Image image = new Image(user.getAvatarUrl(), true);

		image.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !image.isError()) {
				circleAvatar.setFill(new ImagePattern(image));
			}
		});

		email.setText(user.getEmail());
		bod.setValue(user.getBod());
		String gender = user.getGender();
		sex.setText(gender);
		if (gender.equalsIgnoreCase("male")) {
			genderGroup.selectToggle(maleRadio);
		} else if (gender.equalsIgnoreCase("female")) {
			genderGroup.selectToggle(femaleRadio);
		}

	}

	private void setVisible(Boolean isVisible) {
		sex.setVisible(!isVisible);
		labelSelectSex.setVisible(isVisible);
		maleRadio.setVisible(isVisible);
		femaleRadio.setVisible(isVisible);
		submitButton.setVisible(isVisible);
		new_password.setVisible(isVisible);
		btnChoose.setVisible(isVisible);

		username.setDisable(!isVisible);
		email.setDisable(!isVisible);
		bod.setDisable(!isVisible);
	}

	@FXML
	void back(ActionEvent event) {
		if (!isEdit) {
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.close();
		} else {
			commonController.alertConfirm("Thoát", "Những thay đổi sẽ không được lưu?", confirmed -> {
				if (confirmed) {
					Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
					stage.close();
				} else {
					System.out.println("Người dùng hủy thao tác.");
				}
			});

		}
	}

	@FXML
	void submit(ActionEvent event) throws IOException, InterruptedException {

		if (username.getText() != null && email.getText() != null && genderGroup.getSelectedToggle() != null
				&& bod.getValue() != null && !username.getText().isEmpty() && !email.getText().isEmpty()) {

			Pattern pa = Pattern.compile("\\d*");
			Matcher ma = pa.matcher(username.getText());

			if (username.getText().length() < 5 || username.getText().length() > 20) {
				commonController.alertInfo(Alert.AlertType.WARNING, "Cảnh báo!",
						"Username của bạn quá ngắn hoặc quá dài!");
			} else if (ma.matches()) {
				commonController.alertInfo(Alert.AlertType.WARNING, "Lỗi!", "Username của bạn chỉ bao gồm số!");
			} else if (!commonController.isValidEmail(email.getText())) {
				commonController.alertInfo(Alert.AlertType.WARNING, "Lỗi!", "Email không đúng định dạng!");
			} else {

				if (new_password.getText() != null && !new_password.getText().isEmpty()) {
					if (new_password.getText().length() < 6) {
						commonController.alertInfo(Alert.AlertType.WARNING, "Lỗi!", "Password của bạn quá ngắn!");
					} else {
						// TODO: Add new User
						handleCreateAccountAsync(event);
					}
				} else {
					// TODO: Add new User
					handleCreateAccountAsync(event);
				}
			}
		}
	}

	private void handleCreateAccountAsync(ActionEvent event) throws IOException, InterruptedException {
		User user = new User();
		user.set_id(currentUser.get_id());

		String gender = genderGroup.getSelectedToggle().getUserData().toString();
		String urlAvatar = currentUser.getAvatarUrl();

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

			if (new_password.getText() != null && !new_password.getText().isEmpty()) {
				user.setPassword(PasswordUtil.hashPassword(new_password.getText()));
			}

			Packet packetRequest = new Packet();
			packetRequest.setType("UPDATE_INFO");
			packetRequest.setData(user);

			socketClient.sendPacket(packetRequest);
			Packet packetReponse;

			if (socketClient.getInstance().responseQueue != null) {

				packetReponse = socketClient.getInstance().responseQueue.poll(5, TimeUnit.SECONDS);

				if (packetReponse != null) {

					if (packetReponse.getType().equals("UPDATE_RESULT")) {
						Boolean isSuccess = socketClient.gson
								.fromJson(socketClient.gson.toJson(packetReponse.getData()), Boolean.class);

						Platform.runLater(() -> {
							if (isSuccess) {
								commonController.alertInfo(Alert.AlertType.CONFIRMATION, "Thành công!",
										"Bạn cập nhật thông tin tài khoản thành công!");

								if (onCallBack != null) {
									onCallBack.accept(user);
								}
								
								Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
								stage.close();
							} else {
								Platform.runLater(() -> {
									commonController.alertInfo(Alert.AlertType.ERROR, "Lỗi!",
											"Không thể cập nhật tài khoản. Vui lòng thử lại.");
								});
							}
						});
					} else if (packetReponse.getType().equals("DUPLICATE_UserName")) {
						Platform.runLater(() -> {
							commonController.alertInfo(AlertType.INFORMATION, "Không thể cập nhật tài khoản!!!!",
									"Tên đã được sử dụng!");
						});
					}
				} else {
					Platform.runLater(() -> {
						commonController.alertInfo(AlertType.ERROR, "Cảnh báo!!!!",
								"Không nhận được phản hồi nào từ server!");
					});
				}
			}
		}
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		sex.setDisable(true);

		genderGroup = new ToggleGroup();
		maleRadio.setToggleGroup(genderGroup);
		femaleRadio.setToggleGroup(genderGroup);

		maleRadio.setUserData("male");
		femaleRadio.setUserData("female");

		btnChoose.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Chọn ảnh avatar");
			fileChooser.getExtensionFilters()
					.addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

			File file = fileChooser.showOpenDialog(btnChoose.getScene().getWindow());
			if (file != null) {
				selectedAvatarFile = file;

				Image image = new Image(file.toURI().toString());

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

		try {
			socketClient = SocketClient.getInstance();

			socketClient.setOnServerDisconnected(reason -> {
				Platform.runLater(() -> {

					commonController.alertInfo(AlertType.WARNING, "❌ Server disconnected", reason);

					commonController.alertConfirm("Kết nối lại SERVER",
							"Bạn có chắc muốn kết nối lại với Server hay không?", confirmed -> {
								if (confirmed) {
									try {
										socketClient.reConnectToServer();
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
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

}
