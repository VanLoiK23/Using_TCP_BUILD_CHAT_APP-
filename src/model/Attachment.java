package model;

import org.bson.Document;

import lombok.Data;

@Data
public class Attachment {
    private String id;
    private String messageId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private int size;

    public Document toDocument() {
        return new Document("message_id", messageId)
            .append("file_name", fileName)
            .append("file_url", fileUrl)
            .append("file_type", fileType)
            .append("size", size);
    }
}