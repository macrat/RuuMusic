package jp.blanktar.ruumusic.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.content.Context;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;

import jp.blanktar.ruumusic.R;
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
	@NonNull public String title;

	@NonNull private RuuFile[] playlist;
	@NonNull private final RuuFile[] playlistSorted;
	@IntRange(from=0) private int currentIndex;
	private boolean sorted = true;


	private Playlist(@NonNull Context context, @NonNull RuuDirectory path, @NonNull RuuFile[] playlist, @IntRange(from=0) int currentIndex, @NonNull Type type, @Nullable String query) throws EmptyDirectory{
		if(playlist.length <= 0){
			throw new EmptyDirectory();
		}

		this.path = path;
		this.playlist = playlist;
		this.playlistSorted = playlist;
		this.currentIndex = currentIndex;
		this.type = type;
		this.query = query;

		title = "";
		try{
			switch(type){
				case SIMPLE:
					title = path.getRuuPath();
					break;
				case RECURSIVE:
					title = context.getString(R.string.recursive, path.getRuuPath());
					break;
				case SEARCH:
					title = context.getString(R.string.search_play, query);
					break;
			}
		}catch(RuuFileBase.OutOfRootDirectory e){
		}
	}

	@NonNull
	public static Playlist getByMusic(@NonNull Context context, @NonNull RuuFile music) throws RuuFileBase.NotFound, EmptyDirectory{
		List<RuuFile> list = music.getParent().getMusics();
		RuuFile[] playlist = list.toArray(new RuuFile[list.size()]);
		int index = Arrays.binarySearch(playlist, music);
		if(index < 0 || playlist.length <= index){
			throw new RuuFileBase.NotFound(music.getFullPath());
		}
		return new Playlist(context, music.getParent(), playlist, index, Type.SIMPLE, null);
	}

	@NonNull
	public static Playlist getRecursive(@NonNull Context context, @NonNull RuuDirectory dir) throws RuuFileBase.NotFound, EmptyDirectory{
		List<RuuFile> list = dir.getMusicsRecursive();
		return new Playlist(context, dir, list.toArray(new RuuFile[list.size()]), 0, Type.RECURSIVE, null);
	}

	@NonNull
	public static Playlist getSearchResults(@NonNull Context context, @NonNull RuuDirectory dir, @NonNull String query) throws RuuFileBase.NotFound, EmptyDirectory{
		ArrayList<RuuFile> result = new ArrayList<>();
		for(RuuFileBase found: dir.search(query)){
			if(!found.isDirectory()){
				result.add((RuuFile)found);
			}else{
				result.addAll(((RuuDirectory)found).getMusicsRecursive());
			}
		}

		return new Playlist(context, dir, result.toArray(new RuuFile[result.size()]), 0, Type.SEARCH, query);
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
		for(int i=0; i<playlist.length; i++){
			if(playlist[i].equals(path)){
				currentIndex = i;
				return;
			}
		}
		throw new NotFound();
	}

	public void goQueueIndex(long id){
		if(sorted){
			if(id < 0 || playlist.length <= id){
				throw new IndexOutOfBoundsException();
			}
			currentIndex = (int)id;
		}else{
			currentIndex = Arrays.asList(playlist).indexOf(playlistSorted[(int)id]);
		}
	}

	public void shuffle(boolean keepCurrent){
		RuuFile current = getCurrent();

		List<RuuFile> listTmp = Arrays.asList(playlistSorted.clone());
		do{
			Collections.shuffle(listTmp);
		}while(keepCurrent && listTmp.get(0).equals(current) && playlist.length > 1);
		playlist = listTmp.toArray(new RuuFile[playlistSorted.length]);

		if(keepCurrent){
			int pos = listTmp.indexOf(current);
			if(pos >= 0){
				playlist[pos] = playlist[0];
				playlist[0] = current;
			}
		}
		currentIndex = 0;
		sorted = false;
	}

	public void sort(){
		if(!sorted){
			currentIndex = getQueueIndex();
			playlist = playlistSorted;
			sorted = true;
		}
	}

	public List<MediaSessionCompat.QueueItem> getMediaSessionQueue() {
		List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();

		for(int i=0; i<playlistSorted.length; i++){
			MediaDescriptionCompat.Builder description = new MediaDescriptionCompat.Builder()
					.setTitle(playlistSorted[i].getName())
					.setMediaId(playlistSorted[i].getFullPath())
					.setMediaUri(playlistSorted[i].toUri());

			if(type != Type.SIMPLE){
				try{
					description.setSubtitle(playlistSorted[i].getParent().getRuuPath());
				}catch(RuuFileBase.OutOfRootDirectory e){
				}
			}

			queue.add(new MediaSessionCompat.QueueItem(description.build(), i));
		}

		return queue;
	}

	public int getQueueIndex(){
		return Arrays.asList(playlistSorted).indexOf(getCurrent());
	}


	public static class EndOfList extends Throwable{
	}

	public static class NotFound extends Throwable{
	}

	public static class EmptyDirectory extends Throwable{
	}
}
