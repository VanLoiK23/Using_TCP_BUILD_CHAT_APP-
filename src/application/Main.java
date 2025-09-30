package application;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import controller.ServerAndClientSocket.SocketServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
	private static final int PORT = 12345;

	@Override
	public void start(Stage stage) {		
		startServer();

		try {

			Parent root = FXMLLoader.load(getClass().getResource("/view/LoginAndRegister/FormLoginAndRegister.fxml"));
//			--module-path "C:\Users\DELL\Downloads\openjfx-22_windows-x64_bin-sdk\javafx-sdk-22\lib" --add-modules javafx.controls,javafx.fxml
			Scene scene = new Scene(root);
			scene.setFill(Color.TRANSPARENT);

			stage.setScene(scene);
			stage.setTitle("Hệ thống Message");
			stage.initStyle(StageStyle.TRANSPARENT);
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void startServer() {
		Thread serverThread = new Thread(() -> {
			ExecutorService pool = null;
			try (ServerSocket serverSocket = new ServerSocket(PORT)) {
				System.out.println("Server started on port " + PORT);

				// max 100 thread
				pool = Executors.newCachedThreadPool(); // số lượng ko ổn định các thread execute // và xog thì reuse

				while (true) {
					Socket socket = serverSocket.accept();

					// each client tương ứng với mỗi thread nếu nhiều client thì nhiều thread -> tốn
					// tài nguyên
//					ServerController clientHandler = new ServerController(socket);
//					new Thread(clientHandler).start();

					pool.execute(new SocketServer(socket));
				}

			} catch (IOException e) {
				e.printStackTrace();
			} 
		});
		serverThread.setDaemon(true); // để khi tắt app thì server cũng tắt
		serverThread.start();
	}

	public static void main(String[] args) throws IOException {
		launch(args);
	}
}
