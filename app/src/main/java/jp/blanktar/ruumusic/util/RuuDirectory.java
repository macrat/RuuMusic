package jp.blanktar.ruumusic.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.Context;


public class RuuDirectory extends RuuFileBase{
	private final static LinkedHashMap<String, RuuDirectory> cache = new LinkedHashMap<String, RuuDirectory>(){
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, RuuDirectory> eldest){
			return size() > 128;
		}
	};

	private final static LinkedHashMap<String, ArrayList<RuuFile>> recursiveMusicsCache = new LinkedHashMap<String, ArrayList<RuuFile>>(){
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, ArrayList<RuuFile>> eldest){
			return size() > 3;
		}
	};

	private final static LinkedHashMap<String, ArrayList<File>> recursiveAllCache = new LinkedHashMap<String, ArrayList<File>>(){
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, ArrayList<File>> eldest){
			return size() > 3;
		}
	};

	@Nullable private File[] musicsCache = null;
	@Nullable private File[] directoriesCache = null;
	@Nullable private File[] files = null;


	private RuuDirectory(@NonNull Context context, @NonNull File path) throws RuuFileBase.CanNotOpen{
		super(context, path);

		if(!path.isDirectory()){
			throw new CanNotOpen(path.getPath());
		}
	}

	public static RuuDirectory getInstance(@NonNull Context context, @NonNull File path) throws RuuFileBase.CanNotOpen{
		RuuDirectory result = cache.get(path.getPath());
		if(result == null){
			result = new RuuDirectory(context, path);
		}
		cache.put(result.getFullPath(), result);
		return result;
	}

	public static RuuDirectory getInstance(@NonNull Context context, @NonNull String path) throws RuuFileBase.CanNotOpen{
		RuuDirectory result = cache.get(path);
		if(result == null){
			result = getInstance(context, new File(path));
		}
		return result;
	}

	@NonNull
	public static RuuDirectory rootDirectory(@NonNull Context context) throws RuuFileBase.CanNotOpen{
		String root = Preference.Str.ROOT_DIRECTORY.get(context);
		return RuuDirectory.getInstance(context, root == null ? "/" : root);
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
				result.add(RuuDirectory.getInstance(context, file));
			}
		}else{
			if(files == null){
				files = path.listFiles();
				if(files == null){
					throw new RuuFileBase.CanNotOpen(getFullPath());
				}
			}

			ArrayList<File> cacheTmp = new ArrayList<>();

			for(File file: files){
				if(!file.isHidden()){
					RuuDirectory dir;
					try{
						dir = RuuDirectory.getInstance(context, file);
					}catch(RuuFileBase.CanNotOpen e){
						continue;
					}
					result.add(dir);
					cacheTmp.add(dir.path);
				}
			}

			if(musicsCache != null){
				files = null;
			}

			directoriesCache = cacheTmp.toArray(new File[cacheTmp.size()]);
			Arrays.sort(directoriesCache);

			Collections.sort(result);
		}

		return result;
	}

	@NonNull
	public ArrayList<RuuFile> getMusics() throws RuuFileBase.CanNotOpen{
		ArrayList<RuuFile> result = new ArrayList<>();

		if(musicsCache != null){
			for(File file: musicsCache){
				result.add(new RuuFile(context, file));
			}
		}else{
			if(files == null){
				files = path.listFiles();
				if(files == null){
					throw new RuuFileBase.CanNotOpen(getFullPath());
				}
			}

			ArrayList<File> cacheTmp = new ArrayList<>();

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
					RuuFile music = new RuuFile(context, name);
					result.add(music);
					cacheTmp.add(music.path);
					before = name;
				}
			}

			if(directoriesCache != null){
				files = null;
			}

			musicsCache = cacheTmp.toArray(new File[cacheTmp.size()]);
			Arrays.sort(musicsCache);

			Collections.sort(result);
		}

		return result;
	}

	@NonNull
	private ArrayList<RuuFile> getMusicsRecursiveWithoutCache() throws RuuFileBase.CanNotOpen{
		ArrayList<RuuFile> list = new ArrayList<>();

		for(RuuDirectory dir: getDirectories()){
			ArrayList<RuuFile> files;
			try{
				files = dir.getMusicsRecursiveWithoutCache();
			}catch(RuuFileBase.CanNotOpen e){
				continue;
			}
			list.addAll(files);
		}

		list.addAll(getMusics());

		return list;
	}

	@NonNull
	public ArrayList<RuuFile> getMusicsRecursive() throws RuuFileBase.CanNotOpen{
		ArrayList<RuuFile> list = recursiveMusicsCache.get(getFullPath());

		if(list == null){
			list = getMusicsRecursiveWithoutCache();
		}

		recursiveMusicsCache.put(getFullPath(), list);

		return list;
	}

	@NonNull
	private ArrayList<RuuDirectory> getDirectoriesWithoutCache() throws RuuFileBase.CanNotOpen{
		ArrayList<RuuDirectory> list = new ArrayList<>();

		ArrayList<RuuDirectory> dirs = getDirectories();
		for(RuuDirectory dir: dirs){
			ArrayList<RuuDirectory> ruuDirs;
			try{
				ruuDirs = dir.getDirectoriesWithoutCache();
			}catch(RuuFileBase.CanNotOpen e){
				continue;
			}
			list.addAll(ruuDirs);
		}

		list.addAll(dirs);

		return list;
	}

	@NonNull
	private ArrayList<RuuFileBase> getAllRecursiveWithoutCache() throws RuuFileBase.CanNotOpen{
		ArrayList<RuuFileBase> list = new ArrayList<>();
		list.addAll(getDirectoriesWithoutCache());
		list.addAll(getMusicsRecursive());

		return list;
	}

	@NonNull
	public ArrayList<RuuFileBase> getAllRecursive() throws RuuFileBase.CanNotOpen{
		ArrayList<File> list = recursiveAllCache.get(getFullPath());
		ArrayList<RuuFileBase> result;

		if(list == null){
			result = getAllRecursiveWithoutCache();
			list = new ArrayList<>();
			for(RuuFileBase file: result){
				list.add(file.path);
			}
		}else{
			result = new ArrayList<>();
			for(File file: list){
				RuuFileBase ruuFile;
				try{
					if(file.isDirectory()){
						ruuFile = RuuDirectory.getInstance(context, file);
					}else{
						ruuFile = new RuuFile(context, file);
					}
				}catch(RuuFile.CanNotOpen e){
					continue;
				}
				result.add(ruuFile);
			}
		}

		recursiveAllCache.put(getFullPath(), list);

		return result;
	}
}
