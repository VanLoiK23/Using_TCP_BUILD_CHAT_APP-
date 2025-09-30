//package controller;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.net.Socket;
//
//import javafx.scene.layout.VBox;
//
//public class ServerController {
//	private Socket socket;
//	private BufferedWriter out;
//	private BufferedReader in;
//	private BufferedWriter clientPrintWriter;
//
//	public ServerController(Socket socket, BufferedWriter clientPrintWriter) {
//		this.socket = socket;
//		this.clientPrintWriter = clientPrintWriter;
//	}
//
//	public ServerController() {
//		try {
//			in = new BufferedReader((new InputStreamReader(socket.getInputStream())));
//			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//
////			synchronized (clientPrintWriter) {
////				clientPrintWriter.
////			}
//
//		} catch (IOException exception) {
//			exception.printStackTrace();
//		} finally {
//			close(socket, in, out);
//		}
//	}
//
//	public void sendtoServer(String message) {
//		try {
//			out.write(message);
//			out.newLine();
//			out.flush();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			close(socket, in, out);
//		}
//	}
//
//	public void receiveFromServer(VBox vbox) {
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				String message;
//
//				try {
//					while ((message = in.readLine()) != null) {
//
//						ClientController clientController = new ClientController();
//						clientController.onSendAndReceiveMessenge(message, vbox, false);
//
//					}
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//					close(socket, in, out);
//
//				}
//			}
//		}).start();
//
//	}
//
//	public void close(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
//		try {
//			if (bufferedWriter != null) {
//				bufferedWriter.close();
//			}
//			if (bufferedReader != null) {
//				bufferedReader.close();
//			}
//			if (socket != null) {
//				socket.close();
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//}

package controller.ServerAndClientSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import controller.Handler.FileHandler;
import controller.Handler.LoginHandler;
import controller.Handler.MessageHandler;
import controller.Handler.RegisterHandler;
import model.ChatMessage;
import model.FileInfo;
import model.Packet;
import model.User;
import service.ChatService;
import service.CloudinaryService;
import service.RedisOnlineService;
import service.UserService;
import util.RedisUtil;

//Client handle
public class SocketServer implements Runnable {
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
//	private InputStream inputStream;
	public Gson gson;

	private RedisOnlineService redisOnlineService;
	private UserService userService;
	private CloudinaryService cloudinaryService;
	private ChatService chatService;
	private String userID;

	// Danh sách tất cả client đang kết nối (dùng Vector để quản lý)
	private static Vector<SocketServer> clients = new Vector<>();

	public SocketServer(Socket socket) {
		try {
			this.socket = socket;
//			this.inputStream = socket.getInputStream();
			this.in = new DataInputStream(socket.getInputStream());
			this.out = new DataOutputStream(socket.getOutputStream());
			gson = Converters.registerAll(new GsonBuilder()).setDateFormat("EEE MMM dd HH:mm:ss z yyyy").create();
			this.redisOnlineService = new RedisOnlineService(RedisUtil.getClient());
			this.userService = new UserService();
			this.cloudinaryService = new CloudinaryService();
			this.chatService = new ChatService();

			clients.add(this); // thêm client vào danh sách

			System.out.println("Client connected: " + socket.getInetAddress());

		} catch (IOException e) {
			close();
		}
	}

	@Override
	public void run() {
		try {
			String message;
			while ((message = in.readUTF()) != null) {

				System.out.println("Received: " + message);

				Packet packet = gson.fromJson(message, Packet.class);

				System.out.println(packet);
				switch (packet.getType()) {
				case "LOGIN": {
					LoginHandler loginHandler = new LoginHandler(userService, gson);
					Packet loginResponse = loginHandler.handle(packet);
					// if login success add to list online
					User user = gson.fromJson(gson.toJson(loginResponse.getData()), User.class);
					if (user != null) {
						this.userID = user.getIdHex();
						redisOnlineService.setUserOnline(this.userID);
					}
					sendSelfClient(loginResponse);
					break;
				}
				case "REGISTER": {
					RegisterHandler registerHandler = new RegisterHandler(userService, gson);
					Packet registerReponse = registerHandler.handle(packet);
					sendSelfClient(registerReponse);
					break;
				}
				case "FILE_META": {
					ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

					FileInfo meta = gson.fromJson(chatMessage.getContent(), FileInfo.class);

//					new Thread(() -> {
					FileHandler fileHandler = new FileHandler(cloudinaryService, gson, in, chatService);
					fileHandler.setPacketCallback(packetReponse -> {
						broadcast(gson.toJson(packetReponse));

						System.out.println("Reponse: " + packetReponse);
					});

					fileHandler.receiveFile(meta, chatMessage);
//					    fileHandler.receiveFile(); // nhận file bằng inputStream.read(...)
//					}).start();

					break;
				}
				default:

					MessageHandler messageHandler = new MessageHandler(chatService, gson);
					if (messageHandler.handleSave(packet)) {
						broadcast(message); // gửi cho tất cả client khác
					}
				}
			}
		} catch (IOException e) {
			close();
		}
	}

	private void sendSelfClient(Packet packet) throws IOException {
		this.out.writeUTF(gson.toJson(packet));
		this.out.flush();
	}

	// Gửi tin nhắn cho tất cả client trong Vector
	private void broadcast(String message) {
		for (SocketServer client : clients) {
			if (client != this) {
				try {
					if (client.socket.isClosed())
						continue;

					client.out.writeUTF(message);
					client.out.flush();
				} catch (IOException e) {
					client.close();
				}
			}
		}
	}

	// Đóng kết nối
	private void close() {
		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (socket != null)
				socket.close();

			clients.remove(this);

			System.out.println("Client disconnected.");

			// remove from list online
			redisOnlineService.setUserOffline(this.userID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
