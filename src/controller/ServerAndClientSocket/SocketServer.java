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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import controller.Handler.FileHandler;
import controller.Handler.GroupHandler;
import controller.Handler.LoginHandler;
import controller.Handler.MessageHandler;
import controller.Handler.RegisterHandler;
import model.ChatMessage;
import model.FileInfo;
import model.Group;
import model.LogMessage;
import model.Packet;
import model.User;
import service.ChatService;
import service.CloudinaryService;
import service.GroupService;
import service.RedisOnlineService;
import service.UserService;
import util.RedisUtil;

//Client handle
public class SocketServer implements Runnable {
	private Socket socket;
//	private MulticastSocket multicastSocket;
	private DataInputStream in;
	private DataOutputStream out;
//	private InputStream inputStream;
	public Gson gson;

	private RedisOnlineService redisOnlineService;
	private UserService userService;
	private CloudinaryService cloudinaryService;
	private ChatService chatService;
	private GroupService groupService;
	private String userID;
	private String clientIP;
    private static final Map<String, Group> groupHashMap = new ConcurrentHashMap<>();

	// Danh sách tất cả client đang kết nối (dùng Vector để quản lý)
	private static Vector<SocketServer> clients = new Vector<>();

	private static Consumer<SocketServer> onClientConnected;
	private static Consumer<SocketServer> onClientDisConnected;
	private static Consumer<LogMessage> onLogging;

