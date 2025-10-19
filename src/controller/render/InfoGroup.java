package controller.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import model.Group;
import service.UserService;

public class InfoGroup {

	@FXML
	private Text ipGroup;

	@FXML
	private Text nameGroup;

	@FXML
	private ScrollPane scrollPane;

	@FXML
	private VBox vBoxInScroll;

	private UserService userService = new UserService();

	public void setUp(Group group) throws IOException {
		vBoxInScroll.heightProperty().addListener((obs, oldVal, newVal) -> {
			scrollPane.setVvalue(1.0);
		});

		nameGroup.setText(group.getGroupName());
		ipGroup.setText(group.getMulticastIP());

		List<Node> render = new ArrayList<Node>();
		for (String userID : group.getMembers()) {

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/nodeUserInListUser.fxml"));

			Node node = loader.load();

			NodeUserInListUser controller = loader.getController();
			controller.setUpForDisplay(userService.getUserById(userID));

			render.add(node);
		}

		vBoxInScroll.getChildren().addAll(render);
	}

}
