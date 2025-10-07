package controller.ServerAndClientSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import model.ChatMessage;
import model.FileInfo;
import model.Packet;
import model.User;
import service.ChatService;

public class SocketClient {
	private static SocketClient instance;
	private Socket socket;
	private DataOutputStream out;
	private DataInputStream in;
	public Gson gson = Converters.registerAll(new GsonBuilder()).setDateFormat("EEE MMM dd HH:mm:ss z yyyy").create();
//	private OutputStream outputStream;
	private static final String SERVER = "192.168.1.220";// IP server
	private static final int PORT = 12345;
	private Consumer<ChatMessage> messageHandler;
	private Consumer<ChatMessage> fileMessageHandler;
	private Consumer<String> onServerDisconnected;
	public BlockingQueue<Packet> responseQueue;
	public ChatService chatService;
	private Thread listenerThread;

	private Boolean isRunning = false;
	private String lastUsername;
	private String lastPassword;
	private boolean isLoggedIn;

	// Hàng đợi nhận phản hồi test connect
	private final BlockingQueue<Packet> heartbeatQueue = new LinkedBlockingQueue<>();

	private boolean waitForPong(long timeoutMs) {
		try {
			Packet packet = instance.heartbeatQueue.poll(timeoutMs, TimeUnit.SECONDS);

			System.out.println("[DEBUG] Received PONG packet: " + packet);

			return packet != null && "PONG".equals(packet.getType());
		} catch (InterruptedException e) {
			return false;
		}
	}

	private SocketClient() throws InterruptedException {
		try {
			socket = new Socket(SERVER, PORT);
//			outputStream = socket.getOutputStream();
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
//			gson = Converters.registerAll(new GsonBuilder()).setDateFormat("EEE MMM dd HH:mm:ss z yyyy").create();
			responseQueue = new LinkedBlockingQueue<>();
			chatService = new ChatService();
			isRunning = true;

			if (isLoggedIn) {
				sendLogin(lastUsername, lastPassword);
			}

			listenForMessages();
		} catch (EOFException e) {
			notifyServerDisconnected("Server closed the connection.");
		} catch (SocketException e) {
			notifyServerDisconnected("Connection lost: " + e.getMessage());
		} catch (IOException e) {
			notifyServerDisconnected("IO error: " + e.getMessage());
		}
	}

	public void sendLogin(String username, String password) throws IOException, InterruptedException {
		this.lastUsername = username;
		this.lastPassword = password;
//	    this.isLoggedIn = true;

		User userRequest = new User();
		userRequest.setUsername(username);
		userRequest.setPassword(password);

		Packet packetRequest = new Packet();
		packetRequest.setType("LOGIN");
		packetRequest.setData(userRequest);
		System.out.println(packetRequest);

		this.isLoggedIn = sendPacket(packetRequest);

		System.out.println(this.isLoggedIn);
	}

	public void logout() throws InterruptedException {
		isLoggedIn = false;
		close();
	}

	public void reConnectToServer() throws InterruptedException {
		close();

		instance = null;
		instance = new SocketClient();

		if (instance.socket != null) {
			System.out.println("Is connect " + instance.socket.isConnected());
		}
	}

	// check server is running
	public boolean checkRunningServer() {
	    try {
	        if (instance.socket == null || instance.socket.isClosed()) return false;

	        Packet ping = new Packet();
	        ping.setType("PING");
	        ping.setData("ping");

	        String json = gson.toJson(ping);
	        instance.out.writeUTF(json);
	        instance.out.flush();

	        boolean pongReceived = waitForPong(3);
			System.out.println("Check PING result: " + pongReceived);
			return pongReceived;
			
	    } catch (Exception e) {
	    	e.printStackTrace();
	        return false;
	    }
	}

	// create singleton
	public static SocketClient getInstance() throws InterruptedException {
		if (instance == null) {
			instance = new SocketClient();
		}
		return instance;
	}

