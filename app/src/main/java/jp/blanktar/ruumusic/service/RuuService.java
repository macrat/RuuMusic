package jp.blanktar.ruumusic.service;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.PresetReverb;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.KeyEvent;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.client.MainActivity;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


@WorkerThread
public class RuuService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{
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
	@Nullable private static RemoteControlClient remoteControlClient;

	@Nullable private Playlist playlist;

	@NonNull private String repeatMode = "off";
	private boolean shuffleMode = false;
	@NonNull private Status status = Status.INITIAL;

	@Nullable private BassBoost bassBoost = null;
	@Nullable private PresetReverb presetReverb = null;
	@Nullable private LoudnessEnhancer loudnessEnhancer = null;
	@Nullable private Equalizer equalizer = null;


	@Override
	@Nullable
	public IBinder onBind(@Nullable Intent intent){
		throw null;
	}

	@Override
	public void onCreate(){
		endOfListSE = MediaPlayer.create(getApplicationContext(), R.raw.eol);
		errorSE = MediaPlayer.create(getApplicationContext(), R.raw.err);

		repeatMode = Preference.Str.REPEAT_MODE.get(getApplicationContext());
		shuffleMode = Preference.Bool.SHUFFLE_MODE.get(getApplicationContext());

		String recursive = Preference.Str.RECURSIVE_PATH.get(getApplicationContext());
		if(recursive != null){
			try{
				playlist = Playlist.getRecursive(getApplicationContext(), recursive);
			}catch(RuuFileBase.NotFound | Playlist.EmptyDirectory e){
				playlist = null;
			}
		}else{
			String searchQuery = Preference.Str.SEARCH_QUERY.get(getApplicationContext());
			String searchPath = Preference.Str.SEARCH_PATH.get(getApplicationContext());
			if(searchQuery != null && searchPath != null){
				try{
					playlist = Playlist.getSearchResults(getApplicationContext(), searchPath, searchQuery);
				}catch(RuuFileBase.NotFound | Playlist.EmptyDirectory e){
					playlist = null;
				}
			}
		}

		String last_play = Preference.Str.LAST_PLAY_MUSIC.get(getApplicationContext());
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
				if(repeatMode.equals("one")){
					player.pause();
					play();
				}else{
					try{
						playlist.goNext();
						load(false);
					}catch(Playlist.EndOfList e){
						if(repeatMode.equals("off")){
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
					player.seekTo(Preference.Int.LAST_PLAY_POSITION.get(getApplicationContext()));
					sendStatus();
				}else{
					status = Status.READY;
					play();
				}
			}
		});

