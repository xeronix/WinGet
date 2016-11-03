package com.winget.downloader.main;

import com.winget.downloader.WinGetDownloader;
import com.winget.downloaderimpl.WinGetDownloaderImpl;

public class WinGetMain {
	public static void main(String args[]) {
	//	String url = "https://build.shibboleth.net/nexus/content/repositories/releases/org/opensaml/opensaml-core/3.2.0/opensaml-core-3.2.0.jar";
		String url = "https://build.shibboleth.net/nexus/content/repositories/releases/org/opensaml/";
		
		WinGetDownloader downloader = new WinGetDownloaderImpl(url, null);
		downloader.download();
	}
}
