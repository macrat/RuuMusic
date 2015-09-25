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
			return size() > 64;
		}
	};
	
	private ArrayList<RuuFile> musicsCache = null;
	private ArrayList<RuuDirectory> directoriesCache = null;
	
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
		if(directoriesCache == null){
			File[] files = path.listFiles();
			if(files == null){
				throw new RuuFileBase.CanNotOpen(getFullPath());
			}

			Arrays.sort(files);

			directoriesCache = new ArrayList<>();

			for(File file: files){
				if(file.getName().lastIndexOf(".") != 0){
					try{
						directoriesCache.add(RuuDirectory.getInstance(context, file.getPath()));
					}catch(RuuFileBase.CanNotOpen e){
					}
				}
			}
		}

		return directoriesCache;
	}

	@NonNull
	public ArrayList<RuuFile> getMusics() throws RuuFileBase.CanNotOpen{
		if(musicsCache == null){
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
						musicsCache.add(new RuuFile(context, name));
					}catch(RuuFileBase.CanNotOpen e){
						continue;
					}
					before = name;
				}
			}
		}

		return musicsCache;
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
