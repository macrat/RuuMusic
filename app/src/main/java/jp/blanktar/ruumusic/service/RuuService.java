package jp.blanktar.ruumusic.service;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

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
import jp.blanktar.ruumusic.client.AudioPreferenceActivity;
import jp.blanktar.ruumusic.client.MainActivity;
import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


@WorkerThread
public class RuuService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{
	public final static String ACTION_PLAY = "jp.blanktar.ruumusic.PLAY";
	public final static String ACTION_PLAY_RECURSIVE = "jp.blanktar.ruumusic.PLAY_RECURSIVE";
	public final static String ACTION_PLAY_SEARCH = "jp.blanktar.ruumusic.PLAY_SEARCH";
	public final static String ACTION_PAUSE = "jp.blanktar.ruumusic.PAUSE";
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


	private MediaPlayer player;
	private MediaPlayer endOfListSE;
	private MediaPlayer errorSE;
	private Timer deathTimer;
	private static RemoteControlClient remoteControlClient;

	private Playlist playlist;

	private String repeatMode = "off";
	private boolean shuffleMode = false;
	private boolean ready = false;
	private boolean loadingWait = false;
	private boolean errored = false;
	private boolean playingFromLastest = true;

	private BassBoost bassBoost = null;
	private PresetReverb presetReverb = null;
	private LoudnessEnhancer loudnessEnhancer = null;
	private Equalizer equalizer = null;


	@Override
	@Nullable
	public IBinder onBind(@Nullable Intent intent){
		throw null;
	}

