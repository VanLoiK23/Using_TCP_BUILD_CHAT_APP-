package controller.Handler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;

import model.Group;
import service.GroupService;

public class GroupHandler {

	public String createGroup(Group groupPacket, List<String> groups, GroupService groupService)
			throws IOException {

		String ip = generateUniqueGroupIP(groups);
		groupPacket.setMulticastIP(ip);

		groupService.saveGroup(groupPacket);

		InetAddress groupAddress = InetAddress.getByName(ip);
//		socket.joinGroup(groupAddress);

		return ip;
	}

	public void updateGroup(Group group, GroupService groupService) throws IOException {

		groupService.updateGroup(group);

		InetAddress groupAddress = InetAddress.getByName(group.getMulticastIP());
//		socket.joinGroup(groupAddress);
	}

	private String generateUniqueGroupIP(List<String> groups) {
		Random rand = new Random();
		String ip;

//	    int firstOctet = 224 + rand.nextInt(16);   224->239
		if (groups == null) {
			ip = "239." + rand.nextInt(255) + "." + rand.nextInt(255) + "." + rand.nextInt(255);
		}

		do {
			ip = "239." + rand.nextInt(255) + "." + rand.nextInt(255) + "." + rand.nextInt(255);
		} while (groups != null && groups.contains(ip));
		return ip;
	}

}
