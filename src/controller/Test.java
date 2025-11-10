package controller;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Test {
	public static void main(String[] args) throws Exception {
        int port = 5000;
        String groupAddress = "239.227.227.40";
        String localAddress = "192.168.1.52"; // IP của máy thật

        NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getByName(localAddress));
        InetAddress group = InetAddress.getByName(groupAddress);

        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)
            .setOption(StandardSocketOptions.SO_REUSEADDR, true)
            .bind(new InetSocketAddress(port));

        channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
        channel.join(group, ni);

        ByteBuffer buffer = ByteBuffer.allocate(4096);
        System.out.println("Đang lắng nghe multicast trên " + groupAddress + ":" + port);

        while (true) {
            buffer.clear();
            SocketAddress sender = channel.receive(buffer);
            buffer.flip();
            byte[] data = new byte[buffer.limit()];
            buffer.get(data);
            System.out.println("Nhận từ " + sender + ": " + new String(data));
        }
    }

}
