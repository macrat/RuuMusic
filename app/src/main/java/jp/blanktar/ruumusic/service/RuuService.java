package jp.blanktar.ruumusic.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.util.PlayingStatus;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.util.RepeatModeType;
import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


@WorkerThread
public class RuuService extends MediaBrowserServiceCompat implements SharedPreferences.OnSharedPreferenceChangeListener{
	public final static String ACTION_PLAY = "jp.blanktar.ruumusic.PLAY";
	public final static String ACTION_PLAY_RECURSIVE = "jp.blanktar.ruumusic.PLAY_RECURSIVE";
	public final static String ACTION_PLAY_SEARCH = "jp.blanktar.ruumusic.PLAY_SEARCH";
	public final static String ACTION_PAUSE = "jp.blanktar.ruumusic.PAUSE";
	public final static String ACTION_PAUSE_TRANSIENT = "jp.blanktar.ruumusic.PAUSE_TRANSIENT";
	public final static String ACTION_PLAY_PAUSE = "jp.blanktar.ruumusic.PLAY_PAUSE";
	public final static String ACTION_NEXT = "jp.blanktar.ruumusic.NEXT";
	public final static String ACTION_PREV = "jp.blanktar.ruumusic.PREV";
	public final static String ACTION_SEEK = "jp.blanktar.ruumusic.SEEK";
	public final static String ACTION_REPEAT = "jp.blanktar.ruumusic.REPEAT";
	public final static String ACTION_SHUFFLE = "jp.blanktar.ruumusic.SHUFFLE";
	public final static String ACTION_PING = "jp.blanktar.ruumusic.PING";
	public final static String ACTION_REQUEST_EQUALIZER_INFO = "jp.blanktar.ruumusic.REQUEST_EQUALIZER_INFO";
	public final static String ACTION_EQUALIZER_INFO = "jp.blanktar.ruumusic.EQUALIZER_INFO";
	public final static String ACTION_STATUS = "jp.blanktar.ruumusic.STATUS";
	public final static String ACTION_FAILED_PLAY = "jp.blanktar.ruumusic.FAILED_PLAY";
	public final static String ACTION_NOT_FOUND = "jp.blanktar.ruumusic.NOT_FOUND";

	public enum Status{
		INITIAL,
		LOADING_FROM_LASTEST,
		LOADING,
		READY,
		ERRORED
	}

	@NonNull private final MediaPlayer player = new MediaPlayer();
	private MediaPlayer endOfListSE;
	private MediaPlayer errorSE;
	@Nullable private Timer deathTimer;
	@Nullable private Preference preference;

	@Nullable private Playlist playlist;

	@NonNull private RepeatModeType repeatMode = RepeatModeType.OFF;
	private boolean shuffleMode = false;
	@NonNull private Status status = Status.INITIAL;

	@Nullable private EffectManager effectManager = null;

	private IntentEndpoint intentEndpoint;
	private MediaSessionEndpoint mediaSessionEndpoint;
	private EndpointManager endpoints = new EndpointManager();


