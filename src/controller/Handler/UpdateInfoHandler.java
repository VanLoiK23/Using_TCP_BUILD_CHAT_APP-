package controller.Handler;

import com.google.gson.Gson;

import model.Packet;
import model.User;
import service.UserService;

public class UpdateInfoHandler {
	private UserService userService;
	private Gson gson;

	public UpdateInfoHandler(UserService userService, Gson gson) {
		this.userService = userService;
		this.gson = gson;
	}

	public Packet handle(Packet packet) {
		User user = gson.fromJson(gson.toJson(packet.getData()), User.class);
		Boolean isSuccess = userService.updateUserById(user.getIdHex(), user);

		Packet response = new Packet();
		response.setType("UPDATE_RESULT");
		response.setData(isSuccess);
		return response;
	}
}