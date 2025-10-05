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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

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
	private String clientIP;

	// Danh sách tất cả client đang kết nối (dùng Vector để quản lý)
	private static Vector<SocketServer> clients = new Vector<>();

	private static Consumer<SocketServer> onClientConnected;
	private static Consumer<SocketServer> onClientDisConnected;

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

			this.clientIP = socket.getInetAddress().toString();
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
					User userCheck = gson.fromJson(gson.toJson(packet.getData()), User.class);

					User userResult = userService.getUserByUserName(userCheck.getUsername());
					if (userResult != null && isUserOnline(userResult.getIdHex())) {
						Packet response = new Packet();
						response.setType("DUPLICATE_LOGIN");
						response.setData(null);

						sendSelfClient(response);
					} else {

						LoginHandler loginHandler = new LoginHandler(userService, gson);
						Packet loginResponse = loginHandler.handle(packet);
						// if login success add to list online
						User user = gson.fromJson(gson.toJson(loginResponse.getData()), User.class);
						if (user != null) {
							this.userID = user.getIdHex();
							if (RedisUtil.isAlive()) {
								redisOnlineService.setUserOnline(this.userID);
							}
						}
						// only login success
						if (onClientConnected != null) {
							onClientConnected.accept(this);
						}

						sendSelfClient(loginResponse);
					}
					break;
				}
				case "REGISTER": {
					User userCheck = gson.fromJson(gson.toJson(packet.getData()), User.class);

					User userResult = userService.getUserByUserName(userCheck.getUsername());

					if (userResult != null) {
						Packet response = new Packet();
						response.setType("DUPLICATE_UserName");
						response.setData(null);

						sendSelfClient(response);
					} else {
						RegisterHandler registerHandler = new RegisterHandler(userService, gson);
						Packet registerReponse = registerHandler.handle(packet);
						sendSelfClient(registerReponse);
					}
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
				case "PING": {
					Packet pong = new Packet();
					pong.setType("PONG");
					pong.setData("pong");

					sendSelfClient(pong);

					System.out.println("Response: " + pong);

					break;
				}
				default:

					MessageHandler messageHandler = new MessageHandler(chatService, gson);
					if (messageHandler.handleSave(packet)) {
						broadcast(message); // gửi cho tất cả client khác
					}
				}
			}
		} catch (SocketTimeoutException e) {
			System.out.println("Client không gửi dữ liệu, đóng kết nối");
//			close();
		} catch (IOException e) {
			e.printStackTrace();
			close();
		}
	}

	private void sendSelfClient(Packet packet) throws IOException {
		this.out.writeUTF(gson.toJson(packet));
		this.out.flush();

		System.out.println("Sent " + packet);
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

	public static void unConnectClient(String userID) {
		if (userID.equals("all")) {
			List<SocketServer> toRemove = new ArrayList<>();

			synchronized (clients) {
				for (SocketServer client : new ArrayList<>(clients)) {

					if (client.getUserID() != null) {
						client.close();
//	                if (onClientDisConnected != null) {
//	                    onClientDisConnected.accept(client);
//	                }
						toRemove.add(client);
					}
				}

				clients.removeAll(toRemove);
			}

			for (SocketServer client : toRemove) {
				if (onClientDisConnected != null) {
					onClientDisConnected.accept(client);
				}
			}
			return;
		}

		SocketServer target = null;
		for (SocketServer client : clients) {
			if (userID.equals(client.getUserID())) {
				client.close();
				if (onClientDisConnected != null) {
					onClientDisConnected.accept(client);
				}
				target = client;
				break;
			}
		}

		if (target != null) {
			clients.remove(target);
		}
	}

	private static boolean isUserOnline(String userID) {
		return getClients().stream().anyMatch(c -> userID.equals(c.getUserID()));
	}

	public static Vector<SocketServer> getClients() {
		return clients;
	}

	public String getUserID() {
		return userID;
	}

	public String getClientIP() {
		return clientIP;
	}

	public static void setOnClientConnected(Consumer<SocketServer> consumer) {
		onClientConnected = consumer;
	}

	public static void setOffClientDisConnected(Consumer<SocketServer> consumer) {
		onClientDisConnected = consumer;
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

			if (onClientDisConnected != null && userID != null && !userID.isEmpty()) {
				onClientDisConnected.accept(this);
			}

			System.out.println("Client disconnected.");

			// remove from list online
			if (this.userID != null) {
				redisOnlineService.setUserOffline(this.userID);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
