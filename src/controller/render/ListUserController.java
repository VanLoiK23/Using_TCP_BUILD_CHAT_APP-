package controller.render;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Group;
import model.Packet;
import model.User;
import service.UserService;

public class ListUserController implements Initializable {

	private UserService userService = new UserService();
	private SocketClient socketClient;

	@FXML
	private StackPane avatarCircle;

	@FXML
	private FontAwesomeIcon iconCamera;

	@FXML
	private Button btEx;

	@FXML
	private Button addGroup;

	@FXML
	private TextField nameGroupTextField;

	@FXML
	private TextField nameSearchTextField;

	@FXML
	private ScrollPane scrollPane;

	@FXML
	private VBox vBoxInScroll;

	private CommonController commonController = new CommonController();

	private List<User> users = new ArrayList<User>();

	private File selectedAvatarFile;

	private User userCreate;

	private List<User> allUsers = new ArrayList<>();
	private List<User> filteredUsers = new ArrayList<>();

	public User getUserCreate() {
		return userCreate;
	}

	public void setUserCreate(User userCreate) {
		this.userCreate = userCreate;
	}

	@FXML
	void choosenAvatar(MouseEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Chọn ảnh avatar");
		fileChooser.getExtensionFilters()
				.addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

		File file = fileChooser.showOpenDialog(btEx.getScene().getWindow());
		if (file != null) {
			avatarCircle.getChildren().clear();
			selectedAvatarFile = file;

			Image image = new Image(file.toURI().toString());

			// Tạo ImageView chứa ảnh
			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(45);
			imageView.setFitHeight(30);
			imageView.setPreserveRatio(false); // ép cho vừa khung
			imageView.setClip(new Circle(15, 15, 15));

			avatarCircle.getChildren().add(imageView);
		}
	}

	@FXML
	void onClose(ActionEvent event) {
		commonController.onExit(btEx);
	}

	@FXML
	void submit(ActionEvent event) throws IOException, InterruptedException {

		if (nameGroupTextField != null && !nameGroupTextField.getText().isEmpty()) {

			users.add(userCreate);

			Group group = new Group();
			String urlGroup = "https://i.ibb.co/43741PV/download.jpg";

			if (selectedAvatarFile != null) {
				urlGroup = userService.upsertImg(selectedAvatarFile);
			}

			group.setImageGroup(urlGroup);
			group.setGroupName(nameGroupTextField.getText());
			group.setTimestamp(LocalDateTime.now());
			group.setMembers(users.stream().map(User::getIdHex).toList());

			Packet packetRequest = new Packet();
			packetRequest.setType("CREATE_GROUP");
			packetRequest.setData(group);

			socketClient.getInstance().sendPacket(packetRequest);

			Packet packetReponse;
			if (socketClient.getInstance().responseCreateGRQueue != null) {

				try {
					packetReponse = socketClient.getInstance().responseCreateGRQueue.poll(5, TimeUnit.SECONDS);

					if (packetReponse != null) {

						commonController.alertInfo(AlertType.INFORMATION, "Successfull!", "Tạo nhóm thành công!");

						Stage stage = (Stage) btEx.getScene().getWindow();
						stage.close();

						avatarCircle.getChildren().clear();
						avatarCircle.getChildren().add(iconCamera);
						nameGroupTextField.setText(null);
						nameSearchTextField.setText(null);
					} else {
						commonController.alertInfo(AlertType.ERROR, "Cảnh báo!!!!",
								"Không nhận được phản hồi nào từ server!");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else {
			commonController.alertInfo(Alert.AlertType.WARNING, "Cảnh báo!", "Vui lòng nhập đầy đủ thông tin!");
		}
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		vBoxInScroll.heightProperty().addListener((obs, oldVal, newVal) -> {
			scrollPane.setVvalue(1.0);
		});

		addGroup.setDisable(true);
		addGroup.setStyle("-fx-background-color:gray");

//		List<User> userList = userService.getAllUser();
//
//		if (getUserCreate() != null) {
//			userList.remove(getUserCreate());
//		}
//
//		if (userList != null && !userList.isEmpty()) {
//			List<Node> render = new ArrayList<Node>();
//
//			for (User us : userList) {
//				FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/nodeUserInListUser.fxml"));
//
//				try {
//					Node node = loader.load();
//
//					NodeUserInListUser nodeController = loader.getController();
//
//					nodeController.setOnSelect(userSelect -> {
//						if (userSelect != null) {
//							users.add(userSelect);
//						}
//
//						updateAddGroupButtonState();
//						System.out.println("Debug users add " + users);
//					});
//
//					nodeController.setUnSelect(userUnSelect -> {
//						if (userUnSelect != null) {
//							users.remove(userUnSelect);
//						}
//
//						updateAddGroupButtonState();
//
//						System.out.println("Debug users remove " + users);
//					});
//
//					nodeController.setUp(us);
//
//					render.add(node);
//
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//
//			Platform.runLater(() -> vBoxInScroll.getChildren().addAll(render));
//		}
//
//		nameSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
//			// ví dụ: gọi hàm tìm kiếm khi người dùng nhập
//			performSearch(newValue);
//		});

		allUsers = userService.getAllUser();

		if (getUserCreate() != null) {
			allUsers.remove(getUserCreate());
		}

		renderUserList(allUsers);

		nameSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			performSearch(newValue);
		});

	}

	private void updateAddGroupButtonState() {
		if (users != null && !users.isEmpty()) {
			boolean canCreateGroup = users.size() >= 2;
			addGroup.setDisable(!canCreateGroup);
			addGroup.setStyle(canCreateGroup ? "-fx-background-color:#2196f3" : "-fx-background-color:gray");
		}
	}

	private void performSearch(String keyword) {
	    if (keyword == null || keyword.trim().isEmpty()) {
	        filteredUsers = new ArrayList<>(allUsers);
	    } else {
	        String lowerKeyword = keyword.toLowerCase();
	        filteredUsers = allUsers.stream()
	                .filter(u -> u.getUsername().toLowerCase().contains(lowerKeyword)
	                        || u.getEmail().toLowerCase().contains(lowerKeyword))
	                .collect(Collectors.toList());
	    }

	    renderUserList(filteredUsers);
	}

	private void renderUserList(List<User> usersToRender) {
	    Platform.runLater(() -> {
	        vBoxInScroll.getChildren().clear();

	        List<Node> render = new ArrayList<>();

	        for (User us : usersToRender) {
	            try {
	                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/nodeUserInListUser.fxml"));
	                Node node = loader.load();

	                NodeUserInListUser nodeController = loader.getController();

	                // Nếu user này đang nằm trong danh sách "đã chọn" thì bật lại trạng thái
	                boolean isSelected = users.stream().anyMatch(u -> u.getIdHex() == us.getIdHex());
	                nodeController.setSelected(isSelected);

	                nodeController.setOnSelect(userSelect -> {
	                    if (userSelect != null && !users.contains(userSelect)) {
	                        users.add(userSelect);
	                    }
	                    updateAddGroupButtonState();
	                });

	                nodeController.setUnSelect(userUnSelect -> {
	                    if (userUnSelect != null) {
	                        users.removeIf(u -> u.getIdHex() == userUnSelect.getIdHex());
	                    }
	                    updateAddGroupButtonState();
	                });

	                nodeController.setUp(us);
	                render.add(node);

	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }

	        vBoxInScroll.getChildren().addAll(render);
	    });
	}

}