	public synchronized Boolean sendPacket(Packet packet) throws IOException, InterruptedException {
		
		 if (!checkRunningServer()) {
		        notifyServerDisconnected("Server closed the connection.");

		        // Đóng socket cũ và tạo lại
		        reConnectToServer();

		        // Sau khi reconnect, thử gửi lại
		        if (!checkRunningServer()) {
		            System.out.println("❌ Reconnect thất bại");
		            return false;
		        }
		    }

		try {
			String json = gson.toJson(packet);
			instance.out.writeUTF(json);
			instance.out.flush();
			return true;
		} catch (IOException e) {
			notifyServerDisconnected("IO error: " + e.getMessage());
			e.printStackTrace();

			return false;
		}
	}

//	public Packet receivePacket() throws IOException {
//		String json = in.readLine();
//		return gson.fromJson(json, Packet.class);
//	}

	public void setMessageHandler(Consumer<ChatMessage> handler) {
		this.messageHandler = handler;
	}

	public void setFileMessageHandler(Consumer<ChatMessage> handler) {
		this.fileMessageHandler = handler;
	}

	public void setOnServerDisconnected(Consumer<String> handler) {
		this.onServerDisconnected = handler;
	}

	// receive difference message from other client
	private void listenForMessages() {
		if (in == null || listenerThread != null && listenerThread.isAlive()) {
			System.out.println("Response: OK ");
			return;
		}

		listenerThread = new Thread(() -> {

			try {
				String msg;
				while ((msg = instance.in.readUTF()) != null) {
					Packet packet = gson.fromJson(msg, Packet.class);

					System.out.println("Response: " + packet);
					if (packet.getType().equals("MESSAGE")) {

						ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

						if (instance.messageHandler != null) {
							instance.messageHandler.accept(chatMessage);
						}
					} else if (packet.getType().equals("FILE_UPLOAD_RESULT")) {
						ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

						if (instance.fileMessageHandler != null) {
							instance.fileMessageHandler.accept(chatMessage);
						}
					} else if (packet.getType().equals("PONG")) {
						instance.heartbeatQueue.offer(packet);

						System.out.println("[DEBUG] Received PONG packet: " + packet);

					} else {
						instance.responseQueue.offer(packet);
					}
				}
			} catch (EOFException e) {
				e.printStackTrace();
				notifyServerDisconnected("Server closed the connection.");
			} catch (SocketException e) {
				e.printStackTrace();
				notifyServerDisconnected("Connection lost: " + e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				notifyServerDisconnected("IO error: " + e.getMessage());
			}
		});

		listenerThread.setName("MessageListener-" + System.currentTimeMillis());
		listenerThread.start();
	}

	// chia ra từng phân khúc 4KB và gửi
	private static final int BUFFER_SIZE = 4096;

	// Gửi file từ client
	public void sendFile(File file, String senderId, String receiverId) {
		if (checkRunningServer()) {
			new Thread(() -> {
				try {
					FileInputStream fis = new FileInputStream(file);

					ChatMessage chatMessage = new ChatMessage();
					chatMessage.setSenderId(senderId);
					chatMessage.setReceiverId(receiverId);
					chatMessage.setType("file");
					chatMessage.setRead(false);
					chatMessage.setTimestamp(LocalDateTime.now());

					Packet packetRequest = new Packet();
					packetRequest.setType("FILE_META");
					packetRequest.setData(chatMessage);

					// Gửi metadata
					chatMessage.setContent(gson.toJson(new FileInfo(file.getName(), file.length(), null)));

					String json = gson.toJson(packetRequest);
					out.writeUTF(json);
					out.flush();

					// Gửi nội dung file
					// đọc từng byte của file và ghi
					byte[] buffer = new byte[BUFFER_SIZE];
					int count;
					// ghi byte vào outPutStream và send
					while ((count = fis.read(buffer)) > 0) {
						out.write(buffer, 0, count);
					}
					out.flush();
					fis.close();
					System.out.println("File sent: " + file.getName());
				} catch (IOException e) {
					notifyServerDisconnected("IO error: " + e.getMessage());
				}
			}).start();
		} else {
			notifyServerDisconnected("Server closed the connection.");
		}
	}

	private void notifyServerDisconnected(String reason) {
		System.out.println(reason);

		if (onServerDisconnected != null) {
			onServerDisconnected.accept(reason);
		}

	}

	public void close() throws InterruptedException {
		try {
			if (in != null)
				in.close();
			if (socket != null)
				socket.close();
			if (out != null)
				out.close();
			if (listenerThread != null && listenerThread.isAlive()) {
	            listenerThread.join(100); 
	        }


		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}