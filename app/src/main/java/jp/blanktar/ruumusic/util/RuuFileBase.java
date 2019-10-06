package jp.blanktar.ruumusic.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat.MediaItem;


public abstract class RuuFileBase implements Comparable<RuuFileBase>{
	@NonNull final Context context;
	@NonNull final String path;
	@NonNull final String name;
	@Nullable final RuuDirectory parent;


	RuuFileBase(@NonNull Context context, @Nullable RuuDirectory parent, @NonNull String path){
		int slashpos = path.lastIndexOf("/");
		if(slashpos < 0){
			this.path = "/";
			this.name = "";
		}else{
			this.path = path.substring(0, slashpos+1);
			this.name = path.substring(slashpos+1);
		}
		this.parent = parent;
		this.context = context;
	}

	@NonNull
	public abstract String getFullPath();

	public abstract boolean isDirectory();

	@NonNull
	public String getRuuPath() throws OutOfRootDirectory{
		RuuDirectory rootDir = new Preference(context).RootDirectory.get();
		String root = "/";
		if(rootDir != null){
			root = rootDir.getFullPath();
		}
		if(!getFullPath().startsWith(root)){
			throw new OutOfRootDirectory();
		}
		return "/" + getFullPath().substring(root.length());
	}

	@NonNull
	public String getName(){
		return name;
	}

	@NonNull
	public RuuDirectory getParent() throws OutOfRootDirectory{
		if(parent == null){
			throw new OutOfRootDirectory();
		}
		return parent;
	}

	@NonNull
	abstract public Uri toUri();

	@NonNull
	abstract public Intent toIntent();

	public boolean equals(@NonNull RuuFileBase file){
		return isDirectory() == file.isDirectory() && getFullPath().equals(file.getFullPath());
	}

	private int depth(){
		int count = 0;
		for(char x: getFullPath().toCharArray()){
			if(x == '/'){
				count++;
			}
		}
		return count;
	}

	@Override
	public int compareTo(@NonNull RuuFileBase file){
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
		return getFullPath().compareTo(file.getFullPath());
	}

	@NonNull
	abstract public MediaItem toMediaItem();

	public static String getDataVersion(Context context) {
		return Build.VERSION.SDK_INT < 12 ? null : MediaStore.getVersion(context);
	}


	public static class OutOfRootDirectory extends Throwable{
	}

	public static class NotFound extends Throwable{
		final public String path;

		public NotFound(@Nullable String path){
			this.path = path;
		}
	}
}