		updateAudioEffect();
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
				case ACTION_PLAY_SEARCH:
					try{
						if(intent.getAction().equals(ACTION_PLAY_RECURSIVE)){
							playlist = Playlist.getRecursive(getApplicationContext(), intent.getStringExtra("path"));
						}else{
							playlist = Playlist.getSearchResults(getApplicationContext(), intent.getStringExtra("path"), intent.getStringExtra("query"));
						}
					}catch(RuuFileBase.NotFound e){
						showToast(String.format(getString(R.string.cant_open_dir), intent.getStringExtra("path")), true);
						break;
					}catch(Playlist.EmptyDirectory e){
						showToast(String.format(getString(R.string.has_not_music), intent.getStringExtra("path")), true);
						break;
					}
					if(shuffleMode){
						playlist.shuffle(false);
					}
					load(false);
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
			}
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy(){
		removePlayingNotification();
		unregisterReceiver(broadcastReceiver);

		MediaButtonReceiver.onStopService(getApplicationContext());

		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);

		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);

		saveStatus();
	}

	@Override
	public void onSharedPreferenceChanged(@NonNull SharedPreferences preference, @NonNull String key){
		if(key.startsWith(Preference.AUDIO_PREFIX)){
			updateAudioEffect();
		}else if(key.equals("root_directory")){
			updateRoot();
		}
	}

	private void sendStatus(){
		Intent sendIntent = new Intent();

		sendIntent.setAction(ACTION_STATUS);

		sendIntent.putExtra("repeat", repeatMode);
		sendIntent.putExtra("shuffle", shuffleMode);

		if(playlist != null){
			sendIntent.putExtra("path", playlist.getCurrent().getFullPath());
			sendIntent.putExtra("recursivePath", playlist.type == Playlist.Type.RECURSIVE ? playlist.path.getFullPath() : null);
			sendIntent.putExtra("searchPath", playlist.type == Playlist.Type.SEARCH ? playlist.path.getFullPath() : null);
			sendIntent.putExtra("searchQuery", playlist.query);
		}

		if(status == Status.READY){
			sendIntent.putExtra("playing", player.isPlaying());
			sendIntent.putExtra("duration", player.getDuration());
			sendIntent.putExtra("current", player.getCurrentPosition());
			sendIntent.putExtra("basetime", System.currentTimeMillis() - player.getCurrentPosition());
		}

		getBaseContext().sendBroadcast(sendIntent);
	}

	private void saveStatus(){
		if(playlist != null){
			Preference.Str.LAST_PLAY_MUSIC.set(getApplicationContext(), playlist.getCurrent().getFullPath());
			Preference.Int.LAST_PLAY_POSITION.set(getApplicationContext(), player.getCurrentPosition());

			if(playlist.type == Playlist.Type.RECURSIVE){
				Preference.Str.RECURSIVE_PATH.set(getApplicationContext(), playlist.path.getFullPath());
			}else{
				Preference.Str.RECURSIVE_PATH.remove(getApplicationContext());
			}

			if(playlist.type == Playlist.Type.SEARCH){
				Preference.Str.SEARCH_PATH.set(getApplicationContext(), playlist.path.getFullPath());
				Preference.Str.SEARCH_QUERY.set(getApplicationContext(), playlist.query);
			}else{
				Preference.Str.SEARCH_PATH.remove(getApplicationContext());
				Preference.Str.SEARCH_QUERY.remove(getApplicationContext());
			}
		}
	}

	private void removeSavedStatus(){
		Preference.Str.LAST_PLAY_MUSIC.remove(getApplicationContext());
		Preference.Int.LAST_PLAY_POSITION.remove(getApplicationContext());
		Preference.Str.RECURSIVE_PATH.remove(getApplicationContext());
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

		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		intent.setAction(MainActivity.ACTION_OPEN_PLAYER);
		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

		String parentPath;
		try{
			parentPath = playlist.path.getRuuPath();
		}catch(RuuFileBase.OutOfRootDirectory e){
			parentPath = "";
		}

		return new NotificationCompat.Builder(getApplicationContext())
				.setSmallIcon(R.drawable.ic_play_notification)
				.setColor(0xff333333)
				.setTicker(playlist.getCurrent().getName())
				.setContentTitle(playlist.getCurrent().getName())
				.setContentText(parentPath)
				.setContentIntent(contentIntent)
				.setPriority(Notification.PRIORITY_LOW)
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.setCategory(Notification.CATEGORY_TRANSPORT)
				.addAction(R.drawable.ic_prev_for_notif, "prev", prev_pi)
				.addAction(playpause_icon, playpause_text, playpause_pi)
				.addAction(R.drawable.ic_next_for_notif, "next", next_pi)
				.build();
	}

	private void updatePlayingNotification(){
		if(player.isPlaying()){
			startForeground(1, makeNotification());
		}
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
		assert playlist != null;

		if(remoteControlClient != null && Build.VERSION.SDK_INT >= 14){
			String pathStr;
			try{
				pathStr = playlist.path.getRuuPath();
			}catch(RuuFileBase.OutOfRootDirectory e){
				pathStr = "";
			}
			remoteControlClient.editMetadata(true)
					.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playlist.getCurrent().getName())
					.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, player.getDuration())
					.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, pathStr)
					.apply();
			remoteControlClient.setPlaybackState(player.isPlaying() ? RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_PAUSED);
		}
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
			updateMediaMetadata();
		}else{
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
		}
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

					MediaButtonReceiver.onStartService(getApplicationContext());
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

	private void setRepeatMode(@NonNull String mode){
		if(mode.equals("off") || mode.equals("loop") || mode.equals("one")){
			repeatMode = mode;
			sendStatus();

			Preference.Str.REPEAT_MODE.set(getApplicationContext(), repeatMode);
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

		Preference.Bool.SHUFFLE_MODE.set(getApplicationContext(), shuffleMode);
	}

	private void next(){
		if(playlist != null){
			try{
				playlist.goNext();
				load(false);
			}catch(Playlist.EndOfList e){
				if(repeatMode.equals("loop")){
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
					if(repeatMode.equals("loop")){
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

	private void updateAudioEffect(){
		if(Preference.Bool.BASSBOOST_ENABLED.get(getApplicationContext())){
			if(bassBoost == null){
				bassBoost = new BassBoost(0, player.getAudioSessionId());
			}
			bassBoost.setStrength((short)Preference.Int.BASSBOOST_LEVEL.get(getApplicationContext()));
			bassBoost.setEnabled(true);
		}else if(bassBoost != null){
			bassBoost.release();
			bassBoost = null;
		}

		if(Preference.Bool.REVERB_ENABLED.get(getApplicationContext())){
			if(presetReverb == null){
				presetReverb = new PresetReverb(0, player.getAudioSessionId());
			}
			presetReverb.setPreset((short)Preference.Int.REVERB_TYPE.get(getApplicationContext()));
			presetReverb.setEnabled(true);
		}else if(presetReverb != null){
			presetReverb.release();
			presetReverb = null;
		}

		if(Build.VERSION.SDK_INT >= 19 && Preference.Bool.LOUDNESS_ENABLED.get(getApplicationContext())){
			if(loudnessEnhancer == null){
				loudnessEnhancer = new LoudnessEnhancer(player.getAudioSessionId());
			}
			loudnessEnhancer.setTargetGain(Preference.Int.LOUDNESS_LEVEL.get(getApplicationContext()));
			loudnessEnhancer.setEnabled(true);
		}else if(loudnessEnhancer != null){
			loudnessEnhancer.release();
			loudnessEnhancer = null;
		}

		if(Preference.Bool.EQUALIZER_ENABLED.get(getApplicationContext())){
			if(equalizer == null){
				equalizer = new Equalizer(0, player.getAudioSessionId());
			}
			for(short i=0; i<equalizer.getNumberOfBands(); i++){
				equalizer.setBandLevel(i, (short)Preference.IntArray.EQUALIZER_LEVEL.get(getApplicationContext(), i));
			}
			equalizer.setEnabled(true);
		}else if(equalizer != null){
			equalizer.release();
			equalizer = null;
		}
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
		@Nullable private static ComponentName componentName;
		private static boolean serviceRunning = false;
		private static boolean activityRunning = false;


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

		private static void registation(@NonNull Context context){
			if(componentName == null){
				AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
				componentName = new ComponentName(context.getPackageName(), MediaButtonReceiver.class.getName());
				audioManager.registerMediaButtonEventReceiver(componentName);

				if(Build.VERSION.SDK_INT >= 14 && remoteControlClient == null){
					Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
					mediaButtonIntent.setComponent(componentName);
					remoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0));
					remoteControlClient.setTransportControlFlags(
							RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
							RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
							RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
							RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS);
					audioManager.registerRemoteControlClient(remoteControlClient);
				}
			}
		}

		@WorkerThread
		public static void onStartService(@NonNull Context context){
			serviceRunning = true;
			registation(context);
		}

		@UiThread
		public static void onStartActivity(@NonNull Context context){
			activityRunning = true;
			registation(context);
		}

		private static void unregistation(@NonNull Context context){
			if(componentName != null && !serviceRunning && !activityRunning){
				AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
				audioManager.unregisterMediaButtonEventReceiver(componentName);
				componentName = null;

				if(remoteControlClient != null && Build.VERSION.SDK_INT >= 14){
					audioManager.unregisterRemoteControlClient(remoteControlClient);
					remoteControlClient = null;
				}
			}
		}

		@WorkerThread
		public static void onStopService(@NonNull Context context){
			serviceRunning = false;
			unregistation(context);
		}

		@UiThread
		public static void onStopActivity(@NonNull Context context){
			activityRunning = false;
			unregistation(context);
		}
	}
}

