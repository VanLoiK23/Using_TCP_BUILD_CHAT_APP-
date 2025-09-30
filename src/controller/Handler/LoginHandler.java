package controller.Handler;


import com.google.gson.Gson;

import model.Packet;
import model.User;
import service.UserService;

public class LoginHandler {
    private UserService userService;
    private Gson gson;

    public LoginHandler(UserService userService,Gson gson) {
        this.userService = userService;
        this.gson=gson;
    }

    public Packet handle(Packet packet) {
    	User user = gson.fromJson(gson.toJson(packet.getData()), User.class);

//        User user = (User) packet.getData();
		Boolean isAuthentication = userService.authenticationLogin(user.getUsername(), user.getPassword());

        Packet response = new Packet();
        response.setType("LOGIN_RESULT");
        response.setData(isAuthentication ? userService.getUserByUserName(user.getUsername()) : null);
        return response;
    }
}