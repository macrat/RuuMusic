package jp.blanktar.ruumusic;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;

import android.support.annotation.NonNull;
import android.content.Context;


public class RuuDirectory extends RuuFileBase{
	public RuuDirectory(@NonNull Context context, @NonNull String path) throws RuuFileBase.CanNotOpen{
		super(context, path);

		if(this.path.listFiles() == null){
			throw new CanNotOpen(path);
		}
	}

	@NonNull
	public static RuuDirectory rootDirectory(@NonNull Context context) throws RuuFileBase.CanNotOpen{
		return new RuuDirectory(context, getRootDirectory(context));
	}

	@Override
	public boolean isDirectory(){
		return true;
	}

	@Override
	@NonNull
	public String getFullPath(){
		String result = path.getPath();
		if(!result.endsWith("/")){
			result += "/";
		}
		return result;
	}

	public boolean contains(@NonNull RuuFileBase file){
		return file.getFullPath().startsWith(getFullPath());
	}

	@NonNull
	public ArrayList<RuuDirectory> getDirectories() throws RuuFileBase.CanNotOpen{
		ArrayList<RuuDirectory> list = new ArrayList<>();

		File[] files = path.listFiles();
		if(files == null){
			throw new RuuFileBase.CanNotOpen(getFullPath());
		}

		Arrays.sort(files);

		for(File file: files){
			if(file.getName().lastIndexOf(".") != 0){
				try{
					list.add(new RuuDirectory(context, file.getPath()));
				}catch(RuuFileBase.CanNotOpen e){
				}
			}
		}

		return list;
	}

	@NonNull
	public ArrayList<RuuFile> getMusics() throws RuuFileBase.CanNotOpen{
		ArrayList<RuuFile> list = new ArrayList<>();

		File[] files = path.listFiles();
		if(files == null){
			throw new RuuFileBase.CanNotOpen(getFullPath());
		}

		Arrays.sort(files);

		String before = "";
		for(File file: files){
			if(file.getName().lastIndexOf(".") <= 0){
				continue;
			}

			String path = file.getPath();
			int dotPos = path.lastIndexOf(".");
			String name = path.substring(0, dotPos);
			String ext = path.substring(dotPos);
			if(!file.isDirectory() && !name.equals(before) && getSupportedTypes().contains(ext)){
				try{
					list.add(new RuuFile(context, name));
				}catch(RuuFileBase.CanNotOpen e){
					continue;
				}
				before = name;
			}
		}

		return list;
	}

	@NonNull
	public ArrayList<RuuFile> getMusicsRecursive() throws RuuFileBase.CanNotOpen{
		ArrayList<RuuFile> list = new ArrayList<>();

		for(RuuDirectory dir: getDirectories()){
			try{
				list.addAll(dir.getMusicsRecursive());
			}catch(RuuFileBase.CanNotOpen e){
			}
		}

		list.addAll(getMusics());

		return list;
	}
}
