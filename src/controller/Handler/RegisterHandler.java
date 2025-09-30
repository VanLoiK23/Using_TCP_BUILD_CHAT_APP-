package controller.Handler;

import com.google.gson.Gson;

import model.Packet;
import model.User;
import service.UserService;

public class RegisterHandler {
    private UserService userService;
    private Gson gson;


    public RegisterHandler(UserService userService,Gson gson) {
        this.userService = userService;
        this.gson=gson;
    }

    public Packet handle(Packet packet) {
        User user =  gson.fromJson(gson.toJson(packet.getData()), User.class);
		String userID = userService.insertOne(user);

        Packet response = new Packet();
        response.setType("REGISTER_RESULT");
        response.setData(userID);
        return response;
    }
}