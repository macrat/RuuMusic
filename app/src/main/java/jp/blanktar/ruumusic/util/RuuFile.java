package jp.blanktar.ruumusic.util;

import java.io.File;

import android.support.annotation.NonNull;
import android.content.Context;


public class RuuFile extends RuuFileBase{
	public RuuFile(@NonNull Context context, @NonNull File path) throws RuuFileBase.CanNotOpen{
		super(context, path);
	}

	public RuuFile(@NonNull Context context, @NonNull String path) throws RuuFileBase.CanNotOpen{
		super(context, new File(path));
	}

	@Override
	public boolean isDirectory(){
		return false;
	}

	@Override
	@NonNull
	public String getFullPath(){
		return path.getPath();
	}

	@NonNull
	public String getRealPath() throws RuuFileBase.CanNotOpen{
		for(String ext: getSupportedTypes()){
			File file = new File(getFullPath() + ext);
			if(file.isFile()){
				return file.getPath();
			}
		}
		throw new RuuFileBase.CanNotOpen(getFullPath());
	}
}
