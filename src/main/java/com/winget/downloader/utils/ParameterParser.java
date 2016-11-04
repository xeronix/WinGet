package com.winget.downloader.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.winget.downloader.WinGetException;

public class ParameterParser {
	public static final String KEY_URL = "KEY_URL";
	public static final String KEY_RECURSE = "KEY_RECURSE";
	public static final String KEY_OUTPUT_DIR = "KEY_OUTPUT_DIR";
	public static final String KEY_VERBOSE = "KEY_VERBOSE";
	
	private static final String ARG_HELP = "-help";
	private static final String ARG_URL = "-u";
	private static final String ARG_RECURSE = "-r";
	private static final String ARG_OUTPUT_DIR = "-o";
	private static final String ARG_VERBOSE = "-v";
	
	private static final String HELP_TEXT = "WinGet is an HTTP Client based Java Tool to mirror "
			+ "all files in an online repository or website recursively to a local directory. "
			+ "Mirroring will maintain the directory structure of the files.\n" +
			"Usage : winget.bat -help -u <string> -r <true/false> -o <string> -v <true/false>\n" +
			"-help : Display help text" +
			"-u : URL\n" + 
			"-r : Flag to enable/disable recursively mirroring child URLs [Optional, default is false]\n" + 
			"-o : Location of output directory to save downloaded files [default is WinGetOutpuDir]\n" +
			"-v : Flag to enable/disable verbose output [Optional, default is false]\n";
	
	private static final String DEFAULT_OUTPUT_DIR = "WinGetOutputDir";
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	
	public static Map<String, String> createParameterMap(String args[]) throws WinGetException {
		Map<String, String> paramMap = new HashMap<String, String>();
		
		paramMap.put(KEY_RECURSE, FALSE);
		paramMap.put(KEY_OUTPUT_DIR, DEFAULT_OUTPUT_DIR);
		paramMap.put(KEY_VERBOSE, FALSE);

		for (int i = 0; i < args.length-1; i+=2) {
			switch(args[i]) {
			case ARG_HELP:
					System.out.println(HELP_TEXT);
					System.exit(0);
			case ARG_URL:
				paramMap.put(KEY_URL, args[i+1].trim());
				break;
			case ARG_RECURSE:
				paramMap.put(KEY_RECURSE, args[i+1].trim());
				break;
			case ARG_OUTPUT_DIR:
				paramMap.put(KEY_OUTPUT_DIR, args[i+1].trim());
				break;
			case ARG_VERBOSE:
				paramMap.put(KEY_VERBOSE, args[i+1].trim());
				break;
			default:
				throw new WinGetException("Invalid argument " + args[i]);
			}
		}
		
		validateParamMap(paramMap);
		
		return paramMap;
	}
	
	private static void validateParamMap(Map<String, String> paramMap) throws WinGetException {
		String url = paramMap.get(KEY_URL);
		
		if (url == null || url.isEmpty()) {
			throw new WinGetException("Specify a valid url with -u parameter");
		}
		
		String recurseFlag = paramMap.get(KEY_RECURSE);
		
		if (!recurseFlag.equalsIgnoreCase(TRUE) && !recurseFlag.equalsIgnoreCase(FALSE)) {
			throw new WinGetException("Invalid value [" + recurseFlag + "] provided for parameter " + ARG_RECURSE);
		}
		
		String verboseFlag = paramMap.get(KEY_VERBOSE);

		if (!verboseFlag.equalsIgnoreCase(TRUE) && !verboseFlag.equalsIgnoreCase(FALSE)) {
			throw new WinGetException("Invalid value [" + verboseFlag + "] provided for parameter " + ARG_VERBOSE);
		}

		String outputDir = paramMap.get(KEY_OUTPUT_DIR);
		
		File outputDirFile = new File(outputDir);
		
		/**
		 * Create output directory if it does not exist and
		 * clear its content
		 */
		if (!outputDirFile.exists()) {
			System.out.println("Creating Directory : " + outputDir);
			outputDirFile.mkdirs();
		} else {
			System.out.println("Clearing Directory : " + outputDir);

			File childFiles[] = outputDirFile.listFiles();
			
			for (File deletableFile : childFiles) {
				try {
					FileUtils.forceDelete(deletableFile);
				} catch (IOException e) {
					throw new WinGetException(e.getMessage(), e);
				}
			}
		}
	}
}
