package jp.blanktar.ruumusic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

import android.support.annotation.NonNull;
import android.os.Build;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public abstract class RuuFileBase implements Comparable {
	final Context context;
	
	public final File path;

	public RuuFileBase(@NonNull Context context, @NonNull String path) throws CanNotOpen {
		try {
			this.path = (new File(path)).getCanonicalFile();
		}catch(IOException e) {
			throw new CanNotOpen(path);
		}
		this.context = context;
	}

	static List<String> getSupportedTypes() {
		if (Build.VERSION.SDK_INT >= 12) {
			return Arrays.asList(".flac", ".aac", ".mp3", ".ogg", ".wav", ".3gp");
		} else {
			return Arrays.asList(".flac", ".mp3", ".ogg", ".wav", ".3gp");
		}
	}

	static String getRootDirectory(Context context) {
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
		String result = preference.getString("root_directory", "/");
		if(!result.endsWith("/")) {
			result += "/";
		}
		if(!result.startsWith("/")) {
			result = "/" + result;
		}
		return result;
	}
	
	String getRootDirectory() {
		return getRootDirectory(context);
	}

	private String full2ruu(@NonNull String path) throws OutOfRootDirectory {
		String root = getRootDirectory();
		if(!path.startsWith(root)) {
			throw new OutOfRootDirectory(path);
		}
		return "/" + path.substring(root.length());
	}

	public abstract String getFullPath();
	
	public abstract boolean isDirectory();
	
	public String getRuuPath() throws OutOfRootDirectory {
		return full2ruu(getFullPath());
	}

	public String getName() {
		return path.getName();
	}
	
	public RuuDirectory getParent() throws RuuFileBase.CanNotOpen {
		String parent = path.getParent();
		if(parent == null) {
			return null;
		}else {
			return new RuuDirectory(context, path.getParent());
		}
	}
	
	public boolean equals(@NonNull RuuFileBase file) {
		return isDirectory() == file.isDirectory() && path.equals(file.path);
	}
	
	@Override
	public int compareTo(@NonNull Object file) {
		return getFullPath().compareTo(((RuuFileBase)file).getFullPath());
	}
	
	public class OutOfRootDirectory extends Throwable {
		final String path;
		
		public OutOfRootDirectory(@NonNull String path) {
			this.path = path;
		}
	}
	
	public class CanNotOpen extends Throwable {
		final String path;
		
		public CanNotOpen(@NonNull String path) {
			this.path = path;
		}
	}
}
