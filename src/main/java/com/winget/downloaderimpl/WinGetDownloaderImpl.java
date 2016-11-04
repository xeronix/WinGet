package com.winget.downloaderimpl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import com.winget.downloader.WinGetDownloader;
import com.winget.downloader.utils.HTTPContentType;

public class WinGetDownloaderImpl implements WinGetDownloader {

	private final String rootUrl;
	
	private final ConcurrentLinkedQueue<String> urlQueue;
	
	private final HashMap<String, String> parameterMap;
	
	private final CloseableHttpClient httpClient;

	private final String HTTP_PREFIX = "http://";
	
	private final String HTTPS_PREFIX = "https://";
		
	private final static Logger logger = Logger.getLogger(WinGetDownloaderImpl.class);
	
	// Thread Safe Set
	private final Set<String> processedUrls;
	
	public WinGetDownloaderImpl(final String rootUrl, final HashMap<String, String> parameterMap) {
		urlQueue = new ConcurrentLinkedQueue<String>();
		urlQueue.add(rootUrl.trim());

		this.rootUrl = rootUrl;
		this.parameterMap = parameterMap;

		PoolingHttpClientConnectionManager connectionMgr = new PoolingHttpClientConnectionManager();
		connectionMgr.setMaxTotal(200);
		connectionMgr.setDefaultMaxPerRoute(20);
		
		httpClient = HttpClients.custom().setConnectionManager(connectionMgr).build();

		processedUrls = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	}
	
	public void download() {
		Set<Thread> threadSet = new HashSet<Thread>();
		
		while (!stopProcessing(threadSet)) {
			String url = urlQueue.poll();
			
			// Make sure no URL is processed more than once
			if (processedUrls.contains(url)) {
				continue;
			}
						
			processedUrls.add(url);
			
			DownloaderThread thread = new DownloaderThread(httpClient, url);
			thread.start();
			threadSet.add(thread);
		}
	}
	
	/*
	 * We have a set of started threads. If urlQueue is empty and if any thread is
	 * running then that thread might add urls to urlQueue, so we keep waiting
	 * for that running thread to join back.
	 */
	private boolean stopProcessing(Set<Thread> threadSet) {
		if (!urlQueue.isEmpty()) {
			return false;
		}
		
		Iterator<Thread> it = threadSet.iterator();
		
		while (it.hasNext()) {
			Thread thread = it.next();
			if (thread.isAlive()) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
				}
			}
			
			it.remove();
		}
		
		if (!urlQueue.isEmpty()) {
			return false;
		}
		
		return true;
	}
	
	private List<String> getChildUrls(final HttpEntity entity) {
		InputStream is = null;
		BufferedReader br = null;
		
		String line;
		List<String> childUrls = new ArrayList<String>();

		try {
			is = entity.getContent();
			br = new BufferedReader(new InputStreamReader(is));
			
			while ((line = br.readLine()) != null) {
				try {
					if (line.contains(HTTPS_PREFIX)) {
						line = line.substring(line.indexOf(HTTPS_PREFIX));

						String url = line.substring(0, line.indexOf("\""));
						url.trim();

						if (url.startsWith(rootUrl) && !processedUrls.contains(url)) {
							childUrls.add(url);
						}
					} else if (line.contains(HTTP_PREFIX)) {
						line = line.substring(line.indexOf(HTTP_PREFIX));

						String url = line.substring(0, line.indexOf("\""));
						url.trim();

						if (url.startsWith(rootUrl) && !processedUrls.contains(url)) {
							childUrls.add(url);
						}
					}
				} catch (Exception e) {
					logger.error("Error retreiving URL from page line : " + line + "\n" + e.getMessage(), e);
				}
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					is = null;
				}
			}
			
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					br = null;
				}
			}
		}

		return childUrls;
	}

	private void downloadFile(final HttpEntity entity, final String url) {
		String FileRelativePath;
		
		if (rootUrl.endsWith("/")) {
			FileRelativePath = url.substring(rootUrl.length());
		} else {
			FileRelativePath = url.substring(rootUrl.length()-1);
		}
		
		String filePath = "E:\\Softwares\\OpenSAML\\" + FileRelativePath;
		
		System.out.println("Downloading file [" + filePath + "] from [" + url + "] ...");
		
		InputStream is = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		
		File file = new File(filePath);
		file.getParentFile().mkdirs();
		
		try {
			is = entity.getContent();
			bis = new BufferedInputStream(is);
			bos = new BufferedOutputStream(new FileOutputStream(file));
			
			int byteData;

			while ((byteData = bis.read()) != -1) {
				bos.write(byteData);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					is = null;
				}
			}
			
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					bis = null;
				}
			}
			
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					bos = null;
				}
			}
		}
	}
	
	class DownloaderThread extends Thread {
		private final CloseableHttpClient httpClient;
		private final HttpContext context;
		private final String url;
		
		public DownloaderThread(CloseableHttpClient httpClient, String url) {
			this.httpClient = httpClient;
			this.context = HttpClientContext.create();
			this.url = url;
		}

		@Override
		public void run() {
			System.out.println("Processing URL : " + url);

			CloseableHttpResponse response = null;
			
			try {
				HttpGet httpget = new HttpGet(url);
				response = httpClient.execute(httpget, context);
				HttpEntity entity = response.getEntity();

				if (entity != null) {
					String contentType = entity.getContentType().getValue();
					String contentTypePrefix = contentType.substring(0, contentType.indexOf("/"));

					if (contentTypePrefix.equalsIgnoreCase(HTTPContentType.TEXT)) {
						List<String> childUrls = getChildUrls(entity);
						urlQueue.addAll(childUrls);
					} else {
						downloadFile(entity, url);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				try {
					response.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					response = null;
				}
			}
		}
	}
}
