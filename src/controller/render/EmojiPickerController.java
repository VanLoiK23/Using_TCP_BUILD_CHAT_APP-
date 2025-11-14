package controller.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;

public class EmojiPickerController {

	@FXML
	private ScrollPane scrollPane;
	@FXML
	private TilePane emojiTilePane;

	private Consumer<String> onEmojiSelected;

	private List<Emoji> allEmojis;
	private int loadedCount = 0;
	private final int BATCH_SIZE = 100; // load 50 emoji mỗi lần

	@FXML
	public void initialize() {
		allEmojis = new ArrayList<>(EmojiManager.getAll());
		loadNextBatch();

		// Lazy load khi scroll tới cuối
		scrollPane.vvalueProperty().addListener((obs, oldV, newV) -> {
			if (newV.doubleValue() >= 1.0) {
				loadNextBatch();
			}
		});
	}

	public void setOnEmojiSelected(Consumer<String> consumer) {
		this.onEmojiSelected = consumer;
	}

	// Hỗ trợ multi-codepoint emoji
	private String unicodeToCodepoints(String unicode) {
		StringBuilder sb = new StringBuilder();
		int[] cps = unicode.codePoints().toArray();
		for (int cp : cps) {
			if (sb.length() > 0)
				sb.append("-");
			sb.append(Integer.toHexString(cp));
		}
		return sb.toString();
	}

	private void loadNextBatch() {
		int end = Math.min(loadedCount + BATCH_SIZE, allEmojis.size());
		for (int i = loadedCount; i < end; i++) {
			addEmojiButton(allEmojis.get(i));
		}
		loadedCount = end;
	}

	private void addEmojiButton(Emoji emoji) {
		String unicode = emoji.getUnicode();
		String codepoint = unicodeToCodepoints(unicode);
		String localPath = "/Other/Img/emoji_36/" + codepoint + ".png";
		System.out.println("[DEBUG] Loading emoji button image: " + localPath);

		Image image = null;
		if (getClass().getResource(localPath) != null) {
			image = new Image(getClass().getResourceAsStream(localPath));
		} else {
			System.out.println("[WARNING] Local emoji image not found, fallback to CDN: " + codepoint);
			String imageUrl = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/" + codepoint + ".png";
			image = new Image(imageUrl, true);
		}

		ImageView iv = new ImageView(image);
		iv.setFitWidth(24);
		iv.setFitHeight(24);

		Button btn = new Button();
		btn.setGraphic(iv);
		btn.getStyleClass().add("emoji-button-image");

		btn.setOnAction(e -> {
			if (onEmojiSelected != null) {
				onEmojiSelected.accept(unicode);
			}
		});

		Platform.runLater(() -> emojiTilePane.getChildren().add(btn));
	}
}