package jp.blanktar.ruumusic.util;

import android.support.annotation.NonNull;
import android.content.Context;


public class RuuFile extends RuuFileBase{
	private final String[] extensions;


	RuuFile(@NonNull Context context, @NonNull RuuDirectory parent, @NonNull String path, @NonNull String[] extensions){
		super(context, parent, path);
		this.extensions = extensions;
	}

	@NonNull
	public static RuuFile getInstance(@NonNull Context context, @NonNull String path) throws RuuFileBase.NotFound{
		int slashpos = path.lastIndexOf("/");
		return RuuDirectory.getInstance(context, path.substring(0, slashpos + 1)).findMusic(path.substring(slashpos + 1));
	}

	@Override
	public boolean isDirectory(){
		return false;
	}

	@Override
	@NonNull
	public String getFullPath(){
		return path + name;
	}

	@NonNull
	public String getRealPath(){
		return getFullPath() + extensions[0];
	}

	@Override
	@NonNull
	public RuuDirectory getParent(){
		assert parent != null;

		return parent;
	}
}
