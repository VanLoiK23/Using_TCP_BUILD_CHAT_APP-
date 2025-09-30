package model;

import java.time.LocalDateTime;

import org.bson.Document;

import lombok.Data;

@Data
public class Call {
    private String id;
    private String callerId;
    private String receiverId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // "missed", "accepted", "declined"

    public Document toDocument() {
        return new Document("caller_id", callerId)
            .append("receiver_id", receiverId)
            .append("start_time", startTime.toString())
            .append("end_time", endTime.toString())
            .append("status", status);
    }
}