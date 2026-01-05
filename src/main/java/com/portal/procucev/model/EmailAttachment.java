package com.portal.procucev.model;

public class EmailAttachment {

	private String fileName; // report.xlsx
	private String contentType; // application/vnd.openxmlformats-officedocument.spreadsheetml.sheet

	// Base64 string coming from request
	private String fileData;

	// Internal use only (decoded bytes)
	private transient byte[] decodedFileData;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getFileData() {
		return fileData;
	}

	public void setFileData(String fileData) {
		this.fileData = fileData;
	}

	public byte[] getDecodedFileData() {
		return decodedFileData;
	}

	public void setDecodedFileData(byte[] decodedFileData) {
		this.decodedFileData = decodedFileData;
	}

}
