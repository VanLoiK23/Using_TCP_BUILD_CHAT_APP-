package controller.render;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class VoiceMessageController {

	@FXML
	private ImageView playIcon;
	@FXML
	private Label fileNameLabel;

	private Image playImage = new Image(getClass().getResource("/Other/Img/play_audio.png").toExternalForm());
	private Image pauseImage = new Image(getClass().getResource("/Other/Img/pause_audio.jpg").toExternalForm());
	private Image loadingImage = new Image(getClass().getResource("/Other/Img/loading.png").toExternalForm());

	private String audioUrl;
	private byte[] audioData;
	private String originalFileName;

	// Biến trạng thái
	private boolean isPlaying = false;
	private boolean isLoading = false;
	private Clip audioClip;

	@FXML
	public void initialize() {
		playIcon.setOnMouseClicked(event -> {
			if (isPlaying) {
				stopAudio();
			} else if (!isLoading) {
				playAudio();
			}
		});
	}

	public void setAudioFile(byte[] audioData) {
		try {
			this.audioData = audioData;
			this.originalFileName = "voice" + System.currentTimeMillis();
			fileNameLabel.setText(this.originalFileName);
		} catch (Exception e) {
			fileNameLabel.setText("Lỗi đọc file");
			e.printStackTrace();
		}
	}

	public void setAudioUrl(String url, String fileName) {
		this.audioUrl = url;
		this.originalFileName = fileName;
		this.audioData = null;
		fileNameLabel.setText(this.originalFileName);
	}

	private void playAudio() {
		if (audioData != null) {
			playData(audioData);
			return;
		}

		if (audioUrl != null) {
			new Thread(this::downloadAndPlay).start();
		}
	}

	private void downloadAndPlay() {
		try {
			Platform.runLater(() -> {
				isLoading = true;
				playIcon.setImage(loadingImage);
				fileNameLabel.setText("Đang tải...");
			});

			URL url = new URL(this.audioUrl);
			InputStream in = url.openStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int n;
			while ((n = in.read(buffer)) > 0) {
				baos.write(buffer, 0, n);
			}
			in.close();

			this.audioData = baos.toByteArray();

			Platform.runLater(() -> {
				isLoading = false;
				playData(this.audioData);
			});

		} catch (Exception e) {
			e.printStackTrace();
			Platform.runLater(() -> {
				isLoading = false;
				fileNameLabel.setText("Lỗi tải file");
				playIcon.setImage(playImage);
			});
		}
	}

	private void playData(byte[] data) {
		new Thread(() -> {
			try {
				AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
				ByteArrayInputStream bais = new ByteArrayInputStream(data);
				AudioInputStream audioStream = new AudioInputStream(bais, format, data.length / format.getFrameSize());

				audioClip = AudioSystem.getClip();
				audioClip.addLineListener(event -> {
					if (event.getType() == LineEvent.Type.STOP) {
						Platform.runLater(this::resetPlayButton);
					}
				});

				audioClip.open(audioStream);

				Platform.runLater(() -> {
					playIcon.setImage(pauseImage);
					isPlaying = true;
					fileNameLabel.setText("Đang phát...");
				});

				audioClip.start();
			} catch (Exception e) {
				e.printStackTrace();
				Platform.runLater(this::resetPlayButton);
			}
		}).start();
	}

	private void stopAudio() {
		if (audioClip != null && audioClip.isRunning()) {
			audioClip.stop();
		}
		resetPlayButton();
	}

	private void resetPlayButton() {
		playIcon.setImage(playImage);
		isPlaying = false;
		isLoading = false;
		if (audioClip != null) {
			audioClip.close();
		}
		fileNameLabel.setText(this.originalFileName);
	}
}