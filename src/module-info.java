module Chat_Messenger {
	requires javafx.controls;
	requires javafx.fxml;
	requires com.google.gson;
	requires java.persistence;
	requires lombok;
	requires org.mongodb.bson;
	requires org.mongodb.driver.sync.client;
	requires redis.clients.jedis;
	requires mysql.connector.java;
	requires org.hibernate.orm.core;
	requires org.mongodb.driver.core;
	requires jbcrypt;
	requires cloudinary.core;
	requires org.apache.httpcomponents.httpclient;
	requires org.apache.httpcomponents.httpcore;
	requires commons.logging;
	requires org.apache.httpcomponents.httpmime;
	requires javafx.graphics;
	requires org.apache.commons.pool2;
	requires org.slf4j;
	requires gson.javatime.serialisers;
	requires com.gluonhq.charm.glisten;
	requires com.gluonhq.attach.util;
	requires fontawesomefx;
	requires java.desktop;
	requires com.fasterxml.jackson.annotation;

	opens application to javafx.graphics, javafx.fxml;
	opens model to com.google.gson;

	opens controller to javafx.fxml; // 👈 cho phép FXMLLoader truy cập
	opens controller.LoginAndController to javafx.fxml; 
	opens controller.render to javafx.fxml; 

	exports model;
	exports controller; // nếu bạn muốn gói này public cho module khác
	exports controller.LoginAndController;
	exports controller.Common;
	exports controller.render;
}
