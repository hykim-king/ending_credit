package com.endit.cmn;

public class MessageVO {
	
	private String id;
	private String message;
	private String detailMessage;
	
	public MessageVO() {
		super();
	}

	public MessageVO(String id, String message) {
		this(id,message,"No Detail Message.");
	}

	public MessageVO(String id, String message, String detailMessage) {
		super();
		this.id = id;
		this.message = message;
		this.detailMessage = detailMessage;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public String getMessage() {
		return message;
	}


	public void setMessage(String message) {
		this.message = message;
	}


	public String getDetailMessage() {
		return detailMessage;
	}


	public void setDetailMessage(String detailMessage) {
		this.detailMessage = detailMessage;
	}


	@Override
	public String toString() {
		return "MessageVO [id=" + id + ", message=" + message + ", detailMessage=" + detailMessage + "]";
	}

	
}
