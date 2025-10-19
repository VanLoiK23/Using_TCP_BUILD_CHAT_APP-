package controller.render;

import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import model.Group;
import model.User;

public class NodeClientRenderClientSide {

	@FXML
	private Circle avatarUser_Group;

	@FXML
	private Text lastMessageGroup_Client;

	@FXML
	private Text nameGroup_Client;

	private static Consumer<Group> onCLick;

	private static Consumer<User> onCLickPrivate;

	private Group group;
	private User user;

	public void setOnClick(Consumer<Group> handler) {
		onCLick = handler;
	}

	public void setOnClickPrivate(Consumer<User> handler) {
		onCLickPrivate = handler;
	}

	@FXML
	void selecteUser_Group(MouseEvent event) {
		if (group != null) {
			if (onCLick != null) {
				onCLick.accept(group);
			}
		} else {
			if (onCLickPrivate != null) {
				onCLickPrivate.accept(user);
			}
		}
	}

	public void setUp(Group group, User user, String lastMessage) {
		this.group = group;
		this.user = user;

		nameGroup_Client.setText((group == null) ? user.getUsername() : group.getGroupName());

		if (group != null && group.getImageGroup() == null) {
			group.setImageGroup("https://i.ibb.co/43741PV/download.jpg");
		}

		Image image = new Image((group == null) ? user.getAvatarUrl() : group.getImageGroup(), true);

		image.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !image.isError()) {
				avatarUser_Group.setFill(new ImagePattern(image));
			}
		});

		lastMessageGroup_Client.setText(lastMessage);
	}

}
