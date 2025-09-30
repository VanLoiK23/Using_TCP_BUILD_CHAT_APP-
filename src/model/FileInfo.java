package model;

import lombok.Data;

@Data
public class FileInfo {

	private String fileName;
	private long fileSize;
	private String urlUpload;
	
	public FileInfo(String fileName, long fileSize, String urlUpload) {
		super();
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.urlUpload = urlUpload;
	}

	
	
}
