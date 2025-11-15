package controller.Handler;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.function.Consumer;

import com.google.gson.Gson;

import model.ChatMessage;
import model.FileInfo;
import model.Packet;
import service.ChatService;
import service.CloudinaryService;

public class FileHandler {
	private static final int BUFFER_SIZE = 4096;

	private CloudinaryService cloudinaryService;
	private DataInputStream inputStream;
	private Consumer<Packet> packetCallback;
	private ChatService chatService;
	private Gson gson;

	public FileHandler(CloudinaryService cloudinaryService, Gson gson, DataInputStream inputStream,ChatService chatService) {
		this.cloudinaryService = cloudinaryService;
		this.inputStream = inputStream;
		this.gson = gson;
		this.chatService=chatService;

	}

	public void setPacketCallback(Consumer<Packet> handler) {
		this.packetCallback = handler;
	}

	public void receiveFile(FileInfo fileInfo, ChatMessage chatMessage) {

		try {
			// Nhận metadata
			if (fileInfo != null) {
				String fileName = fileInfo.getFileName();
				long fileSize = fileInfo.getFileSize();

				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[BUFFER_SIZE];
				long totalReceived = 0;

				// ghi content file vao byteArray
				while (totalReceived < fileSize) {
					int count = inputStream.read(buffer);
					if (count == -1)
						break;
					byteArrayOutputStream.write(buffer, 0, count);
					totalReceived += count;
				}
				byte[] filebytes = byteArrayOutputStream.toByteArray();

				String uploadURL = cloudinaryService.uploadFile(filebytes, fileInfo);

				if (uploadURL != null && !uploadURL.isEmpty()) {
					Packet packet = new Packet();
					packet.setType("FILE_UPLOAD_RESULT");
					System.out.println(uploadURL);

					//file name and url download file from clound
					FileInfo fileInfoReponse = new FileInfo(fileName, 0, uploadURL);
					chatMessage.setContent(gson.toJson(fileInfoReponse));
					
					
					// tham chiếu đến chatMessage ban đầu đổi content theo cùng trỏ đến cùng một đối tượng ChatMessage trong heap.
					chatService.saveMessage(chatMessage);
					packet.setData(chatMessage);

					if (packetCallback != null) {
						packetCallback.accept(packet);
					}
				}
				byteArrayOutputStream.close();
				System.out.println("File received: " + fileName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
