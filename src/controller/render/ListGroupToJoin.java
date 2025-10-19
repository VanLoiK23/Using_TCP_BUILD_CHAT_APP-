package controller.render;

import java.util.function.Consumer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import model.Group;

public class ListGroupToJoin {

	@FXML
	private Circle avatarGroup;

	@FXML
	private Text nameGroup;

	private static Consumer<Group> onCLick;
	
	private Group group;

	public void setOnClick(Consumer<Group> handler) {
		onCLick = handler;
	}

    @FXML
    void onClickJoin(ActionEvent event) {
    	if (onCLick != null) {
			onCLick.accept(group);
		}
    }


	public void setUp(Group group) {
		nameGroup.setText(group.getGroupName());
		this.group=group;

		if (group.getImageGroup() == null) {
			group.setImageGroup("https://i.ibb.co/43741PV/download.jpg");
		}

		Image image = new Image(group.getImageGroup(), true);

		image.progressProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal.doubleValue() == 1.0 && !image.isError()) {
				avatarGroup.setFill(new ImagePattern(image));
			}
		});
	}

}
