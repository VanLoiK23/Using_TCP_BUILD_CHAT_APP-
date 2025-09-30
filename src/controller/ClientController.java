package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import model.ChatMessage;
import model.User;

public class ClientController implements Initializable {

	private static final String SERVER = "localhost";
	private static final int PORT = 12345;
	private PrintWriter out;

	@FXML
	private Text name;

	@FXML
	private Button button;

	@FXML
	private TextField textField;

//	private ServerController serverController;

	@FXML
	private VBox container;

	@FXML
	private ScrollPane scrollPane;

	private User user;

	private Gson gson = new Gson();

	public void setUser(User user) {
		this.user = user;
		
		name.setText(user.getUsername());
	}

	@FXML
	void clickEnter(KeyEvent event) throws IOException {
		if (event.getCode().toString().equals("ENTER")) {
			onSend(textField.getText());
		}
	}

	@FXML
	void onEnter(ActionEvent event) throws IOException {
		onSend(textField.getText());

	}

	private void onSend(String messenger) throws IOException {
		if (textField.getText() != null && !textField.getText().isEmpty()) {
//			serverController.(messenger);

			
			
//			ChatMessage chatMessage = new ChatMessage();
//			chatMessage.setSender(user);
//			chatMessage.setMessage(messenger);
//			chatMessage.setTimeSender(new Date());
//
//			out.println(gson.toJson(chatMessage));
//			out.flush();

//			onSendAndReceiveMessenge(chatMessage, true);

			textField.clear();
		}
	}

	@FXML
	void submit(MouseEvent event) throws IOException {
		onSend(textField.getText());
	}

	class styleDifferenceClass {
		private Pos position;
		private String styleCss;

		styleDifferenceClass(Pos position, String styleCss) {
			this.position = position;
			this.styleCss = styleCss;
		}
	}

	public void onSendAndReceiveMessenge(ChatMessage chatMessage, Boolean isSend) throws MalformedURLException {
//		User user=chatMessage.getSender();
		String message="";
//				chatMessage.getMessage();

		Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
		mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT, "-fx-text-fill: rgb(239,242,255);"
				+ "-fx-background-color: rgb(15,125,242);" + "-fx-background-radius: 20px;"));
		mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT,
				"-fx-text-fill: black;" + "-fx-background-color: rgb(233,233,235);" + "-fx-background-radius: 20px;"));

		// Avatar hình tròn
//		ImageView avatar = new ImageView(new Image("C:\\Users\\ACER\\Downloads\\download123.jpg"));
		File file = new File("C:\\Users\\ACER\\Downloads\\download123.jpg");
		Image image = new Image(file.toURI().toURL().toExternalForm());
		ImageView avatar = new ImageView(image);
		avatar.setFitWidth(30);
		avatar.setFitHeight(30);
		avatar.setClip(new Circle(15, 15, 15)); // hình tròn

		// Bong bóng tin nhắn
		Text text = new Text(message);
		if (isSend) {
			text.setFill(Color.color(0.934, 0.945, 0.996));
		}
		TextFlow textFlow = new TextFlow(text);
		textFlow.setStyle(mapStyleMessenger.get(isSend).styleCss);
		textFlow.setPadding(new Insets(5, 10, 5, 10));

		// HBox chứa avatar + tin nhắn
		HBox hBox = new HBox(10);
		hBox.setAlignment(mapStyleMessenger.get(isSend).position);
		hBox.setPadding(new Insets(5, 5, 5, 10));

		if (isSend) {
			hBox.getChildren().addAll(textFlow);
		} else {
			hBox.getChildren().addAll(avatar, textFlow); // avatar bên trái
		}

		// VBox chứa tên + HBox
		VBox messageBox = new VBox(2);

		if (isSend) {
			messageBox.getChildren().addAll(hBox);

			container.getChildren().add(messageBox);
		} else {
			// Tên người gửi
//			Label nameLabel = new Label(user.getUserName());
//			nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
//
//			messageBox.getChildren().addAll(nameLabel, hBox);
//
//			Platform.runLater(() -> container.getChildren().add(messageBox));
		}
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		System.out.println("Start initialize");

		Socket socket;

		container.heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				scrollPane.setVvalue((Double) arg2);
			}

		});

		try {
			socket = new Socket(SERVER, PORT);

			out = new PrintWriter(socket.getOutputStream(), true);

			System.out.println("Connected to server! heldas");

			// receive difference message from other client
			new Thread(() -> {
				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String msg;
					while ((msg = in.readLine()) != null) {
						ChatMessage chatMessage = gson.fromJson(msg, ChatMessage.class);
						
//						User sender=new User();
//						sender.setUserName(chatMessage.getSender().getUserName());

						onSendAndReceiveMessenge(chatMessage, false);
						System.out.println("Message from server: " + msg);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).start();

		} catch (IOException e) {
			e.printStackTrace();

			out.close();
		}

		// receive difference message from other client
//		new Thread(() -> {
//			try {
//				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//				String msg;
//				while ((msg = in.readLine()) != null) {
//					onSendAndReceiveMessenge(msg, false);
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}).start();

//		serverController.receiveFromServer(container);
	}

}
