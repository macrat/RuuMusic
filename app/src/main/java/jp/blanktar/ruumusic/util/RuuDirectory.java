package jp.blanktar.ruumusic.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;


public class RuuDirectory extends RuuFileBase{
	@Nullable private static RuuDirectory root;

	@Nullable private String[] childrenTemp;
	@NonNull final private List<RuuDirectory> directories = new ArrayList<>();
	@NonNull final private List<RuuFile> musics = new ArrayList<>();


	private RuuDirectory(@NonNull Context context, @Nullable RuuDirectory parent, @NonNull String path, @Nullable String[] children){
		super(context, parent, path);

		this.childrenTemp = children;
	}

	public static RuuDirectory getInstance(@NonNull Context context, @NonNull String path) throws RuuFileBase.NotFound{
		assert path.startsWith("/") && path.endsWith("/");

		if(root == null){
			ArrayList<String> musics = new ArrayList<>();
			Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
			assert cursor != null;
			if(cursor.moveToFirst()){
				do{
					musics.add(cursor.getString(cursor.getColumnIndex("_data")));
				}while(cursor.moveToNext());
			}
			cursor.close();
			String[] array = musics.toArray(new String[musics.size()]);
			Arrays.sort(array);
			root = new RuuDirectory(context, null, "/", array);
		}

		return root.findDir(path);
	}

	@NonNull
	public static RuuDirectory rootDirectory(@NonNull Context context) throws RuuFileBase.NotFound{
		String root = Preference.Str.ROOT_DIRECTORY.get(context);
		return RuuDirectory.getInstance(context, root == null ? "/" : root);
	}

	@NonNull
	private RuuDirectory findDir(@NonNull String path) throws RuuFileBase.NotFound{
		assert path.startsWith("/") && path.endsWith("/");

		if(getFullPath().equals(path)){
			return this;
		}

		int pathlength = getFullPath().length();
		int slashpos = path.indexOf("/", pathlength);
		String target = path.substring(pathlength, slashpos);

		for(RuuDirectory dir: getDirectories()){
			if(target.equals(dir.getName())){
				return dir.findDir(path);
			}
		}

		throw new RuuFileBase.NotFound(path);
	}

	@NonNull
	RuuFile findMusic(@NonNull String name) throws RuuFileBase.NotFound{
		for(RuuFile music: getMusics()){
			if(name.equals(music.getName())){
				return music;
			}
		}
		throw new RuuFileBase.NotFound(getFullPath() + name);
	}

	@Override
	public boolean isDirectory(){
		return true;
	}

	@Override
	@NonNull
	public String getFullPath(){
		if(name.equals("")){
			return path;
		}else{
			return path + name + "/";
		}
	}

	public boolean contains(@NonNull RuuFileBase file){
		return file.getFullPath().startsWith(getFullPath());
	}

	private void processChildren(){
		assert childrenTemp != null;
		assert musics.isEmpty() && directories.isEmpty();

		int pathlength = getFullPath().length();
		String target = null;
		ArrayList<String> stack = new ArrayList<>();
		boolean isDir = false;
		String next = null;
		for(String child: childrenTemp){
			int nextslash = child.indexOf("/", pathlength);
			String ext = "";

			if(nextslash < 0){
				int dotpos = child.lastIndexOf(".");
				next = child.substring(0, dotpos);
				ext = child.substring(dotpos);
			}else{
				next = child.substring(0, nextslash);
			}

			if(target != null && !next.equals(target)){
				if(isDir){
					directories.add(new RuuDirectory(context, this, target, stack.toArray(new String[stack.size()])));
				}else{
					musics.add(new RuuFile(context, this, target, stack.toArray(new String[stack.size()])));
				}
				stack.clear();
			}
			target = next;
			
			if(nextslash < 0){
				isDir = false;
				stack.add(ext);
			}else{
				isDir = true;
				stack.add(child);
			}
		}
		if(next != null){
			if(isDir){
				directories.add(new RuuDirectory(context, this, next, stack.toArray(new String[stack.size()])));
			}else{
				musics.add(new RuuFile(context, this, next, stack.toArray(new String[stack.size()])));
			}
		}

		childrenTemp = null
		
		;
	}

	@NonNull
	public List<RuuDirectory> getDirectories(){
		if(childrenTemp != null){
			processChildren();
		}

		return directories;
	}

	@NonNull
	public List<RuuFile> getMusics(){
		if(childrenTemp != null){
			processChildren();
		}

		return musics;
	}

	@NonNull
	public List<RuuFile> getMusicsRecursive(){
		ArrayList<RuuFile> list = new ArrayList<>();

		for(RuuDirectory dir: getDirectories()){
			list.addAll(dir.getMusicsRecursive());
		}

		list.addAll(getMusics());

		return list;
	}

	@NonNull
	private List<RuuDirectory> getDirectoriesRecursive(){
		ArrayList<RuuDirectory> list = new ArrayList<>();

		List<RuuDirectory> dirs = getDirectories();
		for(RuuDirectory dir: dirs){
			list.addAll(dir.getDirectories());
		}

		list.addAll(dirs);

		return list;
	}

	@NonNull
	public List<RuuFileBase> getAllRecursive(){
		ArrayList<RuuFileBase> list = new ArrayList<>();
		list.addAll(getDirectoriesRecursive());
		list.addAll(getMusicsRecursive());

		return list;
	}
}
