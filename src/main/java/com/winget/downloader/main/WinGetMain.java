package com.winget.downloader.main;

import java.util.Map;

import com.winget.downloader.WinGetDownloader;
import com.winget.downloader.WinGetException;
import com.winget.downloader.utils.ParameterParser;
import com.winget.downloaderimpl.WinGetDownloaderImpl;

public class WinGetMain {
	public static void main(String args[]) {
		Map<String, String> paramMap = null;
		
		try {
			paramMap = ParameterParser.createParameterMap(args);
		} catch (WinGetException e) {
			System.out.println("Error :" + e.getMessage());
			return;
		}
		
		WinGetDownloader downloader = new WinGetDownloaderImpl(paramMap);
		downloader.download();
	}
}
