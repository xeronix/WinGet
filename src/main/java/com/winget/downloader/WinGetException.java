package com.winget.downloader;

public class WinGetException extends Exception {
	private static final long serialVersionUID = -1177257038589811252L;

	public WinGetException(String message) {
		super(message);
	}
	
	public WinGetException(String message, Throwable th) {
		super(message, th);
	}
}
