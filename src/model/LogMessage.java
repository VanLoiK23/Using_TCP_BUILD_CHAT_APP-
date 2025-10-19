package model;

import lombok.Data;

@Data
public class LogMessage {
	private final String message;
    private final String type; // "INFO", "WARN", "ERROR"
}
