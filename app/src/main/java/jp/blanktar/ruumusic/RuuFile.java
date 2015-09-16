package jp.blanktar.ruumusic;

import java.io.File;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
	@NonNull
	public String getFullPath() {
		return path.getPath();
	}

	@Nullable
	public String getRealPath() {
		for(String ext: getSupportedTypes()) {
			File file = new File(getFullPath() + ext);
			if (file.isFile()) {
				return file.getPath();
			}
		}
		return null;
	}

	@NonNull
	public String getName() {
		return path.getName();
	}
}
