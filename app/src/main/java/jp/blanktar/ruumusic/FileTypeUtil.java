package jp.blanktar.ruumusic;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

import android.support.annotation.NonNull;
import android.os.Build;


class FileTypeUtil {
	public static List<String> getSupportedTypes() {
		if(Build.VERSION.SDK_INT >= 12) {
			return Arrays.asList(".flac", ".aac", ".mp3", ".ogg", ".wav", ".3gp");
		}else {
			return Arrays.asList(".flac", ".mp3", ".ogg", ".wav", ".3gp");
		}
	}
	
	public static boolean isSupported(@NonNull String name) {
		int dotPos = name.lastIndexOf(".");
		return dotPos > 0 && getSupportedTypes().contains(name.substring(dotPos));
	}
	
	public static String stripExtension(@NonNull String path) {
		int pos = path.lastIndexOf(".");
		if(pos <= 0) {
			return null;
		}
		return path.substring(0, pos);
	}
	
	public static ArrayList<File> getMusics(File path) {
		if(path == null || !path.isDirectory()) {
			return null;
		}

		ArrayList<File> result = new ArrayList<>();

		String before = "";
		File[] files = path.listFiles();
		Arrays.sort(files);
		for(File file: files) {
			String name = file.getPath();
			if(file.isFile() && isSupported(name)) {
				name = stripExtension(name);

				if(name != null && !name.equals(before)) {
					result.add(new File(name));
					before = name;
				}
			}
		}

		return result;
	}
	
	public static File detectRealName(File path) {
		if(path == null) {
			return null;
		}
		for(String ext: getSupportedTypes()) {
			File file = new File(path.getPath() + ext);
			if (file.isFile()) {
				return file;
			}
		}
		return null;
	}
}
