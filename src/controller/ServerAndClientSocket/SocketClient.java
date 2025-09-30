package controller.ServerAndClientSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import model.ChatMessage;
import model.FileInfo;
import model.Packet;
import service.ChatService;

public class SocketClient {
	private static SocketClient instance;
	private Socket socket;
	private DataOutputStream out;
	private DataInputStream in;
	public Gson gson;
//	private OutputStream outputStream;
	private static final String SERVER = "192.168.1.159";//IP server
	private static final int PORT = 12345;
	private Consumer<ChatMessage> messageHandler;
	private Consumer<ChatMessage> fileMessageHandler;
	public BlockingQueue<Packet> responseQueue;
	public ChatService chatService;

	private SocketClient() {
		try {
			socket = new Socket(SERVER, PORT);
//			outputStream = socket.getOutputStream();
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
			gson = Converters.registerAll(new GsonBuilder()).setDateFormat("EEE MMM dd HH:mm:ss z yyyy").create();
			responseQueue = new LinkedBlockingQueue<>();
			chatService=new ChatService();

			listenForMessages();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// create singleton
	public static SocketClient getInstance() {
		if (instance == null) {
			instance = new SocketClient();
		}
		return instance;
	}

	public void sendPacket(Packet packet) throws IOException {
		String json = gson.toJson(packet);
		out.writeUTF(json);
		out.flush();
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

	// receive difference message from other client
	private void listenForMessages() {
		new Thread(() -> {
			try {
				String msg;
				while ((msg = in.readUTF()) != null) {
					Packet packet = gson.fromJson(msg, Packet.class);
					if (packet.getType().equals("MESSAGE")) {

						ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

						if (messageHandler != null) {
							messageHandler.accept(chatMessage);
						}
					} else if (packet.getType().equals("FILE_UPLOAD_RESULT")) {
						ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

						if (fileMessageHandler != null) {
							fileMessageHandler.accept(chatMessage);
						}
					} else {
						responseQueue.offer(packet);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	// chia ra từng phân khúc 4KB và gửi
	private static final int BUFFER_SIZE = 4096;

	// Gửi file từ client
	public void sendFile(File file, String senderId, String receiverId) {
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
				e.printStackTrace();
			}
		}).start();
	}

	public void close() {
		try {
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}