	@Override
	public void onCreate(){
		super.onCreate();

		endOfListSE = MediaPlayer.create(getApplicationContext(), R.raw.eol);
		errorSE = MediaPlayer.create(getApplicationContext(), R.raw.err);
		preference = new Preference(getApplicationContext());

		repeatMode = preference.RepeatMode.get();
		shuffleMode = preference.ShuffleMode.get();

		String recursive = preference.RecursivePath.get();
		if(recursive != null){
			try{
				playlist = Playlist.getRecursive(getApplicationContext(), recursive);
			}catch(RuuFileBase.NotFound | Playlist.EmptyDirectory e){
				playlist = null;
			}
		}else{
			String searchQuery = preference.SearchQuery.get();
			String searchPath = preference.SearchPath.get();
			if(searchQuery != null && searchPath != null){
				try{
					playlist = Playlist.getSearchResults(getApplicationContext(), RuuDirectory.getInstanceFromFullPath(getApplicationContext(), searchPath), searchQuery);
				}catch(RuuFileBase.NotFound | RuuFileBase.OutOfRootDirectory | Playlist.EmptyDirectory e){
					playlist = null;
				}
			}
		}

		String last_play = preference.LastPlayMusic.get();
		if(last_play != null && !last_play.equals("")){
			try{
				if(playlist != null){
					playlist.goMusic(RuuFile.getInstance(getApplicationContext(), last_play));
				}else{
					playlist = Playlist.getByMusicPath(getApplicationContext(), last_play);
				}
				if(shuffleMode){
					playlist.shuffle(true);
				}
				load(true);
			}catch(RuuFileBase.NotFound | Playlist.NotFound | Playlist.EmptyDirectory e){
				playlist = null;
			}
		}else if(playlist != null && shuffleMode){
			playlist.shuffle(false);
			load(true);
		}

		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
			@Override
			public void onCompletion(@Nullable MediaPlayer mp){
				if(repeatMode.equals(RepeatModeType.SINGLE)){
					player.pause();
					play();
				}else{
					try{
						playlist.goNext();
						load(false);
					}catch(Playlist.EndOfList e){
						if(repeatMode.equals(RepeatModeType.OFF)){
							player.pause();
							player.seekTo(0);
							pause();
						}else{
							if(shuffleMode){
								playlist.shuffle(false);
							}else{
								playlist.goFirst();
							}
							load(false);
						}
					}
				}
			}
		});

		player.setOnErrorListener(new MediaPlayer.OnErrorListener(){
			@Override
			public boolean onError(@Nullable MediaPlayer mp, int what, int extra){
				player.reset();

				if(status != Status.ERRORED && playlist != null){
					endpoints.onFailedPlay(getPlayingStatus());

					if(!errorSE.isPlaying()){
						errorSE.start();
					}
				}

				status = Status.ERRORED;

				return true;
			}
		});

		player.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
			@Override
			public void onPrepared(@NonNull MediaPlayer mp){
				if(status == Status.LOADING_FROM_LASTEST){
					status = Status.READY;
					player.seekTo(preference.LastPlayPosition.get());
					sendStatus();
				}else{
					status = Status.READY;
					play();
				}
			}
		});

		effectManager = new EffectManager(player, getApplicationContext());
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		registerReceiver(broadcastReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

		intentEndpoint = new IntentEndpoint(getApplicationContext(), new Controller());
		endpoints.add(intentEndpoint);

		mediaSessionEndpoint = new MediaSessionEndpoint(getApplicationContext(), new Controller(), getPlayingStatus(), playlist);
		setSessionToken(mediaSessionEndpoint.getSessionToken());
		endpoints.add(mediaSessionEndpoint);

		endpoints.add(new NotificationEndpoint(this, mediaSessionEndpoint.getMediaSession()));
		endpoints.add(new WearEndpoint(getApplicationContext(), new Controller()));
		endpoints.add(new ShortcutsEndpoint(getApplicationContext()));

		String dataVersion = RuuFileBase.getDataVersion(getApplicationContext());
		String currentVersion = preference.MediaStoreVersion.get();
		if (currentVersion == null || dataVersion == null || !currentVersion.equals(dataVersion)) {
			endpoints.onMediaStoreUpdated();
			preference.MediaStoreVersion.set(dataVersion);
		}

		startDeathTimer();
	}

	@Override
	public int onStartCommand(@Nullable Intent intent, int flags, int startId){
		intentEndpoint.onIntent(intent);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy(){
		unregisterReceiver(broadcastReceiver);

		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);

		endpoints.close();

		effectManager.release();

		saveStatus();
	}

	@Override
	public void onSharedPreferenceChanged(@NonNull SharedPreferences p, @NonNull String key){
		if(key.equals("root_directory")){
			updateRoot();
		}
	}

	private PlayingStatus getPlayingStatus(){
		return new PlayingStatus(
				status == Status.READY && player.isPlaying(),
				playlist != null ? playlist.getCurrent() : null,

				status == Status.READY ? (long)player.getDuration() : 0,
				status == Status.READY ? System.currentTimeMillis() - player.getCurrentPosition() : 0,
				status == Status.READY ? (long)player.getCurrentPosition() : 0,

				repeatMode,
				shuffleMode,

				(playlist != null && playlist.type == Playlist.Type.SEARCH) ? playlist.path : null,
				playlist != null ? playlist.query : null,

				(playlist != null && playlist.type == Playlist.Type.RECURSIVE) ? playlist.path : null
		);
	}

	private void sendStatus(){
		if (playlist != null) {
			mediaSessionEndpoint.updateQueue(playlist);
		}

		endpoints.onStatusUpdated(getPlayingStatus());
	}

	private void sendEqualizerInfo(){
		new Thread(new Runnable(){
			@Override
			public void run(){
				endpoints.onEqualizerInfo(effectManager.getEqualizerInfo());
			}
		}).start();
	}

	private void saveStatus(){
		if(playlist != null){
			preference.LastPlayMusic.set(playlist.getCurrent().getFullPath());
			preference.LastPlayPosition.set(player.getCurrentPosition());

			if(playlist.type == Playlist.Type.RECURSIVE){
				preference.RecursivePath.set(playlist.path.getFullPath());
			}else{
				preference.RecursivePath.remove();
			}

			if(playlist.type == Playlist.Type.SEARCH){
				preference.SearchPath.set(playlist.path.getFullPath());
				preference.SearchQuery.set(playlist.query);
			}else{
				preference.SearchPath.remove();
				preference.SearchQuery.remove();
			}
		}
	}

	private void removeSavedStatus(){
		preference.LastPlayMusic.remove();
		preference.LastPlayPosition.remove();
		preference.RecursivePath.remove();
		preference.SearchPath.remove();
		preference.SearchQuery.remove();
	}

	private void updateRoot(){
		RuuDirectory root;
		try{
			root = RuuDirectory.rootDirectory(getApplicationContext());
		}catch(RuuFileBase.NotFound e){
			root = null;
		}

		if(playlist != null && (root == null || !root.contains(playlist.path))){
			player.reset();
			status = Status.INITIAL;
			playlist = null;

			removeSavedStatus();
		}

		sendStatus();
	}

	private void play(){
		if(playlist != null){
			if(status == Status.READY){
				if(((AudioManager)getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
				== AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
					player.start();
					sendStatus();
					saveStatus();
					stopDeathTimer();
				}else{
					notifyError(getString(R.string.audiofocus_denied));
				}
			}else if(status == Status.LOADING_FROM_LASTEST){
				status = Status.LOADING;
			}else{
				load(false);
			}
		}
	}

	private void playByPath(@NonNull String path){
		try{
			RuuFile file = RuuFile.getInstance(getApplicationContext(), path);

			if(playlist == null || playlist.type != Playlist.Type.SIMPLE || !playlist.path.equals(file.getParent())){
				playlist = Playlist.getByMusicPath(getApplicationContext(), path);
				if(shuffleMode){
					playlist.shuffle(true);
				}
				load(false);
			}else{
				if(!playlist.getCurrent().equals(file)){
					playlist.goMusic(file);
					load(false);
				}else if(status == Status.LOADING_FROM_LASTEST){
					status = Status.LOADING;
				}else if(status == Status.READY){
					if(player.isPlaying()){
						player.pause();
					}
					player.seekTo(0);
					play();
				}else{
					load(false);
				}
			}
		}catch(RuuFileBase.NotFound e){
			notifyError(getString(R.string.cant_open_dir, e.path));
		}catch(Playlist.EmptyDirectory e){
			notifyError(getString(R.string.has_not_music, path));
		}catch(Playlist.NotFound e){
			notifyError(getString(R.string.music_not_found, path));
		}
	}

	private void playRecursive(String path){
		try{
			playlist = Playlist.getRecursive(getApplicationContext(), path);
		}catch(RuuFileBase.NotFound e){
			notifyError(getString(R.string.cant_open_dir, path));
			return;
		}catch(Playlist.EmptyDirectory e){
			notifyError(getString(R.string.has_not_music, path));
			return;
		}
		if(shuffleMode){
			playlist.shuffle(false);
		}
		load(false);
	}
	
	private void playBySearch(String path, String query){
		try{
			playlist = Playlist.getSearchResults(getApplicationContext(), RuuDirectory.getInstanceFromFullPath(getApplicationContext(), path), query);
		}catch(RuuFileBase.OutOfRootDirectory e){
			notifyError(getString(R.string.out_of_root, path));
		}catch(RuuFileBase.NotFound e){
			notifyError(getString(R.string.cant_open_dir, path));
			return;
		}catch(Playlist.EmptyDirectory e){
			notifyError(getString(R.string.search_no_hits, query));
			return;
		}
		if(shuffleMode){
			playlist.shuffle(false);
		}
		load(false);
	}

	private void load(boolean fromLastest){
		assert playlist != null;

		status = fromLastest ? Status.LOADING_FROM_LASTEST : Status.LOADING;
		player.reset();

		String realName = playlist.getCurrent().getRealPath();
		try{
			player.setDataSource(realName);
			player.prepareAsync();
		}catch(IOException e){
			notifyError(getString(R.string.failed_open_music, realName));
		}
	}

	private void pauseTransient(){
		if(status == Status.READY){
			if(player.isPlaying()){
				player.pause();
			}
			sendStatus();
			startDeathTimer();
		}
	}

	private void pause(){
		pauseTransient();
		((AudioManager)getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(focusListener);
		sendStatus();
		saveStatus();
	}

	private void seek(@IntRange(from=0) int newtime){
		if(0 <= newtime && newtime <= player.getDuration() && status == Status.READY){
			player.seekTo(newtime);
			sendStatus();
		}
	}

	private void setRepeatMode(@NonNull RepeatModeType mode){
		repeatMode = mode;

		sendStatus();
		preference.RepeatMode.set(repeatMode);
	}

	private void setShuffleMode(boolean mode){
		if(playlist != null){
			if(mode){
				playlist.shuffle(true);
			}else{
				playlist.sort();
			}
		}

		shuffleMode = mode;
		sendStatus();

		preference.ShuffleMode.set(shuffleMode);
	}

	private void next(){
		if(playlist != null){
			try{
				playlist.goNext();
				load(false);
			}catch(Playlist.EndOfList err){
				if(repeatMode.equals(RepeatModeType.LOOP)){
					if(shuffleMode){
						playlist.shuffle(false);
					}else{
						playlist.goFirst();
					}
					load(false);
				}else{
					showToast(getString(R.string.last_of_directory), false);
					if(!endOfListSE.isPlaying()){
						endOfListSE.start();
					}
					endpoints.onEndOfList(false, getPlayingStatus());
				}
			}
		}
	}

	private void prev(){
		if(playlist != null){
			if(player.getCurrentPosition() >= 3000){
				seek(0);
			}else{
				try{
					playlist.goPrev();
					load(false);
				}catch(Playlist.EndOfList err){
					if(repeatMode.equals(RepeatModeType.LOOP)){
						if(shuffleMode){
							playlist.shuffle(false);
						}else{
							playlist.goLast();
						}
						load(false);
					}else{
						showToast(getString(R.string.first_of_directory), false);
						if(!endOfListSE.isPlaying()){
							endOfListSE.start();
						}
						endpoints.onEndOfList(true, getPlayingStatus());
					}
				}
			}
		}
	}

	private void notifyError(@NonNull final String message){
		endpoints.onError(message, getPlayingStatus());
		showToast(message, true);
	}

	private void showToast(@NonNull final String message, final boolean show_long){
		final Handler handler = new Handler();
		(new Thread(new Runnable(){
			@Override
			public void run(){
				handler.post(new Runnable(){
					@Override
					public void run(){
						Toast.makeText(getApplicationContext(), message, show_long ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
					}
				});
			}
		})).start();
	}

	private void startDeathTimer(){
		stopDeathTimer();

		final Handler handler = new Handler();
		deathTimer = new Timer(true);
		deathTimer.schedule(new TimerTask(){
			@Override
			public void run(){
				handler.post(new Runnable(){
					@Override
					public void run(){
						if(!player.isPlaying()){
							stopSelf();
						}
					}
				});
			}
		}, 60 * 1000);
	}

	private void stopDeathTimer(){
		if(deathTimer != null){
			deathTimer.cancel();
			deathTimer = null;
		}
	}

	@Override
	public MediaBrowserServiceCompat.BrowserRoot onGetRoot(String clientPackageName, int cliendUid, Bundle rootHints) {
		String id = "";
		try{
			id = RuuDirectory.rootDirectory(getApplicationContext()).getFullPath();
		}catch(RuuFileBase.NotFound e){
		}
		return new MediaBrowserServiceCompat.BrowserRoot(id, null);
	}

	@Override
	public void onLoadChildren(String parentMediaId, final Result<List<MediaItem>> result){
		if(parentMediaId == null || parentMediaId.equals("")){
			result.sendResult(null);
			return;
		}

		try{
			result.sendResult(RuuDirectory.getInstance(getApplicationContext(), parentMediaId).getMediaItemList());
		}catch(RuuDirectory.NotFound e){
			result.sendResult(null);
		}
	}

	@Override
	public void onSearch(String query, Bundle extras, Result<List<MediaItem>> result){
		RuuDirectory dir = null;
		try{
			dir = RuuDirectory.getInstance(getApplicationContext(), extras.getCharSequence("path").toString());
		}catch(NullPointerException | RuuDirectory.NotFound e){
			try{
				dir = RuuDirectory.rootDirectory(getApplicationContext());
			}catch(RuuDirectory.NotFound f){
				result.sendResult(null);
			}
		}

		ArrayList<MediaItem> list = new ArrayList<>();
		for(RuuFileBase file: dir.search(query)){
			list.add(file.toMediaItem());
		}
		result.sendResult(list);
	}


	private final AudioManager.OnAudioFocusChangeListener focusListener = new AudioManager.OnAudioFocusChangeListener(){
		private float volume = 1.0f;


		@Override
		public void onAudioFocusChange(int focusChange){
			switch(focusChange){
				case AudioManager.AUDIOFOCUS_GAIN:
					if(volume < 1.0f){
						final Handler handler = new Handler();
						final Timer timer = new Timer(true);
						timer.schedule(new TimerTask(){
							@Override
							public void run(){
								handler.post(new Runnable(){
									@Override
									public void run(){
										volume += 0.1f;
										if(volume >= 1.0f){
											timer.cancel();
											volume = 1.0f;
										}
										player.setVolume(volume, volume);
									}
								});
							}
						}, 0, 20);
					}
					play();
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					pause();
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					volume = 0.0f;
					player.setVolume(volume, volume);
					pauseTransient();
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					volume = 0.3f;
					player.setVolume(volume, volume);
					break;
			}
		}
	};


	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(@Nullable Context context, @NonNull Intent intent){
			if(intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)){
				pause();
			}
		}
	};


	public class Controller{
		public void play(){
			RuuService.this.play();
		}

		public void play(String file){
			RuuService.this.playByPath(file);
		}

		public void play(long index){
			try{
				playlist.goQueueIndex(index);
			}catch(IndexOutOfBoundsException e){
			}

			load(false);
			play();
		}

		public void playRecursive(String dir){
			RuuService.this.playRecursive(dir);
		}

		public void playSearch(String dir, String query){
			RuuService.this.playBySearch(dir, query);
		}

		public void pause(){
			RuuService.this.pause();
		}

		public void pauseTransient(){
			RuuService.this.pauseTransient();
		}

		public void playPause(){
			if(player.isPlaying()){
				pause();
			}else{
				play();
			}
		}

		public void seek(long msec){
			RuuService.this.seek((int)msec);
		}

		public void setRepeatMode(RepeatModeType type){
			RuuService.this.setRepeatMode(type);
		}

		public void setShuffleMode(boolean enabled){
			RuuService.this.setShuffleMode(enabled);
		}

		public void next(){
			RuuService.this.next();
		}

		public void prev(){
			RuuService.this.prev();
		}

		public void sendStatus(){
			RuuService.this.sendStatus();
		}

		public void sendEqualizerInfo(){
			RuuService.this.sendEqualizerInfo();
		}
	}
}

