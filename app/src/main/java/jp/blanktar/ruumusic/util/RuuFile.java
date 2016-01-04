package jp.blanktar.ruumusic.util;

import java.io.File;

import android.support.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;


public class RuuFile extends RuuFileBase{
	private final String extension;


	RuuFile(@NonNull Context context, @NonNull RuuDirectory parent, @NonNull String path, @NonNull String[] extensions){
		super(context, parent, path);

		int max = 0;
		String candidate = extensions[0];
		for(String ext: extensions){
			if(max < (new File(getFullPath() + ext)).length()){
				candidate = ext;
			}
		}
		this.extension = candidate;
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
		return getFullPath() + extension;
	}

	@Override
	@NonNull
	public RuuDirectory getParent(){
		assert parent != null;

		return parent;
	}

	@Override
	@NonNull
	public Intent toIntent(){
		Uri uri = (new Uri.Builder()).scheme("file").path(getRealPath()).build();
		return (new Intent(Intent.ACTION_VIEW)).setDataAndType(
			uri,
			MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.substring(1))
		).putExtra(Intent.EXTRA_STREAM, uri);
	}
}
