package com.salesforce.dataloader.client;

public class SessionInfo {

	private final String sessionId;
	private final long loginTime;
	private final String serverUrl;

	SessionInfo(String sessionId, String server) {
		this.sessionId = sessionId;
		this.loginTime = (sessionId == null ? 0 : System.currentTimeMillis());
		this.serverUrl = server;
	}

	SessionInfo() {
		this(null, null);
	}

	private static final long sessionExpiration = 1000 * 60 * 25;

	private boolean isSessionValid() {
		return this.sessionId != null
				&& System.currentTimeMillis() - this.loginTime < sessionExpiration;
	}

	public String getSessionId() {
		return isSessionValid() ? this.sessionId : null;
	}

	public String getServer() {
		return this.serverUrl;
	}
}
