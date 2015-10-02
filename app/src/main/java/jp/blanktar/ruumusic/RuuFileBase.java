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


public abstract class RuuFileBase implements Comparable{
	final Context context;
	public final File path;


	RuuFileBase(@NonNull Context context, @NonNull String path) throws CanNotOpen{
		try{
			this.path = (new File(path)).getCanonicalFile();
		}catch(IOException e){
			throw new CanNotOpen(path);
		}
		this.context = context;
	}

	@NonNull
	static List<String> getSupportedTypes(){
		if(Build.VERSION.SDK_INT >= 12){
			return Arrays.asList(".flac", ".aac", ".mp3", ".m4a", ".ogg", ".wav", ".3gp");
		}else{
			return Arrays.asList(".mp3", ".m4a", ".ogg", ".wav", ".3gp");
		}
	}

	@NonNull
	static String getRootDirectory(Context context){
		return PreferenceManager.getDefaultSharedPreferences(context).getString("root_directory", "/");
	}

	@NonNull
	public abstract String getFullPath();

	public abstract boolean isDirectory();

	@NonNull
	public String getRuuPath() throws OutOfRootDirectory{
		String root = getRootDirectory(context);
		if(!getFullPath().startsWith(root)){
			throw new OutOfRootDirectory();
		}
		return "/" + getFullPath().substring(root.length());
	}

	@NonNull
	public String getName(){
		return path.getName();
	}

	@NonNull
	public RuuDirectory getParent() throws CanNotOpen{
		String parent = path.getParent();
		if(parent == null){
			throw new CanNotOpen(null);
		}else{
			return RuuDirectory.getInstance(context, parent);
		}
	}

	public boolean equals(@NonNull RuuFileBase file){
		return isDirectory() == file.isDirectory() && path.equals(file.path);
	}

	private int depth(){
		String pathStr = getFullPath();
		return pathStr.length() - pathStr.replaceAll("/", "").length();
	}

	@Override
	public int compareTo(@NonNull Object obj){
		RuuFileBase file = (RuuFileBase)obj;

		if(equals(file)){
			return 0;
		}else if(isDirectory() && file.isDirectory()){
			if(((RuuDirectory)this).contains(file)){
				return -1;
			}else if(((RuuDirectory)file).contains(this)){
				return 1;
			}
		}else if(!isDirectory() && !file.isDirectory()){
			int depthDiff = file.depth() - depth();
			if(depthDiff != 0){
				return depthDiff;
			}
		}
		return path.compareTo(file.path);
	}


	public class OutOfRootDirectory extends Throwable{
	}

	public class CanNotOpen extends Throwable{
		final String path;

		public CanNotOpen(@Nullable String path){
			this.path = path;
		}
	}
}
