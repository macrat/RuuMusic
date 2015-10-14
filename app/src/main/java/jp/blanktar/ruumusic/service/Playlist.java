package jp.blanktar.ruumusic.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.content.Context;
import android.text.TextUtils;

import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


@WorkerThread
public class Playlist{
	enum Type{
		SIMPLE,
		RECURSIVE,
		SEARCH
	}

	@NonNull public final Type type;
	@NonNull public final RuuDirectory path;
	@Nullable public final String query;

	@NonNull private RuuFile[] playlist;
	@NonNull private final RuuFile[] playlistSorted;
	@IntRange(from=0) private int currentIndex;
	private boolean sorted = true;


	private Playlist(@NonNull RuuDirectory path, @NonNull RuuFile[] playlist, @IntRange(from=0) int currentIndex, @NonNull Type type, @Nullable String query) throws EmptyDirectory{
		if(playlist.length <= 0){
			throw new EmptyDirectory();
		}

		this.path = path;
		this.playlist = playlist;
		this.playlistSorted = playlist;
		this.currentIndex = currentIndex;
		this.type = type;
		this.query = query;
	}
	
	@NonNull
	public static Playlist getByMusicPath(@NonNull Context context, @NonNull String path) throws RuuFileBase.CanNotOpen, EmptyDirectory{
		RuuFile music = new RuuFile(context, path);
		List<RuuFile> list = music.getParent().getMusics();
		RuuFile[] playlist = list.toArray(new RuuFile[list.size()]);
		return new Playlist(music.getParent(), playlist, Arrays.binarySearch(playlist, music), Type.SIMPLE, null);
	}

	@NonNull
	public static Playlist getRecursive(@NonNull Context context, @NonNull String path) throws RuuFileBase.CanNotOpen, EmptyDirectory{
		RuuDirectory dir = RuuDirectory.getInstance(context, path);
		List<RuuFile> list = dir.getMusicsRecursive();
		return new Playlist(dir, list.toArray(new RuuFile[list.size()]), 0, Type.RECURSIVE, null);
	}

	@NonNull
	public static Playlist getSearchResults(@NonNull Context context, @NonNull String path, @NonNull String query) throws RuuFileBase.CanNotOpen, EmptyDirectory{
		String[] queries = TextUtils.split(query.toLowerCase(), " \t");

		RuuDirectory dir = RuuDirectory.getInstance(context, path);
		List<RuuFile> list = dir.getMusicsRecursive();
		for(RuuFile file: list){
			String name = file.getName().toLowerCase();
			for(String qs: queries){
				if(!name.contains(qs)){
					list.remove(file);
					break;
				}
			}
		}
		return new Playlist(dir, list.toArray(new RuuFile[list.size()]), 0, Type.SEARCH, query);
	}


	@NonNull
	public RuuFile getCurrent(){
		return playlist[currentIndex];
	}

	public void goFirst(){
		currentIndex = 0;
	}

	public void goLast(){
		currentIndex = playlist.length - 1;
	}

	public void goNext() throws EndOfList{
		if(currentIndex + 1 >= playlist.length){
			throw new EndOfList();
		}
		currentIndex++;
	}

	public void goPrev() throws EndOfList{
		if(currentIndex - 1 < 0){
			throw new EndOfList();
		}
		currentIndex--;
	}

	public void goMusic(@NonNull RuuFile path) throws NotFound{
		int result;
		if(sorted){
			result = Arrays.binarySearch(playlist, path);
		}else{
			result = Arrays.asList(playlist).indexOf(path);
		}
		if(result >= 0){
			currentIndex = result;
		}else{
			throw new NotFound();
		}
	}

	public void shuffle(boolean keepCurrent){
		RuuFile current = getCurrent();

		List<RuuFile> listTmp = Arrays.asList(playlistSorted.clone());
		do{
			Collections.shuffle(listTmp);
		}while(keepCurrent && listTmp.get(0).equals(current));
		playlist = listTmp.toArray(new RuuFile[playlistSorted.length]);

		if(keepCurrent){
			int pos = listTmp.indexOf(current);
			if(pos >= 0){
				playlist[0] = playlist[pos];
				playlist[pos] = current;
			}
		}
		currentIndex = 0;
		sorted = false;
	}

	public void sort(){
		if(!sorted){
			currentIndex = Arrays.binarySearch(playlistSorted, getCurrent());
			playlist = playlistSorted;
			sorted = true;
		}
	}


	public class EndOfList extends Throwable{
	}

	public class NotFound extends Throwable{
	}

	public class EmptyDirectory extends Throwable{
	}
}
