package controller.render;

import java.util.function.Consumer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import model.User;

public class NodeUserInListUser {

	@FXML
	private Circle avatarNode;

	@FXML
	private Text nameNode;

	@FXML
	private RadioButton radioCustomer;

	private static Consumer<User> onSelectUser;
	
	private static Consumer<User> unSelectUser;

	public void setOnSelect(Consumer<User> consumer) {
		onSelectUser = consumer;
	}
	
	public void setUnSelect(Consumer<User> consumer) {
		unSelectUser = consumer;
	}

	private User user;

	@FXML
	void isSelect(ActionEvent event) {
		if (radioCustomer.isSelected()) {
			if (onSelectUser != null) {
				onSelectUser.accept(user);
			}
		} else {
//			onSelectUser = null;
			if (unSelectUser != null) {
				unSelectUser.accept(user);
			}
		}
	}
	
	public void setSelected(boolean selected) {
	    radioCustomer.setSelected(selected); 
	}

	public void setUp(User user) {
		this.user = user;

		nameNode.setText(user.getUsername());

		Image image = new Image(user.getAvatarUrl(), true);

		image.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !image.isError()) {
				avatarNode.setFill(new ImagePattern(image));
			}
		});
	}
	
	public void setUpForDisplay(User user) {
		radioCustomer.setOpacity(0);
		
		nameNode.setText(user.getUsername());

		Image image = new Image(user.getAvatarUrl(), true);

		image.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !image.isError()) {
				avatarNode.setFill(new ImagePattern(image));
			}
		});
	}

}
