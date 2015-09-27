package jp.blanktar.ruumusic;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.IBinder;
import android.media.MediaPlayer;
import android.os.Handler;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.os.Build;
import android.media.AudioManager;
import android.content.ComponentName;
import android.view.KeyEvent;
import android.text.TextUtils;


@WorkerThread
public class RuuService extends Service{
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
	public final static String ACTION_ROOT_CHANGED = "jp.blanktar.ruumusic.ROOT_CHANGED";
	public final static String ACTION_PING = "jp.blanktar.ruumusic.PING";
	public final static String ACTION_STATUS = "jp.blanktar.ruumusic.STATUS";
	public final static String ACTION_FAILED_PLAY = "jp.blanktar.ruumusic.FAILED_PLAY";
	public final static String ACTION_NOT_FOUND = "jp.blanktar.ruumusic.NOT_FOUND";

	private RuuFile path;
	private MediaPlayer player;
	private String repeatMode = "off";
	private boolean shuffleMode = false;
	private boolean ready = false;
	private Timer deathTimer;
	private boolean loadingWait = false;
	private boolean errored = false;
	private RuuDirectory recursivePath = null;
	private String searchQuery = null;
	private RuuDirectory searchPath = null;

	private List<RuuFile> playlist;
	private int currentIndex;

	private MediaPlayer endOfListSE;
	private MediaPlayer errorSE;


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
				recursivePath = RuuDirectory.getInstance(getApplicationContext(), recursive);
			}catch(RuuFileBase.CanNotOpen e){
				recursivePath = null;
			}
		}

		searchQuery = preference.getString("search_query", null);
		String searchPathStr = preference.getString("search_path", null);
		if(searchQuery != null && searchPathStr != null){
			try{
				searchPath = RuuDirectory.getInstance(getApplicationContext(), searchPathStr);
				playSearch(searchPath, searchQuery);
			}catch(RuuFileBase.CanNotOpen e){
				searchPath = null;
				searchQuery = null;
			}
		}

		String last_play = preference.getString("last_play_music", "");
		if(!last_play.equals("")){
			try{
				load(new RuuFile(getApplicationContext(), last_play), new MediaPlayer.OnPreparedListener(){
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
			}catch(RuuFileBase.CanNotOpen e){
				removeSavedStatus();
			}
		}

		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
			@Override
			public void onCompletion(@Nullable MediaPlayer mp){
				if(repeatMode.equals("one")){
					player.pause();
					play();
				}else{
					if(playlist == null || currentIndex + 1 >= playlist.size() && repeatMode.equals("off")){
						player.pause();
						player.seekTo(0);
						pause();
					}else{
						if(shuffleMode && currentIndex + 1 >= playlist.size()){
							shufflePlay();
						}else{
							play(playlist.get((currentIndex + 1) % playlist.size()));
						}
					}
				}
			}
		});

		player.setOnErrorListener(new MediaPlayer.OnErrorListener(){
			@Override
			public boolean onError(@Nullable MediaPlayer mp, int what, int extra){
				player.reset();

				if(!errored && path != null){
					String realName = path.getRealPath();

					Intent sendIntent = new Intent();
					sendIntent.setAction(ACTION_FAILED_PLAY);
					sendIntent.putExtra("path", (realName == null ? path.getFullPath() : realName));
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

		startDeathTimer();
	}

	@Override
	public int onStartCommand(@Nullable Intent intent, int flags, int startId){
		if(intent != null){
			switch(intent.getAction()){
				case ACTION_PLAY:
					String newpath = intent.getStringExtra("path");
					if(newpath != null){
						if(recursivePath != null){
							recursivePath = null;
							playlist = null;
						}
						searchPath = null;
						searchQuery = null;
					}
					play(newpath);
					errored = false;
					break;
				case ACTION_PLAY_RECURSIVE:
					playRecursive(intent.getStringExtra("path"));
					searchQuery = null;
					searchPath = null;
					break;
				case ACTION_PLAY_SEARCH:
					playSearch(intent.getStringExtra("path"), intent.getStringExtra("query"));
					recursivePath = null;
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
				case ACTION_ROOT_CHANGED:
					updateRoot();
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

		saveStatus();
	}

	private void sendStatus(){
		Intent sendIntent = new Intent();

		sendIntent.setAction(ACTION_STATUS);
		if(path != null){
			sendIntent.putExtra("path", path.getFullPath());
		}
		sendIntent.putExtra("repeat", repeatMode);
		sendIntent.putExtra("shuffle", shuffleMode);

		if(recursivePath == null){
			sendIntent.putExtra("recursivePath", (String)null);
		}else{
			sendIntent.putExtra("recursivePath", recursivePath.getFullPath());
		}

		sendIntent.putExtra("searchQuery", searchQuery);
		sendIntent.putExtra("searchPath", searchPath == null ? null : searchPath.getFullPath());

		if(ready){
			sendIntent.putExtra("playing", player.isPlaying());
			sendIntent.putExtra("duration", player.getDuration());
			sendIntent.putExtra("current", player.getCurrentPosition());
			sendIntent.putExtra("basetime", System.currentTimeMillis() - player.getCurrentPosition());
		}

		getBaseContext().sendBroadcast(sendIntent);
	}

	private void saveStatus(){
		if(path != null){
			String recursive = null;
			if(recursivePath != null){
				recursive = recursivePath.getFullPath();
			}
			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
					.putString("last_play_music", path.getFullPath())
					.putInt("last_play_position", player.getCurrentPosition())
					.putString("recursive_path", recursive)
					.putString("search_query", searchQuery)
					.putString("search_path", searchPath == null ? null : searchPath.getFullPath())
					.apply();
		}
	}

	private void removeSavedStatus(){
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
				.putString("last_play_music", "")
				.putInt("last_play_position", 0)
				.putString("recursive_path", null)
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
		intent.setAction("jp.blanktar.ruumusic.OPEN_PLAYER");
		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

		String parentPath;
		try{
			parentPath = path.getParent().getRuuPath();
		}catch(RuuFileBase.OutOfRootDirectory | RuuFileBase.CanNotOpen e){
			parentPath = "";
		}

		return new NotificationCompat.Builder(getApplicationContext())
				.setSmallIcon(R.drawable.ic_play_notification)
				.setColor(0xff333333)
				.setTicker(path.getName())
				.setContentTitle(path.getName())
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

		if(Build.VERSION.SDK_INT >= 16 && path != null){
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, makeNotification());
		}
	}

	private void updateRoot(){
		RuuDirectory root;
		try{
			root = RuuDirectory.rootDirectory(getApplicationContext());
		}catch(RuuFileBase.CanNotOpen e){
			root = null;
		}

		if(path != null && (root == null || !root.contains(path))){
			if(player.isPlaying()){
				stopForeground(true);
			}else{
				((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
			}
			player.reset();

			removeSavedStatus();

			path = null;
			ready = false;
			playlist = null;

			sendStatus();
		}else if(player.isPlaying()){
			updatePlayingNotification();
		}else{
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
		}
	}

	private void play(){
		if(path != null){
			if(ready){
				errored = false;

				player.start();
				sendStatus();
				updatePlayingNotification();
				saveStatus();
				stopDeathTimer();

				MediaButtonReceiver.onStartService(getApplicationContext());
			}else if(!errored){
				loadingWait = true;
			}else{
				play(path);
			}
		}
	}

	private void play(@Nullable String path){
		if(path == null){
			play();
		}else{
			try{
				play(new RuuFile(getApplicationContext(), path));
			}catch(RuuFileBase.CanNotOpen e){
				showToast(String.format(getString(R.string.cant_open_dir), path));
			}
		}
	}

	private void play(@NonNull RuuFile path){
		if(this.path != null && this.path.equals(path) && playlist != null){
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

		if(ready || errored || this.path == null){
			load(path, new MediaPlayer.OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer mp){
					ready = true;
					play();
				}
			});
		}
	}

	private void playRecursive(@NonNull RuuDirectory dir){
		if(playlist == null || recursivePath == null || !recursivePath.equals(dir)){
			try{
				playlist = dir.getMusicsRecursive();
			}catch(RuuFileBase.CanNotOpen e){
				showToast(String.format(getString(R.string.cant_open_dir), e.path));
				return;
			}
			recursivePath = dir;
		}

		if(shuffleMode){
			shufflePlay();
		}else{
			path = playlist.get(0);

			load(path, new MediaPlayer.OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer mp){
					ready = true;
					play();
				}
			});
		}
	}

	private void playRecursive(@Nullable String path){
		if(path != null){
			try{
				playRecursive(RuuDirectory.getInstance(getApplicationContext(), path));
			}catch(RuuFileBase.CanNotOpen e){
				showToast(String.format(getString(R.string.cant_open_dir), e.path));
			}
		}
	}

	private void playSearch(@NonNull RuuDirectory dir, @NonNull String query){
		String[] queries = TextUtils.split(query.toLowerCase(), " \t");

		recursivePath = null;
		playlist = new ArrayList<>();
		try{
			for(RuuFile music: dir.getMusicsRecursive()){
				String name = music.getName().toLowerCase();
				boolean isOk = true;
				for(String qs: queries){
					if(!name.contains(qs)){
						isOk = false;
						break;
					}
				}
				if(isOk){
					playlist.add(music);
				}
			}
		}catch(RuuFileBase.CanNotOpen e){
			return;
		}

		searchQuery = query;
		searchPath = dir;

		if(shuffleMode){
			shufflePlay();
		}else{
			path = playlist.get(0);
			searchQuery = query;

			load(path, new MediaPlayer.OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer mp){
					ready = true;
					play();
				}
			});
		}
	}

	private void playSearch(@Nullable String path, @Nullable String query){
		if(!TextUtils.isEmpty(path) && !TextUtils.isEmpty(query)){
			try{
				playSearch(RuuDirectory.getInstance(getApplicationContext(), path), query);
			}catch(RuuFileBase.CanNotOpen e){
				return;
			}
		}
	}

	private void load(@NonNull RuuFile path, @NonNull MediaPlayer.OnPreparedListener onPrepared){
		ready = false;
		player.reset();

		RuuDirectory oldDir = null;
		if(this.path != null){
			try{
				oldDir = this.path.getParent();
			}catch(RuuFileBase.CanNotOpen e){
				oldDir = null;
			}
		}

		this.path = path;

		String realName = path.getRealPath();

		if(realName == null){
			Intent sendIntent = new Intent();
			sendIntent.setAction(ACTION_NOT_FOUND);
			sendIntent.putExtra("path", path.getFullPath());
			getBaseContext().sendBroadcast(sendIntent);
			sendStatus();
			errored = true;

			if(!errorSE.isPlaying()){
				errorSE.start();
			}
		}else{
			try{
				player.setDataSource(realName);

				player.setOnPreparedListener(onPrepared);
				player.prepareAsync();
			}catch(IOException e){
				showToast(String.format(getString(R.string.failed_open_music), realName));
			}
		}

		RuuDirectory newparent;
		try{
			newparent = path.getParent();
		}catch(RuuFileBase.CanNotOpen e){
			showToast(String.format(getString(R.string.cant_open_dir), path.path.getParent()));
			return;
		}
		if(recursivePath != null && playlist == null){
			try{
				playlist = recursivePath.getMusicsRecursive();
			}catch(RuuFileBase.CanNotOpen e){
				showToast(String.format(getString(R.string.cant_open_dir), e.path));
				return;
			}

			if(shuffleMode){
				shuffleList();
			}else{
				Collections.sort(playlist);
				currentIndex = Arrays.binarySearch(playlist.toArray(), path);
			}
		}else if(playlist == null || oldDir == null
				|| (recursivePath != null && !recursivePath.contains(newparent))
				|| (recursivePath == null && searchQuery == null && !oldDir.equals(newparent))){
			try{
				playlist = newparent.getMusics();
			}catch(RuuFileBase.CanNotOpen e){
				showToast(String.format(getString(R.string.cant_open_dir), e.path));
				return;
			}
			recursivePath = null;

			if(shuffleMode){
				shuffleList();
			}else{
				Collections.sort(playlist);
				currentIndex = Arrays.binarySearch(playlist.toArray(), path);
			}
		}else{
			currentIndex = findMusicPos(path);
		}
	}

	private int findMusicPos(@NonNull RuuFile music){
		int pos = -1;
		for(int i = 0; i < playlist.size(); i++){
			if(playlist.get(i).equals(music)){
				pos = i;
				break;
			}
		}
		return pos;
	}

	private void shuffleList(){
		if(playlist != null){
			int pos = findMusicPos(path);
			if(pos >= 0){
				Collections.shuffle(playlist);
				Collections.swap(playlist, 0, pos);
				currentIndex = 0;
			}
		}
	}

	private void shufflePlay(){
		if(playlist != null){
			do{
				Collections.shuffle(playlist);
			}while(playlist.get(0).equals(path));

			currentIndex = 0;
			play(playlist.get(0));
		}
	}

	private void pause(){
		player.pause();
		sendStatus();
		removePlayingNotification();
		saveStatus();
		startDeathTimer();
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
			if(!shuffleMode && mode){
				shuffleList();
			}
			if(shuffleMode && !mode){
				Collections.sort(playlist);
				currentIndex = Arrays.binarySearch(playlist.toArray(), path);
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
			if(currentIndex + 1 < playlist.size()){
				play(playlist.get(currentIndex + 1));
			}else if(repeatMode.equals("loop")){
				if(shuffleMode){
					shufflePlay();
				}else{
					play(playlist.get(0));
				}
			}else{
				showToast(getString(R.string.last_of_directory));
				if(!endOfListSE.isPlaying()){
					endOfListSE.start();
				}
			}
		}
	}

	private void prev(){
		if(playlist != null){
			if(player.getCurrentPosition() >= 3000){
				seek(0);
			}else{
				if(currentIndex > 0){
					play(playlist.get(currentIndex - 1));
				}else if(repeatMode.equals("loop")){
					if(shuffleMode){
						shufflePlay();
					}else{
						play(playlist.get(playlist.size() - 1));
					}
				}else{
					showToast(getString(R.string.first_of_directory));
					if(!endOfListSE.isPlaying()){
						endOfListSE.start();
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
				componentName = new ComponentName(context, MediaButtonReceiver.class);
				((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).registerMediaButtonEventReceiver(componentName);
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
				((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).unregisterMediaButtonEventReceiver(componentName);
				componentName = null;
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
