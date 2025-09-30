package service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import model.FileInfo;
import util.CloudinaryUtil;

public class CloudinaryService {
	private final Cloudinary cloudinary;

    public CloudinaryService() {
        this.cloudinary = CloudinaryUtil.getCloudinary();
    }

    public String uploadFile(byte[] fileBytes,FileInfo fileInfo) {
        try {
        	String safeFileName = Normalizer.normalize(fileInfo.getFileName(), Normalizer.Form.NFD)
        	        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") // bỏ dấu
        	        .replaceAll("[^a-zA-Z0-9._-]", "_"); // chỉ giữ ký tự hợp lệ

        	
        	 Map uploadResult = cloudinary.uploader().upload(fileBytes,
                     ObjectUtils.asMap(
                             "public_id", "uploads/" + safeFileName,
                             "resource_type", "auto",
                             "overwrite", true
                     )
             );


            return uploadResult.get("secure_url").toString(); // URL tải xuống
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
   

//    // Upload nhiều file
//    public List<String> uploadMultipleFiles(List<MultipartFile> files) {
//        List<String> urls = new ArrayList<>();
//        for (MultipartFile file : files) {
//            String url = uploadFile(file);
//            if (url != null) {
//                urls.add(url);
//            }
//        }
//        return urls;
//    }


}
