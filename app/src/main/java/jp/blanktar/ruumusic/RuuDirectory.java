package jp.blanktar.ruumusic;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

import android.support.annotation.NonNull;
import android.content.Context;


public class RuuDirectory extends RuuFileBase{
	private static LinkedHashMap<String, RuuDirectory> cache = new LinkedHashMap<String, RuuDirectory>(){
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, RuuDirectory> eldest){
			return size() > 128;
		}
	};

	private ArrayList<File> musicsCache = null;
	private ArrayList<File> directoriesCache = null;

	private RuuDirectory(@NonNull Context context, @NonNull String path) throws RuuFileBase.CanNotOpen{
		super(context, path);

		if(this.path.listFiles() == null){
			throw new CanNotOpen(path);
		}
	}

	public static RuuDirectory getInstance(@NonNull Context context, @NonNull String path) throws RuuFileBase.CanNotOpen{
		RuuDirectory result = cache.get(path);
		if(result == null){
			result = new RuuDirectory(context, path);
			cache.put(path, result);
		}
		return result;
	}

	@NonNull
	public static RuuDirectory rootDirectory(@NonNull Context context) throws RuuFileBase.CanNotOpen{
		return RuuDirectory.getInstance(context, getRootDirectory(context));
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
		ArrayList<RuuDirectory> result = new ArrayList<>();

		if(directoriesCache != null){
			for(File file: directoriesCache){
				result.add(RuuDirectory.getInstance(context, file.getPath()));
			}
		}else{
			File[] files = path.listFiles();
			if(files == null){
				throw new RuuFileBase.CanNotOpen(getFullPath());
			}

			Arrays.sort(files);

			directoriesCache = new ArrayList<>();

			for(File file: files){
				if(file.getName().lastIndexOf(".") != 0){
					try{
						RuuDirectory dir = RuuDirectory.getInstance(context, file.getPath());
						result.add(dir);
						directoriesCache.add(dir.path);
					}catch(RuuFileBase.CanNotOpen e){
					}
				}
			}
		}

		return result;
	}

	@NonNull
	public ArrayList<RuuFile> getMusics() throws RuuFileBase.CanNotOpen{
		ArrayList<RuuFile> result = new ArrayList<>();

		if(musicsCache != null){
			for(File file: musicsCache){
				result.add(new RuuFile(context, file.getPath()));
			}
		}else{
			File[] files = path.listFiles();
			if(files == null){
				throw new RuuFileBase.CanNotOpen(getFullPath());
			}

			Arrays.sort(files);

			musicsCache = new ArrayList<>();

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
						RuuFile music = new RuuFile(context, name);
						result.add(music);
						musicsCache.add(music.path);
					}catch(RuuFileBase.CanNotOpen e){
						continue;
					}
					before = name;
				}
			}
		}

		return result;
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
