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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.client.main.MainActivity;
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
	@Nullable private static MediaSessionCompat mediaSession;
	@Nullable private Preference preference;

	@Nullable private Playlist playlist;

	@NonNull private RepeatModeType repeatMode = RepeatModeType.OFF;
	private boolean shuffleMode = false;
	@NonNull private Status status = Status.INITIAL;

	@Nullable private EffectManager effectManager = null;


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
					getBaseContext().sendBroadcast((new Intent(ACTION_FAILED_PLAY))
							.putExtra("path", playlist.getCurrent().getRealPath()));
					sendStatus();

					if(!errorSE.isPlaying()){
						errorSE.start();
					}
				}

				status = Status.ERRORED;
				removePlayingNotification();

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

		ComponentName componentName = new ComponentName(getApplicationContext().getPackageName(), MediaButtonReceiver.class.getName());
		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(componentName);

		mediaSession = new MediaSessionCompat(getApplicationContext(), "RuuMusicService", componentName, PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0));
		mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
		mediaSession.setActive(true);

		setSessionToken(mediaSession.getSessionToken());

		mediaSession.setCallback(new MediaSessionCompat.Callback(){
			@Override
			public void onPlay(){
				play();
			}

			@Override
			public void onPlayFromMediaId(String mediaId, Bundle extras){
				playByPath(mediaId);
			}

			@Override
			public void onPlayFromUri(Uri uri, Bundle extras){
				playByPath(uri.getPath());
			}

			@Override
			public void onPlayFromSearch(String query, Bundle extras){
				playBySearch(extras.getCharSequence("path", preference.RootDirectory.get()).toString(), query);
			}

			@Override
			public void onSkipToQueueItem(long id){
				try{
					playlist.goQueueIndex(id);
				}catch(IndexOutOfBoundsException e){
				}
				load(false);
				play();
			}

			@Override
			public void onPause(){
				pause();
			}

			@Override
			public void onStop(){
				pause();
			}

			@Override
			public void onSkipToNext(){
				next();
			}

			@Override
			public void onSkipToPrevious(){
				prev();
			}

			@Override
			public void onSeekTo(long pos){
				seek((int)pos);
			}

			@Override
			public void onSetRepeatMode(int repeatMode){
				switch(repeatMode){
					case PlaybackStateCompat.REPEAT_MODE_NONE:
						setRepeatMode(RepeatModeType.OFF);
						break;
					case PlaybackStateCompat.REPEAT_MODE_ONE:
						setRepeatMode(RepeatModeType.SINGLE);
						break;
					case PlaybackStateCompat.REPEAT_MODE_ALL:
						setRepeatMode(RepeatModeType.LOOP);
						break;
				}
			}

			@Override
			public void onSetShuffleModeEnabled(boolean shuffleMode){
				setShuffleMode(shuffleMode);
			}
		});

		effectManager = new EffectManager(player, getApplicationContext());
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		registerReceiver(broadcastReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

		startDeathTimer();
	}

	@Override
	public int onStartCommand(@Nullable Intent intent, int flags, int startId){
		if(intent != null){
			switch(intent.getAction()){
				case ACTION_PLAY:
					String path = intent.getStringExtra("path");
					if(path == null){
						play();
					}else{
						playByPath(path);
					}
					break;
				case ACTION_PLAY_RECURSIVE:
					playRecursive(intent.getStringExtra("path"));
					break;
				case ACTION_PLAY_SEARCH:
					playBySearch(intent.getStringExtra("path"), intent.getStringExtra("query"));
					break;
				case ACTION_PAUSE:
					pause();
					break;
				case ACTION_PAUSE_TRANSIENT:
					pauseTransient();
					break;
				case ACTION_PLAY_PAUSE:
					if(player.isPlaying()){
						pause();
					}else{
						play();
					}
					break;
				case ACTION_SEEK:
					seek(intent.getIntExtra("newtime", -1));
					break;
				case ACTION_REPEAT:
					setRepeatMode(intent.getStringExtra("mode"));
					break;
				case ACTION_SHUFFLE:
					setShuffleMode(intent.getBooleanExtra("mode", false));
					break;
				case ACTION_PING:
					sendStatus();
					break;
				case ACTION_NEXT:
					next();
					break;
				case ACTION_PREV:
					prev();
					break;
				case ACTION_REQUEST_EQUALIZER_INFO:
					sendEqualizerInfo();
					break;
			}
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy(){
		removePlayingNotification();
		unregisterReceiver(broadcastReceiver);

		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);

		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);

		effectManager.release();
		mediaSession.release();

		saveStatus();
	}

	@Override
	public void onSharedPreferenceChanged(@NonNull SharedPreferences p, @NonNull String key){
		if(key.equals("root_directory")){
			updateRoot();
		}
	}

	private void sendStatus(){
		Intent sendIntent = new Intent();

		sendIntent.setAction(ACTION_STATUS);

		sendIntent.putExtra("repeat", repeatMode.name());
		sendIntent.putExtra("shuffle", shuffleMode);

		if(playlist != null){
			sendIntent.putExtra("path", playlist.getCurrent().getFullPath());
			sendIntent.putExtra("recursivePath", playlist.type == Playlist.Type.RECURSIVE ? playlist.path.getFullPath() : null);
			sendIntent.putExtra("searchPath", playlist.type == Playlist.Type.SEARCH ? playlist.path.getFullPath() : null);
			sendIntent.putExtra("searchQuery", playlist.query);
		}

		if(status == Status.READY){
			sendIntent.putExtra("playing", player.isPlaying());
			sendIntent.putExtra("duration", (long)player.getDuration());
			sendIntent.putExtra("current", (long)player.getCurrentPosition());
			sendIntent.putExtra("basetime", System.currentTimeMillis() - player.getCurrentPosition());
		}

		getBaseContext().sendBroadcast(sendIntent);
	}

	private void sendEqualizerInfo(){
		getBaseContext().sendBroadcast(effectManager.getEqualizerInfo().toIntent());
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

	@NonNull
	private Notification makeNotification(){
		assert playlist != null;

		int playpause_icon = player.isPlaying() ? R.drawable.ic_pause_for_notif : R.drawable.ic_play_for_notif;
		String playpause_text = player.isPlaying() ? "pause" : "play";
		PendingIntent playpause_pi = PendingIntent.getService(
				getApplicationContext(),
				0,
				(new Intent(getApplicationContext(), RuuService.class)).setAction(player.isPlaying() ? ACTION_PAUSE : ACTION_PLAY),
				0);

		PendingIntent prev_pi = PendingIntent.getService(getApplicationContext(), 0, (new Intent(getApplicationContext(), RuuService.class)).setAction(ACTION_PREV), 0);
		PendingIntent next_pi = PendingIntent.getService(getApplicationContext(), 0, (new Intent(getApplicationContext(), RuuService.class)).setAction(ACTION_NEXT), 0);

		int shuffle_icon = shuffleMode ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off;
		PendingIntent shuffle_pi = PendingIntent.getService(
				getApplicationContext(),
				0,
				(new Intent(getApplicationContext(), RuuService.class)).setAction(ACTION_SHUFFLE).putExtra("mode", !shuffleMode),
				PendingIntent.FLAG_CANCEL_CURRENT);

		int repeat_icon = R.drawable.ic_repeat_off;
		switch(repeatMode){
			case OFF:
				repeat_icon = R.drawable.ic_repeat_off;
				break;
			case SINGLE:
				repeat_icon = R.drawable.ic_repeat_one;
				break;
			case LOOP:
				repeat_icon = R.drawable.ic_repeat_all;
				break;
		}
		PendingIntent repeat_pi = PendingIntent.getService(
				getApplicationContext(),
				0,
				(new Intent(getApplicationContext(), RuuService.class)).setAction(ACTION_REPEAT).putExtra("mode", repeatMode.getNext().name()),
				PendingIntent.FLAG_CANCEL_CURRENT);

		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		intent.setAction(MainActivity.ACTION_OPEN_PLAYER);
		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

		String parentPath;
		try{
			parentPath = playlist.getCurrent().getParent().getRuuPath();
		}catch(RuuFileBase.OutOfRootDirectory e){
			parentPath = "";
		}

		return new NotificationCompat.Builder(getApplicationContext())
				.setSmallIcon(R.drawable.ic_play_notification)
				.setTicker(playlist.getCurrent().getName())
				.setContentTitle(playlist.getCurrent().getName())
				.setContentText(parentPath)
				.setContentIntent(contentIntent)
				.setPriority(Notification.PRIORITY_LOW)
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.setCategory(Notification.CATEGORY_TRANSPORT)
				.addAction(shuffle_icon, "shuffle", shuffle_pi)
				.addAction(R.drawable.ic_prev_for_notif, "prev", prev_pi)
				.addAction(playpause_icon, playpause_text, playpause_pi)
				.addAction(R.drawable.ic_next_for_notif, "next", next_pi)
				.addAction(repeat_icon, "repeat", repeat_pi)
				.setStyle(new NotificationCompat.MediaStyle()
						.setMediaSession(mediaSession.getSessionToken())
						.setShowActionsInCompactView(1, 2, 3))
				.build();
	}

	private void updatePlayingNotification(){
		startForeground(1, makeNotification());
	}

	private void removePlayingNotification(){
		if(!player.isPlaying()){
			stopForeground(true);

			if(Build.VERSION.SDK_INT >= 16 && playlist != null){
				((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, makeNotification());
			}
		}
	}

	private void updateMediaMetadata(){
		if(playlist == null){
			return;
		}

		String parentPath;
		try{
			parentPath = playlist.getCurrent().getParent().getRuuPath();
		}catch(RuuFileBase.OutOfRootDirectory e){
			parentPath = "";
		}

		mediaSession.setMetadata(new MediaMetadataCompat.Builder()
				.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, parentPath)
				.putString(MediaMetadataCompat.METADATA_KEY_TITLE, playlist.getCurrent().getName())
				.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.getDuration())
				.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.drawable.display_icon))
				.build());

		mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
				.setState(player.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
						player.getCurrentPosition(),
						1.0f)
				.setActiveQueueItemId(playlist.getQueueIndex())
				.setActions(PlaybackStateCompat.ACTION_PLAY
						| PlaybackStateCompat.ACTION_PAUSE
						| PlaybackStateCompat.ACTION_PLAY_PAUSE
						| PlaybackStateCompat.ACTION_SKIP_TO_NEXT
						| PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
						| PlaybackStateCompat.ACTION_SET_REPEAT_MODE
						| PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED
						| PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
						| PlaybackStateCompat.ACTION_PLAY_FROM_URI
						| PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
				).build());

		mediaSession.setQueue(playlist.getMediaSessionQueue());
		mediaSession.setQueueTitle(playlist.title);
	}

	private void updateRoot(){
		RuuDirectory root;
		try{
			root = RuuDirectory.rootDirectory(getApplicationContext());
		}catch(RuuFileBase.NotFound e){
			root = null;
		}

		if(playlist != null && (root == null || !root.contains(playlist.path))){
			if(player.isPlaying()){
				stopForeground(true);
			}else{
				((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
			}

			player.reset();
			status = Status.INITIAL;
			playlist = null;

			sendStatus();
			removeSavedStatus();
		}else if(player.isPlaying()){
			updatePlayingNotification();
		}else{
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
		}

		updateMediaMetadata();
	}

	private void play(){
		if(playlist != null){
			if(status == Status.READY){
				if(((AudioManager)getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
				== AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
					player.start();
					sendStatus();
					updatePlayingNotification();
					updateMediaMetadata();
					saveStatus();
					stopDeathTimer();
				}else{
					showToast(getString(R.string.audiofocus_denied), true);
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
			showToast(String.format(getString(R.string.cant_open_dir), e.path), true);
		}catch(Playlist.EmptyDirectory e){
			showToast(String.format(getString(R.string.has_not_music), path), true);
		}catch(Playlist.NotFound e){
			showToast(String.format(getString(R.string.music_not_found), path), true);
		}
	}

	private void playRecursive(String path){
		try{
			playlist = Playlist.getRecursive(getApplicationContext(), path);
		}catch(RuuFileBase.NotFound e){
			showToast(String.format(getString(R.string.cant_open_dir), path), true);
			return;
		}catch(Playlist.EmptyDirectory e){
			showToast(String.format(getString(R.string.has_not_music), path), true);
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
			showToast(String.format(getString(R.string.out_of_root), path), true);
		}catch(RuuFileBase.NotFound e){
			showToast(String.format(getString(R.string.cant_open_dir), path), true);
			return;
		}catch(Playlist.EmptyDirectory e){
			showToast(String.format(getString(R.string.has_not_music), path), true);
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
			showToast(String.format(getString(R.string.failed_open_music), realName), true);
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
		removePlayingNotification();
		updateMediaMetadata();
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

		switch(mode){
			case OFF:
				mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);
				break;
			case SINGLE:
				mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE);
				break;
			case LOOP:
				mediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);
				break;
		}

		sendStatus();
		updatePlayingNotification();
		preference.RepeatMode.set(repeatMode);
	}

	private void setRepeatMode(@NonNull String mode){
		try{
			setRepeatMode(RepeatModeType.valueOf(mode));
		}catch(IllegalArgumentException e) {
			return;
		}
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
		updatePlayingNotification();
		mediaSession.setShuffleModeEnabled(mode);

		preference.ShuffleMode.set(shuffleMode);
	}

	private void next(){
		if(playlist != null){
			try{
				playlist.goNext();
				load(false);
			}catch(Playlist.EndOfList e){
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
				}catch(Playlist.EndOfList e){
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
					}
				}
			}
		}
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


	public static class MediaButtonReceiver extends BroadcastReceiver{
		@Override
		@WorkerThread
		public void onReceive(@NonNull Context context, @NonNull Intent intent){
			if(intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)){
				KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
				if(keyEvent.getAction() != KeyEvent.ACTION_UP){
					return;
				}
				switch(keyEvent.getKeyCode()){
					case KeyEvent.KEYCODE_MEDIA_PLAY:
						sendIntent(context, ACTION_PLAY);
						break;
					case KeyEvent.KEYCODE_MEDIA_PAUSE:
						sendIntent(context, ACTION_PAUSE);
						break;
					case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					case KeyEvent.KEYCODE_HEADSETHOOK:
						sendIntent(context, ACTION_PLAY_PAUSE);
						break;
					case KeyEvent.KEYCODE_MEDIA_NEXT:
						sendIntent(context, ACTION_NEXT);
						break;
					case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
						sendIntent(context, ACTION_PREV);
						break;
				}
			}
		}

		@WorkerThread
		private void sendIntent(@NonNull Context context, @NonNull String event){
			context.startService((new Intent(context, RuuService.class)).setAction(event));
		}
	}
}

