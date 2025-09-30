//package model;
//
//import java.util.Date;
//
//import javax.persistence.Column;
//import javax.persistence.Entity;
//import javax.persistence.GeneratedValue;
//import javax.persistence.GenerationType;
//import javax.persistence.Id;
//hibernate
//import javax.persistence.Table;
//import javax.persistence.Temporal;
//import javax.persistence.TemporalType;
//
//import lombok.Data;
//
//
//@Entity
//@Table(name = "user")
//@Data
//public class User {
//	
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int id;
//
//    @Column(name = "username", length = 255, nullable = false)
//    private String username;
//
//    @Column(name = "phone", length = 10, nullable = false)
//    private String phone;
//
//    @Column(name = "email", length = 100, nullable = false)
//    private String email;
//
//    @Column(name = "ngaysinh", nullable = false)
//    @Temporal(TemporalType.DATE)
//    private Date ngaySinh;
//
//    @Column(name = "gender", length = 50, nullable = true)
//    private String gender;
//
//    @Column(name = "password", length = 255, nullable = true)
//    private String password;
//
//    @Column(name = "role", length = 50, nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'khách hàng'")
//    private String role="khách hàng";
//
//    @Column(name = "reset_token_hash", length = 64, nullable = true)
//    private String resetTokenHash;
//
//    @Column(name = "reset_token_expires_at", nullable = true)
//    @Temporal(TemporalType.TIMESTAMP)
//    private Date resetTokenExpiresAt;
//
//    @Column(name = "status", columnDefinition = "ENUM('active','deleted','lock') DEFAULT 'active'")
//    private String status;
//}

package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import org.bson.Document;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class User {
	
	private Id _id; 

	private String username;

	private String email;

	private LocalDate bod;

	private Date createAt;

	private String gender;

	private String password;

	private String role = "khách hàng";

	private String resetTokenHash = null;

	private Date resetTokenExpiresAt = null;

	private String status;// 'online','offline','lock'

	private LocalDateTime lastTimeOnline = null;

	private String avatarUrl;

	public Document toDocument() {

		return new Document("username", username).append("password", password).append("avatarUrl", avatarUrl)
				.append("status", status).append("email", email).append("bod", bod.toString()).append("gender", gender)
				.append("role", role).append("createAt", createAt.toString()).append("resetTokenHash", resetTokenHash)
				.append("resetTokenExpiresAt", (resetTokenExpiresAt != null) ? (resetTokenExpiresAt.toString()) : null)
				.append("lastTimeOnline", (lastTimeOnline != null) ? (lastTimeOnline.toString()) : null);
	}

	public String getAvatarDefault(String sex) {
		if (sex.equals("male")) {
			return "https://i.ibb.co/HfJgxVWN/istockphoto-1934800957-612x612.jpg";
		}
		return "https://i.ibb.co/fzYbSjVG/istockphoto-2014684899-612x612.jpg";
	}

	
	static class Id {
        @SerializedName("$oid")
        private String oid;

        public String getOid() {
            return oid;
        }
    }

    public String getIdHex() {
        return _id != null ? _id.oid : null;
    }
}