	public SocketServer(Socket socket) {
		try {
			this.socket = socket;
//			this.inputStream = socket.getInputStream();
//			this.multicastSocket = new MulticastSocket();
			this.in = new DataInputStream(socket.getInputStream());
			this.out = new DataOutputStream(socket.getOutputStream());
			gson = Converters.registerAll(new GsonBuilder()).setDateFormat("EEE MMM dd HH:mm:ss z yyyy").create();
			this.redisOnlineService = new RedisOnlineService(RedisUtil.getClient());
			this.userService = new UserService();
			this.cloudinaryService = new CloudinaryService();
			this.chatService = new ChatService();
			this.groupService = new GroupService();

			clients.add(this); // thêm client vào danh sách
			updateHashMap();

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
					setLogging("Yêu cầu: LOGIN từ " + socket.getInetAddress() + ":" + socket.getPort(), "INFO");

					User userCheck = gson.fromJson(gson.toJson(packet.getData()), User.class);

					User userResult = userService.getUserByUserName(userCheck.getUsername());
					if (userResult != null && isUserOnline(userResult.getIdHex())) {
						Packet response = new Packet();
						response.setType("DUPLICATE_LOGIN");
						response.setData(null);

						sendSelfClient(response);

						setLogging("Phản hồi tới yêu cầu LOGIN từ " + socket.getInetAddress() + ":" + socket.getPort()
								+ " :" + response.getType(), "WARNING");
					} else {

						LoginHandler loginHandler = new LoginHandler(userService, gson);
						Packet loginResponse = loginHandler.handle(packet);
						// if login success add to list online
						User user = gson.fromJson(gson.toJson(loginResponse.getData()), User.class);
						if (user != null) {
							this.userID = user.getIdHex();
							if (RedisUtil.isAlive()) {
								redisOnlineService.setUserOnline(this.userID);

								// only login success
								if (onClientConnected != null) {
									onClientConnected.accept(this);
								}
								setLogging("Phản hồi tới yêu cầu LOGIN từ " + socket.getInetAddress() + ":"
										+ socket.getPort() + " :" + "SUCCESS", "INFO");
							}
						} else {
							setLogging("Phản hồi tới yêu cầu LOGIN từ " + socket.getInetAddress() + ":"
									+ socket.getPort() + " :" + "FAIL", "WARNING");
						}

						sendSelfClient(loginResponse);
					}
					break;
				}
				case "REGISTER": {
					User userCheck = gson.fromJson(gson.toJson(packet.getData()), User.class);
					setLogging("Yêu cầu: REGISTER từ " + socket.getInetAddress() + ":" + socket.getPort(), "INFO");

					User userResult = userService.getUserByUserName(userCheck.getUsername());

					if (userResult != null) {
						Packet response = new Packet();
						response.setType("DUPLICATE_UserName");
						response.setData(null);

						sendSelfClient(response);
						setLogging("Phản hồi tới yêu cầu REGISTER từ " + socket.getInetAddress() + ":"
								+ socket.getPort() + " :" + response.getType(), "WARNING");
					} else {
						RegisterHandler registerHandler = new RegisterHandler(userService, gson);
						Packet registerReponse = registerHandler.handle(packet);
						sendSelfClient(registerReponse);
						setLogging("Phản hồi tới yêu cầu REGISTER từ " + socket.getInetAddress() + ":"
								+ socket.getPort() + " :" + "SUCCESS", "INFO");
					}
					break;
				}
				case "FILE_META": {
					ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

					FileInfo meta = gson.fromJson(chatMessage.getContent(), FileInfo.class);
					setLogging("Yêu cầu: SEND FILE từ " + socket.getInetAddress() + ":" + socket.getPort(), "INFO");

//					new Thread(() -> {
					FileHandler fileHandler = new FileHandler(cloudinaryService, gson, in, chatService);
					fileHandler.setPacketCallback(packetReponse -> {
						broadcast(gson.toJson(packetReponse));

						System.out.println("Reponse: " + packetReponse);
						setLogging("Phản hồi tới yêu cầu SEND FILE từ " + socket.getInetAddress() + ":"
								+ socket.getPort() + " :" + "SUCCESS", "INFO");
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
				case "CREATE_GROUP": {
					Group group = gson.fromJson(gson.toJson(packet.getData()), Group.class);

					GroupHandler groupHandler = new GroupHandler();
					String newKeyGroup = groupHandler.createGroup(group, getListKeyGroup(), groupService);

					// ????
					group.setMulticastIP(newKeyGroup);

					// Thêm vào hash map
					groupHashMap.put(newKeyGroup.trim(), group);

					Packet packetResponse = new Packet();
					packetResponse.setType("CREATE_RESULT");
					packetResponse.setData(newKeyGroup);

					sendSelfClient(packetResponse);

					break;
				}
				case "JOIN_GROUP": {
					setLogging("Yêu cầu: JOIN GROUP từ " + socket.getInetAddress() + ":" + socket.getPort(), "INFO");

					Group group = gson.fromJson(gson.toJson(packet.getData()), Group.class);

					Group groupAdjusted = updateListMember(group, true);
					
					groupAdjusted.set_id(group.get_id());

					GroupHandler groupHandler = new GroupHandler();
					groupHandler.updateGroup(groupAdjusted, groupService);

//					updateListMember(group);

					Packet packetResponse = new Packet();
					packetResponse.setType("JOIN_RESULT");
					packetResponse.setData(true);

					setLogging("Phản hồi: JOIN GROUP từ " + socket.getInetAddress() + ":" + socket.getPort() + " :"
							+ "SUCCESS", "INFO");

					sendSelfClient(packetResponse);

					// only send to online user in group INFO JOIN GR
					Packet packetResponseToGR = new Packet();
					packetResponseToGR.setType("USER_JoinOrLeave");
					packetResponseToGR.setData(this.userID + "_" + group.getIdHex());

					String messageInfo = gson.toJson(packetResponseToGR);

					broadcastToGroup(messageInfo, group.getMembers());

					break;
				}
				case "LEAVE_GROUP": {
					Group group = gson.fromJson(gson.toJson(packet.getData()), Group.class);

					Group groupAdjusted = updateListMember(group, false);
					
					groupAdjusted.set_id(group.get_id());

					setLogging("Yêu cầu: LEAVE GROUP từ " + socket.getInetAddress() + ":" + socket.getPort(), "INFO");

					GroupHandler groupHandler = new GroupHandler();
					groupHandler.updateGroup(groupAdjusted, groupService);

//					updateListMember(group);

					// response user leave group
					Packet packetResponse = new Packet();
					packetResponse.setType("LEAVE_RESULT");
					packetResponse.setData(true);

					setLogging("Phản hồi: LEAVE GROUP từ " + socket.getInetAddress() + ":" + socket.getPort() + " :"
							+ "SUCCESS", "INFO");

					sendSelfClient(packetResponse);

					// only send to online user in group INFO LEAVE GR
					Packet packetResponseToGR = new Packet();
					packetResponseToGR.setType("USER_JoinOrLeave");
					packetResponseToGR.setData(this.userID + "_" + group.getIdHex());

					String messageInfo = gson.toJson(packetResponseToGR);

					broadcastToGroup(messageInfo, group.getMembers());

					break;
				}
				case "MESSAGE_PRIVATE": {
					ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

					setLogging("Yêu cầu: SEND TEXT TO PRIVATE từ " + socket.getInetAddress() + ":" + socket.getPort()
							+ " tới " + getIPClient(chatMessage.getReceiverId()), "INFO");

					MessageHandler messageHandler = new MessageHandler(chatService, gson);

					packet.setType("MESSAGE");

					if (messageHandler.handleSave(packet)) {
						sendPrivate(gson.toJson(packet), chatMessage.getReceiverId()); // gửi cho client khác cụ thể
					}

					setLogging("Phản hồi tới yêu cầu SEND TEXT TO PRIVATE từ " + socket.getInetAddress() + ":"
							+ socket.getPort() + " tới " + getIPClient(chatMessage.getReceiverId()) + " :" + "SUCCESS",
							"INFO");

					break;
				}
				default:
					setLogging("Yêu cầu: SEND TEXT TO ALL từ " + socket.getInetAddress() + ":" + socket.getPort(),
							"INFO");

					MessageHandler messageHandler = new MessageHandler(chatService, gson);
					if (messageHandler.handleSave(packet)) {
						broadcast(message); // gửi cho tất cả client khác
					}

					setLogging("Phản hồi tới yêu cầu SEND TEXT TO ALL từ " + socket.getInetAddress() + ":"
							+ socket.getPort() + " :" + "SUCCESS", "INFO");
				}
			}
		}
//		catch (SocketTimeoutException e) {
//			System.out.println("Client không gửi dữ liệu, đóng kết nối");
////			close();
//		}
		catch (IOException e) {
			e.printStackTrace();
			close();
		}
	}

