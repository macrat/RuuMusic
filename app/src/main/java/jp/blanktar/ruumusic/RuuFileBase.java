package jp.blanktar.ruumusic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Build;
import android.content.Context;
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

	@NonNull
	static List<String> getSupportedTypes() {
		if (Build.VERSION.SDK_INT >= 12) {
			return Arrays.asList(".flac", ".aac", ".mp3", ".ogg", ".wav", ".3gp");
		} else {
			return Arrays.asList(".flac", ".mp3", ".ogg", ".wav", ".3gp");
		}
	}

	@NonNull
	static String getRootDirectory(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("root_directory", "/");
	}

	@NonNull
	public abstract String getFullPath();

	public abstract boolean isDirectory();

	@NonNull
	public String getRuuPath() throws OutOfRootDirectory {
		String root = getRootDirectory(context);
		if(!getFullPath().startsWith(root)) {
			throw new OutOfRootDirectory();
		}
		return "/" + getFullPath().substring(root.length());
	}

	@NonNull
	public String getName() {
		return path.getName();
	}

	@NonNull
	public RuuDirectory getParent() throws CanNotOpen {
		String parent = path.getParent();
		if(parent == null) {
			throw new CanNotOpen(null);
		}else {
			return new RuuDirectory(context, parent);
		}
	}

	public boolean equals(@NonNull RuuFileBase file) {
		return isDirectory() == file.isDirectory() && path.equals(file.path);
	}

	@Override
	public int compareTo(@NonNull Object file) {
		return getFullPath().compareTo(((RuuFileBase)file).getFullPath());
	}
	
	public class OutOfRootDirectory extends Throwable { }
	
	public class CanNotOpen extends Throwable {
		final String path;
		
		public CanNotOpen(@Nullable String path) {
			this.path = path;
		}
	}
}