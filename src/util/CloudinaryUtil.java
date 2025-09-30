package util;

import com.cloudinary.Cloudinary;

import java.util.Map;

public class CloudinaryUtil {
	private static Cloudinary cloudinary;

	static {
		String cloud_name = System.getenv("cloud_name");
		String api_key = System.getenv("api_key");
		String api_secret = System.getenv("api_secret");

		if (cloud_name == null || api_key == null || api_secret == null) {
			throw new IllegalStateException("Missing Cloudinary environment variables");
		}

		cloudinary = new Cloudinary(Map.of(
			"cloud_name", cloud_name,
			"api_key", api_key,
			"api_secret", api_secret
		));
	}

	public static Cloudinary getCloudinary() {
		return cloudinary;
	}
}