	@Override
	public void onCreate(){
		player = new MediaPlayer();
		endOfListSE = MediaPlayer.create(getApplicationContext(), R.raw.eol);
		errorSE = MediaPlayer.create(getApplicationContext(), R.raw.err);

		final SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		repeatMode = preference.getString("repeat_mode", "off");
		shuffleMode = preference.getBoolean("shuffle_mode", false);

		String recursive = preference.getString("recursive_path", null);
		if(recursive != null){
			try{
				playlist = Playlist.getRecursive(getApplicationContext(), recursive);
				if(shuffleMode){
					playlist.shuffle(false);
				}
			}catch(RuuFileBase.CanNotOpen | Playlist.EmptyDirectory e){
				playlist = null;
			}
		}

		if(playlist == null){
			String searchQuery = preference.getString("search_query", null);
			String searchPath = preference.getString("search_path", null);
			if(searchQuery != null && searchPath != null){
				try{
					playlist = Playlist.getSearchResults(getApplicationContext(), searchPath, searchQuery);
					if(shuffleMode){
						playlist.shuffle(false);
					}
				}catch(RuuFileBase.CanNotOpen | Playlist.EmptyDirectory e){
					playlist = null;
				}
			}
		}

		String last_play = preference.getString("last_play_music", "");
		if(!last_play.equals("")){
			if(playlist != null){
				try{
					playlist.goMusic(new RuuFile(getApplicationContext(), last_play));
				}catch(RuuFileBase.CanNotOpen | Playlist.NotFound e){
					playlist = null;
				}
			}else{
				try{
					playlist = Playlist.getByMusicPath(getApplicationContext(), last_play);
				}catch(RuuFileBase.CanNotOpen | Playlist.EmptyDirectory e){
					playlist = null;
				}
			}
			if(playlist != null){
				load(new MediaPlayer.OnPreparedListener(){
					@Override
					public void onPrepared(@Nullable MediaPlayer mp){
						ready = true;
						player.seekTo(preference.getInt("last_play_position", 0));
						if(loadingWait){
							play();
							loadingWait = false;
						}else{
							sendStatus();
						}
					}
				});
			}
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
						playCurrent();
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
							playCurrent();
						}
					}
				}
			}
		});

		player.setOnErrorListener(new MediaPlayer.OnErrorListener(){
			@Override
			public boolean onError(@Nullable MediaPlayer mp, int what, int extra){
				player.reset();

				if(!errored && playlist != null){
					String realName = playlist.getCurrent().getRealPath();

					Intent sendIntent = new Intent();
					sendIntent.setAction(ACTION_FAILED_PLAY);
					sendIntent.putExtra("path", (realName == null ? playlist.getCurrent().getFullPath() : realName));
					getBaseContext().sendBroadcast(sendIntent);
					sendStatus();

					if(!errorSE.isPlaying()){
						errorSE.start();
					}
				}

				ready = false;
				errored = true;
				removePlayingNotification();

				return true;
			}
		});

		registerReceiver(broadcastReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

		updateAudioEffect();
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);

		startDeathTimer();
	}

	@Override
	public int onStartCommand(@Nullable Intent intent, int flags, int startId){
		if(intent != null){
			switch(intent.getAction()){
				case ACTION_PLAY:
					play(intent.getStringExtra("path"));
					break;
				case ACTION_PLAY_RECURSIVE:
					try{
						playlist = Playlist.getRecursive(getApplicationContext(), intent.getStringExtra("path"));
					}catch(RuuFileBase.CanNotOpen e){
						showToast(String.format(getString(R.string.cant_open_dir), intent.getStringExtra("path")));
						break;
					}catch(Playlist.EmptyDirectory e){
						showToast(String.format(getString(R.string.has_not_music), intent.getStringExtra("path")));
						break;
					}
					if(shuffleMode){
						playlist.shuffle(false);
					}
					playCurrent();
					break;
				case ACTION_PLAY_SEARCH:
					try{
						playlist = Playlist.getSearchResults(getApplicationContext(), intent.getStringExtra("path"), intent.getStringExtra("query"));
					}catch(RuuFileBase.CanNotOpen e){
						showToast(String.format(getString(R.string.cant_open_dir), intent.getStringExtra("path")));
						break;
					}catch(Playlist.EmptyDirectory e){
						showToast(String.format(getString(R.string.has_not_music), intent.getStringExtra("path")));
						break;
					}
					if(shuffleMode){
						playlist.shuffle(false);
					}
					playCurrent();
					break;
				case ACTION_PAUSE:
					pause();
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

		saveStatus();
	}

	@Override
	public void onSharedPreferenceChanged(@NonNull SharedPreferences preference, @NonNull String key){
		if(key.startsWith(AudioPreferenceActivity.PREFERENCE_PREFIX)){
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

		if(ready){
			sendIntent.putExtra("playing", player.isPlaying());
			sendIntent.putExtra("duration", player.getDuration());
			sendIntent.putExtra("current", player.getCurrentPosition());
			sendIntent.putExtra("basetime", System.currentTimeMillis() - player.getCurrentPosition());
		}

		getBaseContext().sendBroadcast(sendIntent);
	}

	private void saveStatus(){
		if(playlist != null){
			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
					.putString("last_play_music", playlist.getCurrent().getFullPath())
					.putInt("last_play_position", player.getCurrentPosition())
					.putString("recursive_path", playlist.type == Playlist.Type.RECURSIVE ? playlist.path.getFullPath() : null)
					.putString("search_query", playlist.query)
					.putString("search_path", playlist.type == Playlist.Type.SEARCH ? playlist.path.getFullPath() : null)
					.apply();
		}
	}

	private void removeSavedStatus(){
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
				.remove("last_play_music")
				.remove("last_play_position")
				.remove("recursive_path")
				.apply();
	}

	@NonNull
	private Notification makeNotification(){
		int playpause_icon = player.isPlaying() ? R.drawable.ic_pause_for_notif : R.drawable.ic_play_for_notif;
		String playpause_text = player.isPlaying() ? "pause" : "play";
		PendingIntent playpause_pi = PendingIntent.getService(getApplicationContext(), 0, (new Intent(getApplicationContext(), RuuService.class)).setAction(player.isPlaying() ? ACTION_PAUSE : ACTION_PLAY), 0);

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
				.setPriority(Notification.PRIORITY_MIN)
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.setCategory(Notification.CATEGORY_TRANSPORT)
				.addAction(R.drawable.ic_prev_for_notif, "prev", prev_pi)
				.addAction(playpause_icon, playpause_text, playpause_pi)
				.addAction(R.drawable.ic_next_for_notif, "next", next_pi)
				.build();
	}

	private void updatePlayingNotification(){
		if(!player.isPlaying()){
			return;
		}

		startForeground(1, makeNotification());
	}

	private void removePlayingNotification(){
		if(player.isPlaying()){
			return;
		}

		stopForeground(true);

		if(Build.VERSION.SDK_INT >= 16 && playlist != null){
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, makeNotification());
		}
	}

	private void updateMediaMetadata(){
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
		}catch(RuuFileBase.CanNotOpen e){
			root = null;
		}

		if(playlist != null && (root == null || !root.contains(playlist.path))){
			if(player.isPlaying()){
				stopForeground(true);
			}else{
				((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
			}
			player.reset();

			removeSavedStatus();

			ready = false;
			playlist = null;

			sendStatus();
		}else if(player.isPlaying()){
			updatePlayingNotification();
			updateMediaMetadata();
		}else{
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
		}
	}

	private void play(){
		if(playlist != null){
			if(ready){
				errored = false;

				((AudioManager)getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

				player.start();
				sendStatus();
				updatePlayingNotification();
				updateMediaMetadata();
				saveStatus();
				stopDeathTimer();

				MediaButtonReceiver.onStartService(getApplicationContext());
			}else if(!errored){
				loadingWait = true;
			}else{
				playCurrent();
			}
		}
	}

	private void play(@Nullable String path){
		if(path == null){
			play();
			return;
		}

		if(playlist != null && playlist.type == Playlist.Type.SIMPLE){
			RuuFile file;
			try{
				file = new RuuFile(getApplicationContext(), path);
			}catch(RuuFileBase.CanNotOpen e){
				showToast(String.format(getString(R.string.cant_open_dir), path));
				return;
			}

			if(playlist.getCurrent().equals(file)){
				if(ready){
					if(player.isPlaying()){
						player.pause();
					}
					player.seekTo(0);
					play();
					return;
				}else if(!errored){
					loadingWait = true;
					return;
				}
			}
		}

		try{
			playlist = Playlist.getByMusicPath(getApplicationContext(), path);
		}catch(RuuFileBase.CanNotOpen e){
			showToast(String.format(getString(R.string.cant_open_dir), e.path));
			return;
		}catch(Playlist.EmptyDirectory e){
			showToast(String.format(getString(R.string.has_not_music), path));
			return;
		}
		playCurrent();
	}

	private void playCurrent(){
		if(playlist != null && (playingFromLastest || ready || errored)){
			load(new MediaPlayer.OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer mp){
					ready = true;
					playingFromLastest = false;
					errored = false;
					play();
				}
			});
		}
	}

	private void load(@NonNull MediaPlayer.OnPreparedListener onPrepared){
		ready = false;
		player.reset();

		String realName = playlist.getCurrent().getRealPath();
		if(realName == null){
			showToast(String.format(getString(R.string.music_not_found), playlist.getCurrent().getFullPath()));
			return;
		}

		try{
			player.setDataSource(realName);

			player.setOnPreparedListener(onPrepared);
			player.prepareAsync();
		}catch(IOException e){
			showToast(String.format(getString(R.string.failed_open_music), realName));
		}
	}

	private void pause(){
		player.pause();
		sendStatus();
		removePlayingNotification();
		updateMediaMetadata();
		saveStatus();
		startDeathTimer();

		((AudioManager)getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(focusListener);
	}

	private void seek(int newtime){
		if(0 <= newtime && newtime <= player.getDuration()){
			player.seekTo(newtime);
			sendStatus();
			saveStatus();
		}
	}

	private void setRepeatMode(@NonNull String mode){
		if(mode.equals("off") || mode.equals("loop") || mode.equals("one")){
			repeatMode = mode;
			sendStatus();

			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
					.putString("repeat_mode", repeatMode)
					.apply();
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

		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
				.putBoolean("shuffle_mode", shuffleMode)
				.apply();
	}

	private void next(){
		if(playlist != null){
			try{
				playlist.goNext();
				playCurrent();
			}catch(Playlist.EndOfList e){
				if(repeatMode.equals("loop")){
					if(shuffleMode){
						playlist.shuffle(false);
					}else{
						playlist.goFirst();
					}
					playCurrent();
				}else{
					showToast(getString(R.string.last_of_directory));
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
					playCurrent();
				}catch(Playlist.EndOfList e){
					if(repeatMode.equals("loop")){
						if(shuffleMode){
							playlist.shuffle(false);
						}else{
							playlist.goLast();
						}
						playCurrent();
					}else{
						showToast(getString(R.string.first_of_directory));
						if(!endOfListSE.isPlaying()){
							endOfListSE.start();
						}
					}
				}
			}
		}
	}

	private void showToast(@NonNull final String message){
		final Handler handler = new Handler();
		(new Thread(new Runnable(){
			@Override
			public void run(){
				handler.post(new Runnable(){
					@Override
					public void run(){
						Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
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
		if(getPreference(AudioPreferenceActivity.PREFERENCE_BASSBOOST_ENABLED, false)){
			if(bassBoost == null){
				bassBoost = new BassBoost(0, player.getAudioSessionId());
			}
			bassBoost.setStrength((short)getPreference(AudioPreferenceActivity.PREFERENCE_BASSBOOST_LEVEL, 0));
			bassBoost.setEnabled(true);
		}else if(bassBoost != null){
			bassBoost.release();
			bassBoost = null;
		}

		if(getPreference(AudioPreferenceActivity.PREFERENCE_REVERB_ENABLED, false)){
			if(presetReverb == null){
				presetReverb = new PresetReverb(0, player.getAudioSessionId());
			}
			presetReverb.setPreset((short)getPreference(AudioPreferenceActivity.PREFERENCE_REVERB_TYPE, 0));
			presetReverb.setEnabled(true);
		}else if(presetReverb != null){
			presetReverb.release();
			presetReverb = null;
		}

		if(Build.VERSION.SDK_INT >= 19 && getPreference(AudioPreferenceActivity.PREFERENCE_LOUDNESS_ENABLED, false)){
			if(loudnessEnhancer == null){
				loudnessEnhancer = new LoudnessEnhancer(player.getAudioSessionId());
			}
			loudnessEnhancer.setTargetGain(getPreference(AudioPreferenceActivity.PREFERENCE_LOUDNESS_LEVEL, 0));
			loudnessEnhancer.setEnabled(true);
		}else if(loudnessEnhancer != null){
			loudnessEnhancer.release();
			loudnessEnhancer = null;
		}

		if(getPreference(AudioPreferenceActivity.PREFERENCE_EQUALIZER_ENABLED, false)){
			if(equalizer == null){
				equalizer = new Equalizer(0, player.getAudioSessionId());
			}
			for(short i=0; i<equalizer.getNumberOfBands(); i++){
				equalizer.setBandLevel(i, (short)getPreference(AudioPreferenceActivity.PREFERENCE_EQUALIZER_VALUE + i, (equalizer.getBandLevelRange()[0] + equalizer.getBandLevelRange()[1])/2));
			}
			equalizer.setEnabled(true);
		}else if(equalizer != null){
			equalizer.release();
			equalizer = null;
		}
	}

	private boolean getPreference(@NonNull String key, boolean default_value){
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(key, default_value);
	}

	private int getPreference(@NonNull String key, int default_value){
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(key, default_value);
	}


	private final AudioManager.OnAudioFocusChangeListener focusListener = new AudioManager.OnAudioFocusChangeListener(){
		@Override
		public void onAudioFocusChange(int focusChange){
			if(focusChange == AudioManager.AUDIOFOCUS_LOSS){
				pause();
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
		private static ComponentName componentName;
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
