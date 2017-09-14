package jp.blanktar.ruumusic.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.text.TextUtils;


public class RuuDirectory extends RuuFileBase{
	@Nullable private static RuuDirectory root;

	@Nullable private String[] childrenTemp;
	private boolean childrenProceed = false;
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
			Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_data"}, null, null, "lower(_data)");
			if(cursor == null || !cursor.moveToFirst()){
				throw new RuuFileBase.NotFound(path);
			}else{
				do{
					musics.add(cursor.getString(cursor.getColumnIndex("_data")));
				}while(cursor.moveToNext());
			}
			cursor.close();
			String[] array = musics.toArray(new String[musics.size()]);
			root = new RuuDirectory(context, null, "/", array);
		}

		return root.findDir(path);
	}

	@NonNull
	public static RuuDirectory rootDirectory(@NonNull Context context) throws RuuFileBase.NotFound{
		RuuDirectory root = new Preference(context).RootDirectory.get();
		if(root == null){
			throw new RuuFileBase.NotFound(null);
		}
		return root;
	}

	@NonNull
	public static RuuDirectory rootCandidate(@NonNull Context context) throws RuuFileBase.NotFound{
		RuuDirectory dir = RuuDirectory.getInstance(context, "/");
		while(dir.getDirectories().size() + dir.getMusics().size() < 2 && dir.getDirectories().size() > 0){
			dir = dir.getDirectories().get(0);
		}
		return dir;
	}

	@NonNull
	private RuuDirectory findDir(@NonNull String path) throws RuuFileBase.NotFound{
		assert path != null && path.startsWith("/") && path.endsWith("/");

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

	@NonNull
	public List<RuuFileBase> search(@NonNull String query){
		final ArrayList<RuuFileBase> filtered = new ArrayList<>();

		if(TextUtils.isEmpty(query)){
			return filtered;
		}

		final String[] queries = TextUtils.split(query.toLowerCase(), " \t");

		for(RuuFileBase file: getAllRecursive()){
			String name = file.getName().toLowerCase();
			boolean isOk = true;
			for(String q: queries){
				if(!name.contains(q)){
					isOk = false;
					break;
				}
			}
			if(isOk){
				filtered.add(file);
			}
		}

		return filtered;
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

	@Override
	@NonNull
	public Uri toUri(){
		return (new Uri.Builder()).scheme("file").path(getFullPath()).build();
	}

	@Override
	@NonNull
	public Intent toIntent(){
		return (new Intent(Intent.ACTION_VIEW)).setDataAndType(toUri(), "text/directory").putExtra(Intent.EXTRA_STREAM, toUri());
	}

	public boolean contains(@NonNull RuuFileBase file){
		return file.getFullPath().startsWith(getFullPath());
	}

	private void processChildren(){
		assert childrenTemp != null;
		assert musics.isEmpty() && directories.isEmpty();

		if(childrenProceed){
			return;
		}
		childrenProceed = true;

		int pathlength = getFullPath().length();
		String target = null;
		ArrayList<String> stack = new ArrayList<>();
		boolean isDir = false;
		String next = null;
		for(String child: childrenTemp){
			if(child == null){
				continue;
			}
			int nextslash = child.indexOf("/", pathlength);
			String ext = "";

			if(nextslash < 0){
				int dotpos = child.lastIndexOf(".");
				next = child.substring(0, dotpos);
				ext = child.substring(dotpos);
			}else{
				next = child.substring(0, nextslash);
			}

			if(target != null && (!next.equals(target) || nextslash >= 0 != isDir)){
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

		childrenTemp = null;

		Collections.sort(musics);
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
	public List<RuuFileBase> getChildren(){
		ArrayList<RuuFileBase> children = new ArrayList<>();

		for(RuuDirectory x: getDirectories()){
			children.add(x);
		}

		for(RuuFile x: getMusics()){
			children.add(x);
		}

		return children;
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
	public List<RuuDirectory> getDirectoriesRecursive(){
		ArrayList<RuuDirectory> list = new ArrayList<>();

		List<RuuDirectory> dirs = getDirectories();
		for(RuuDirectory dir: dirs){
			list.addAll(dir.getDirectoriesRecursive());
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

	@Override
	@NonNull
	public MediaItem toMediaItem(){
		return new MediaItem(
				new MediaDescriptionCompat.Builder()
						.setTitle(getName() + "/")
						.setMediaUri(toUri())
						.setMediaId(getFullPath())
						.build(),
				MediaItem.FLAG_BROWSABLE);
	}

	@NonNull
	public List<MediaItem> getMediaItemList(){
		ArrayList<MediaItem> list = new ArrayList<>();

		for(RuuDirectory d: getDirectories()){
			list.add(d.toMediaItem());
		}
		for(RuuFile f: getMusics()){
			list.add(f.toMediaItem());
		}

		return list;
	}
}
