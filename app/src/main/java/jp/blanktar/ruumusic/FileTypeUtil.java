package jp.blanktar.ruumusic;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

import android.os.Build;


public class FileTypeUtil {
	public static List<String> getSupportedTypes() {
		if(Build.VERSION.SDK_INT >= 12) {
			return Arrays.asList(".flac", ".aac", ".mp3", ".ogg", ".wav", ".3gp");
		}else {
			return Arrays.asList(".flac", ".mp3", ".ogg", ".wav", ".3gp");
		}
	}
	
	public static boolean isSupported(String name) {
		int dotPos = name.lastIndexOf(".");
		if(dotPos <= 0) {
			return false;
		}
		return getSupportedTypes().contains(name.substring(dotPos));
	}
	
	public static String stripExtension(String path) {
		int pos = path.lastIndexOf(".");
		if(pos <= 0) {
			return null;
		}
		return path.substring(0, pos);
	}
	
	public static ArrayList<String> getMusics(File path) {
		if(path == null || !path.isDirectory()) {
			return null;
		}

		ArrayList<String> result = new ArrayList<String>();

		String before = "";
		File[] files = path.listFiles();
		Arrays.sort(files);
		for(File file: files) {
			String name = file.getPath();
			if(name.length() > 0 && name.charAt(0) != '.' && file.isFile() && isSupported(name)) {
				name = stripExtension(name);

				if(!name.equals(before)) {
					result.add(name);
					before = name;
				}
			}
		}

		return result;
	}
	
	public static String detectRealName(String path) {
		for(String ext: getSupportedTypes()) {
			File file = new File(path + ext);
			if (file.isFile()) {
				return file.getPath();
			}
		}
		return "";
	}
}
