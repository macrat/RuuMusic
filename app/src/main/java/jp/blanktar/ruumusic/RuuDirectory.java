package jp.blanktar.ruumusic;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.Context;


public class RuuDirectory extends RuuFileBase {
	public RuuDirectory(@NonNull Context context, @NonNull String path) throws RuuFileBase.CanNotOpen {
		super(context, path);
		
		if(this.path.listFiles() == null) {
			throw new CanNotOpen(path);
		}
	}
	
	@NonNull
	public static RuuDirectory rootDirectory(@NonNull Context context) throws RuuFileBase.CanNotOpen {
		return new RuuDirectory(context, getRootDirectory(context));
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	@NonNull
	public String getFullPath() {
		String result = path.getPath();
		if(!result.endsWith("/")) {
			result += "/";
		}
		return result;
	}

	public boolean contains(@NonNull RuuFileBase file) {
		return file.getFullPath().startsWith(getFullPath());
	}

	@Nullable
	public ArrayList<RuuDirectory> getDirectories() {
		ArrayList<RuuDirectory> list = new ArrayList<>();

		File[] files = path.listFiles();
		if(files == null) {
			return null;
		}

		Arrays.sort(files);

		for(File file: files) {
			if(file.getName().lastIndexOf(".") != 0) {
				try {
					list.add(new RuuDirectory(context, file.getPath()));
				}catch(RuuFileBase.CanNotOpen e) {
					continue;
				}
			}
		}

		return list;
	}

	@Nullable
	public ArrayList<RuuFile> getMusics() {
		ArrayList<RuuFile> list = new ArrayList<>();

		File[] files = path.listFiles();
		if(files == null) {
			return null;
		}

		Arrays.sort(files);

		String before = "";
		for(File file: files) {
			if(file.getName().lastIndexOf(".") <= 0) {
				continue;
			}

			String path = file.getPath();
			int dotPos = path.lastIndexOf(".");
			String name = path.substring(0, dotPos);
			String ext = path.substring(dotPos);
			if(!file.isDirectory() && !name.equals(before) && getSupportedTypes().contains(ext)) {
				try {
					list.add(new RuuFile(context, name));
				}catch(RuuFileBase.CanNotOpen e) {
					continue;
				}
				before = name;
			}
		}

		return list;
	}

	@NonNull
	public ArrayList<RuuFile> getMusicsRecursive() {
		ArrayList<RuuFile> list = new ArrayList<>();

		ArrayList<RuuDirectory> dirs = getDirectories();
		for(RuuDirectory dir: dirs) {
			list.addAll(dir.getMusicsRecursive());
		}
		
		ArrayList<RuuFile> musics = getMusics();
		if(musics != null) {
			list.addAll(musics);
		}
		
		return list;
	}
}