	private void sendSelfClient(Packet packet) throws IOException {
		this.out.writeUTF(gson.toJson(packet));
		this.out.flush();

		System.out.println("Sent " + packet);
	}

	private void broadcastToGroup(String message, List<String> groupMembers) {
		Set<String> memberSet = new HashSet<>(groupMembers);

		for (SocketServer client : clients) {
			String targetId = client.getUserID();

			if (targetId != null && memberSet.contains(targetId)) {
				if (!targetId.equals(this.userID)) { // không gửi cho chính mình
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
	}

	// Gửi tin nhắn cho tất cả client trong Vector
	private void broadcast(String message) {
		for (SocketServer client : clients) {
			if (client.getUserID() != null) {
				if (client != this && !client.getUserID().equals(this.userID)) {
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
	}

	private void sendPrivate(String message, String targetUserId) {
		for (SocketServer client : clients) {
			if (client == null || client.getUserID() == null)
				continue;

			if (client.getUserID().equals(targetUserId)) {
				try {
					if (client.socket.isClosed())
						return;

					client.out.writeUTF(message);
					client.out.flush();

					System.out.println("Sent succes" + message);

				} catch (IOException e) {
					client.close();
				}
				return;
			}
		}

//	    // Nếu không tìm thấy người nhận
//	    try {
//	        this.out.writeUTF("Người dùng " + targetUserId + " không online hoặc không tồn tại!");
//	        this.out.flush();
//	    } catch (IOException e) {
//	        e.printStackTrace();
//	    }
	}

	private String getIPClient(String userId) {
		for (SocketServer client : clients) {
			if (client.getUserID() != null) {
				if (client.getUserID().equals(userId)) {
					return client.clientIP;
				}
			}
		}

		return null;
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

	private Group updateListMember(Group group, Boolean isJoin) {
		Group targetGroup = groupHashMap.get(group.getMulticastIP().trim());
		if (targetGroup != null) {
			List<String> members = targetGroup.getMembers();
			if (members == null) {
				members = new ArrayList<>();
				targetGroup.setMembers(members);
			}

			if (isJoin) {
				if (!members.contains(this.userID)) { // tránh trùng thành viên
					members.add(this.userID);
					System.out.println("Thêm user " + this.userID + " vào nhóm " + group.getMulticastIP());
				}
			} else {
				members.remove(this.userID);
			}

			return targetGroup;
		} else {
			System.out.println("Nhóm với IP " + group.getMulticastIP() + " không tồn tại!");

			return null;
		}
	}

	private void updateHashMap() {
		List<Group> allGroups = groupService.getAllGroups();
		if (allGroups != null && !allGroups.isEmpty())
			for (Group gr : allGroups)
				groupHashMap.put(gr.getMulticastIP().trim(), gr);
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

	private void setLogging(String msg, String type) {
		if (onLogging != null) {
			LogMessage logMessage = new LogMessage(msg, type);

			onLogging.accept(logMessage);
		}
	}

	public static void setOnLogging(Consumer<LogMessage> consumer) {
		onLogging = consumer;
	}

	public static void setOnClientConnected(Consumer<SocketServer> consumer) {
		onClientConnected = consumer;
	}

	public static void setOffClientDisConnected(Consumer<SocketServer> consumer) {
		onClientDisConnected = consumer;
	}

	private List<String> getListKeyGroup() {
		if (groupHashMap != null && !groupHashMap.isEmpty()) {
			Set<String> keys = groupHashMap.keySet();

			List<String> keyList = new ArrayList<String>();

			for (String key : keys) {
				keyList.add(key);
			}

			return keyList;
		}

		return null;
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
