package controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import controller.ServerAndClientSocket.SocketServer;
import controller.render.NodeClientRenderServerSide;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import model.ChatMessage;
import model.Packet;

public class ServerController implements Initializable {
	private static Integer PORT = null;
	private static Integer MAX_CONNECT = null;
	private volatile boolean isRunning = false;
	private ExecutorService pool;
	private ServerSocket serverSocket;

	private SocketClient socketClient;

	private CommonController commonController = new CommonController();

	@FXML
	private TextField maxNumberClientTextFileld;

	@FXML
	private TextArea messageTextGerneral;

	@FXML
	private Label numberOnline;

	@FXML
	private TextField portNumberTextField;

	@FXML
	private ScrollPane scrolListClient;

	@FXML
	private ScrollPane scrollLog;

	@FXML
	private VBox vBoxInScrollList;

	@FXML
	private VBox vBoxInScrollLog;

	@FXML
	private Button start;

	@FXML
	private Button stop;

	@FXML
	void sendSystemMessage(KeyEvent event) throws IOException, InterruptedException {
		if (event.getCode().toString().equals("ENTER")) {

			if (messageTextGerneral.getText() != null && !messageTextGerneral.getText().isEmpty()) {
				socketClient = SocketClient.getInstance();

				ChatMessage chatMessage = new ChatMessage();
				chatMessage.setSenderId("Server");
				chatMessage.setReceiverId("all");
				chatMessage.setContent(messageTextGerneral.getText());
				chatMessage.setType("text");
				chatMessage.setRead(false);
				chatMessage.setTimestamp(LocalDateTime.now());

				Packet packetRequest = new Packet();
				packetRequest.setType("MESSAGE");
				packetRequest.setData(chatMessage);

				socketClient.sendPacket(packetRequest);
			}
		}
	}

	@FXML
	void unConnectAllClient(MouseEvent event) {
		SocketServer.unConnectClient("all");

		clearNodeClient();
	}

	@FXML
	void startServer(MouseEvent event) {
		if (commonController.checkValidTextField(portNumberTextField)
				&& commonController.checkValidTextField(maxNumberClientTextFileld)) {

			if (commonController.isValidNumber(portNumberTextField.getText())
					&& commonController.checkValidTextField(maxNumberClientTextFileld)) {
				PORT = Integer.parseInt(portNumberTextField.getText());
				MAX_CONNECT = Integer.parseInt(maxNumberClientTextFileld.getText());
			}
		}

		if (PORT == null && MAX_CONNECT == null) {
			commonController.alertInfo(AlertType.WARNING, "Vui lòng điền các thông số cho Server",
					"Server không khởi động do việc thiếu các thông số cần thiết");
		} else {
			isRunning = true;
			start.setDisable(true);
			start.setOpacity(0.3);
			stop.setDisable(false);
			stop.setOpacity(1);
			startServer();
			commonController.alertInfo(AlertType.INFORMATION, "Server đã được khởi tạo",
					"Server chạy trên port " + PORT);
		}
	}

	@FXML
	void stopServer(MouseEvent event) {
		if (!isRunning) {
			commonController.alertInfo(AlertType.WARNING, "Server chưa được khởi tạo", "Server chưa được khởi động");
		} else {

			isRunning = false;
			start.setDisable(false);
			start.setOpacity(1);
			stop.setDisable(true);
			stop.setOpacity(0.3);
			SocketServer.unConnectClient("all");

			try {
				if (serverSocket != null && !serverSocket.isClosed()) {
					serverSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (pool != null && !pool.isShutdown()) {
//				pool.shutdownNow(); // dừng tất cả thread xử lý client
				
				pool.shutdown();
				
				try {
		            // đợi các client đang chạy kết thúc trong 5s
		            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
		                System.out.println("Force shutdown...");
//		                pool.awaitTermination(3, TimeUnit.SECONDS);
		                pool.shutdownNow(); // vẫn còn thì mới ép dừng
		            }
		        } catch (InterruptedException e) {
		            pool.shutdownNow();
		            Thread.currentThread().interrupt();
		        }

			}
			
			clearNodeClient();

			commonController.alertInfo(AlertType.INFORMATION, "Server đã tắt", "Server đã ngừng chạy ");
		}
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		SocketServer.setOnClientConnected(client -> {
			if (client.getUserID() != null) {
				Platform.runLater(() -> {
					addClientNode(client);
					updateOnlineCount();
				});
			}
		});

		SocketServer.setOffClientDisConnected(client -> {
			if (client.getUserID() != null) {
				Platform.runLater(() -> {
					removeClientNode(client);
					updateOnlineCount();
				});
			}
		});

		vBoxInScrollList.heightProperty().addListener((obs, oldVal, newVal) -> {
			scrolListClient.setVvalue(1.0);
		});

		vBoxInScrollLog.heightProperty().addListener((obs, oldVal, newVal) -> {
			scrollLog.setVvalue(1.0);
		});

	}

	private void addClientNode(SocketServer client) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/clientNodeServerSide.fxml"));
			Node node = loader.load();
			NodeClientRenderServerSide controller = loader.getController();
			controller.setClient(client);
			node.setUserData(client.getUserID());
			vBoxInScrollList.getChildren().add(node);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void removeClientNode(SocketServer client) {
		vBoxInScrollList.getChildren().removeIf(node -> {
			Object data = node.getUserData();
			return data != null && data.equals(client.getUserID());
		});
	}

	private void clearNodeClient() {
		Platform.runLater(() -> {
			vBoxInScrollList.getChildren().clear();
			numberOnline.setText("0");
		});
	}

	private void updateOnlineCount() {
	    List<SocketServer> snapshot = new ArrayList<>(SocketServer.getClients());
	    long count = snapshot.stream()
	        .filter(c -> c.getUserID() != null)
	        .count();

	    numberOnline.setText(String.valueOf(count));
	}

	private void startServer() {
		Thread serverThread = new Thread(() -> {
			pool = null;
			try {
				serverSocket = new ServerSocket(PORT);

				System.out.println("Server started on port " + PORT);
				isRunning = true;

				// max n thread
				pool = Executors.newFixedThreadPool(MAX_CONNECT);

//				pool = Executors.newCachedThreadPool(); // số lượng ko ổn định các thread execute // và xog thì reuse

				while (isRunning) {
//
//					// each client tương ứng với mỗi thread nếu nhiều client thì nhiều thread -> tốn
//					// tài nguyên
//					ServerController clientHandler = new ServerController(socket);
//					new Thread(clientHandler).start();
//					
					try {
						Socket socket = serverSocket.accept();
						
						//2 minute if don't call inputStream in this range time then throw SocketTimeoutException
//						socket.setSoTimeout(120000);
						if (!isRunning) {
				            socket.close(); // server đã tắt -> không xử lý
				            break;
				        }
						
						if (socket.isClosed() || !socket.isConnected()) {
						    System.out.println("Socket không hợp lệ, bỏ qua");
						    continue;
						}
						
						pool.execute(new SocketServer(socket));
					} catch (IOException e) {
						if (!isRunning)
							break; // server đã stop
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				isRunning = false;
			}
		});
		serverThread.setDaemon(true); // để khi tắt app thì server cũng tắt
		serverThread.start();
	}

}
