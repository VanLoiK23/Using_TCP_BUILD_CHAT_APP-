package util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoUtil {
    private static final String USERNAME = "huynhvanloi956_db_user";
    private static final String HOST = "cluster0.c40nhhu.mongodb.net";
    private static final String DB_NAME = "chat_app";

    private static MongoClient client;

    static {
        try {
            String rawPassword = System.getenv("MONGO_PASS");
            if (rawPassword == null) {
                throw new RuntimeException("Biến môi trường MONGO_PASS chưa được thiết lập");
            }

            String encodedPassword = URLEncoder.encode(rawPassword, "UTF-8");
            String uri = String.format(
            	    "mongodb+srv://%s:%s@%s/?retryWrites=true&w=majority&appName=Cluster0",
            	    USERNAME, encodedPassword, HOST
            	);

            client = MongoClients.create(uri);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Lỗi mã hóa mật khẩu MongoDB", e);
        }
    }

    public static MongoDatabase getDatabase() {
        return client.getDatabase(DB_NAME);
    }
}

