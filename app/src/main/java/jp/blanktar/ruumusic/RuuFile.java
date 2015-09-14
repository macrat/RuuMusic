package jp.blanktar.ruumusic;

import java.io.File;

import android.support.annotation.NonNull;
import android.content.Context;


public class RuuFile extends RuuFileBase {
	public RuuFile(@NonNull Context context, @NonNull String path) throws RuuFileBase.CanNotOpen {
		super(context, path);
	}
	
	@Override
	public boolean isDirectory() {
		return false;
	}
	
	@Override
	public String getFullPath() {
		return path.getPath();
	}
	
	public String getRealPath() {
		for(String ext: getSupportedTypes()) {
			File file = new File(getFullPath() + ext);
			if (file.isFile()) {
				return file.getPath();
			}
		}
		return null;
	}
	
	public String getName() {
		return path.getName();
	}
}
