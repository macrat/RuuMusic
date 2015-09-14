package jp.blanktar.ruumusic;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import android.support.annotation.NonNull;
import android.app.Service;
import android.content.Intent;
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


public class RuuService extends Service {
	private RuuFile path;
	private MediaPlayer player;
	private String repeatMode = "off";
	private boolean shuffleMode = false;
	private boolean ready = false;
	private Timer deathTimer;
	
	private List<RuuFile> playlist;
	private int currentIndex;
	private RuuDirectory currentDir;
	
	@Override
	public IBinder onBind(Intent intent) {
		throw null;
	}
	
	@Override
	public void onCreate() {
		player = new MediaPlayer();

		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
		repeatMode = preference.getString("repeat_mode", "off");
		shuffleMode = preference.getBoolean("shuffle_mode", false);

		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (repeatMode.equals("one")) {
					player.pause();
					play();
				} else {
					if (playlist == null || currentIndex + 1 >= playlist.size() && repeatMode.equals("off")) {
						pause();
					} else {
						if (shuffleMode) {
							shufflePlay();
						} else {
							play(playlist.get((currentIndex + 1) % playlist.size()));
						}
					}
				}
			}
		});
		
		player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				player.reset();
				String realName = path.getRealPath();

				Intent sendIntent = new Intent();
				sendIntent.setAction("RUU_FAILED_OPEN");
				sendIntent.putExtra("path", (realName==null ? path.getFullPath() : realName));
				getBaseContext().sendBroadcast(sendIntent);
	
				return true;
			}
		});
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null) {
			switch (intent.getAction()) {
				case "RUU_PLAY":
					play(intent.getStringExtra("path"));
					break;
				case "RUU_PAUSE":
					pause();
					break;
				case "RUU_SEEK":
					seek(intent.getIntExtra("newtime", -1));
					break;
				case "RUU_REPEAT":
					setRepeatMode(intent.getStringExtra("mode"));
					break;
				case "RUU_SHUFFLE":
					setShuffleMode(intent.getBooleanExtra("mode", false));
					break;
				case "RUU_PING":
					sendStatus();
					break;
				case "RUU_NEXT":
					next();
					break;
				case "RUU_PREV":
					prev();
					break;
				case "RUU_ROOT_CHANGE":
					updateRoot();
					break;
			}
		}
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		removePlayingNotification();
	}
	
	private void sendStatus() {
		Intent sendIntent = new Intent();
		
		sendIntent.setAction("RUU_STATUS");
		if(path != null) {
			sendIntent.putExtra("path", path.getFullPath());
		}
		sendIntent.putExtra("repeat", repeatMode);
		sendIntent.putExtra("shuffle", shuffleMode);
		
		if(ready) {
			sendIntent.putExtra("playing", player.isPlaying());
			sendIntent.putExtra("duration", player.getDuration());
			sendIntent.putExtra("current", player.getCurrentPosition());
			sendIntent.putExtra("basetime", System.currentTimeMillis() - player.getCurrentPosition());
		}
		
		getBaseContext().sendBroadcast(sendIntent);
	}
	
	private Notification makeNotification() {
		int playpause_icon = player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow;
		String playpause_text = player.isPlaying() ? "pause" : "play";
		PendingIntent playpause_pi = PendingIntent.getService(this, 0, (new Intent(this, RuuService.class)).setAction(player.isPlaying() ? "RUU_PAUSE" : "RUU_PLAY"), 0);
		
		PendingIntent prev_pi = PendingIntent.getService(this, 0, (new Intent(this, RuuService.class)).setAction("RUU_PREV"), 0);
		PendingIntent next_pi = PendingIntent.getService(this, 0, (new Intent(this, RuuService.class)).setAction("RUU_NEXT"), 0);

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
		String parentPath;
		try {
			parentPath = path.getParent().getFullPath();
		}catch(RuuFileBase.CanNotOpen e) {
			parentPath = path.path.getParent();
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
				.addAction(R.drawable.ic_skip_previous, "prev", prev_pi)
				.addAction(playpause_icon, playpause_text, playpause_pi)
				.addAction(R.drawable.ic_skip_next, "next", next_pi)
				.build();
	}

	private void updatePlayingNotification(){
		if(!player.isPlaying()) {
			return;
		}
		
		startForeground(1, makeNotification());
	}

	private void removePlayingNotification() {
		if(player.isPlaying()) {
			return;
		}

		stopForeground(true);

		if(Build.VERSION.SDK_INT >= 16) {
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, makeNotification());
		}
	}
	
	private void updateRoot() {
		if(player.isPlaying()) {
			updatePlayingNotification();
		}else {
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
		}
	}
	
	private void play() {
		if(path != null) {
			player.start();
			sendStatus();
			updatePlayingNotification();
			stopDeathTimer();
		}
	}
	
	private void play(String path) {
		if(path == null) {
			play();
			return;
		}
		try {
			play(new RuuFile(this, path));
		}catch(RuuFileBase.CanNotOpen e) {
			showToast(String.format(getString(R.string.cant_open_dir), path));
		}
	}
	
	private void play(@NonNull RuuFile path) {
		if(this.path != null && this.path.equals(path)) {
			player.pause();
			player.seekTo(0);
			play();
			return;
		}
		
		ready = false;
		player.reset();
		this.path = path;
		
		String realName = path.getRealPath();

		if(realName == null) {
			Intent sendIntent = new Intent();
			sendIntent.setAction("RUU_NOT_FOUND");
			sendIntent.putExtra("path", path.getFullPath());
			getBaseContext().sendBroadcast(sendIntent);
		}else {
			try {
				player.setDataSource(realName);

				player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mp) {
						ready = true;
						play();
					}
				});
				player.prepareAsync();
			}catch(IOException e) {
				showToast(String.format(getString(R.string.failed_open_music), realName));
			}
		}

		RuuDirectory newparent;
		try {
			newparent = path.getParent();
		}catch(RuuFileBase.CanNotOpen e) {
			showToast(String.format(getString(R.string.cant_open_dir), path.path.getParent()));
			return;
		}
		if(currentDir == null || !currentDir.equals(newparent)) {
			currentDir = newparent;
	
			playlist = currentDir.getMusics();

			if(shuffleMode) {
				shuffleList();
			}else {
				Collections.sort(playlist);
				currentIndex = Arrays.binarySearch(playlist.toArray(), path);
			}
		}else {
			currentIndex = playlist.indexOf(path);
		}
	}
	
	private void shuffleList() {
		if(playlist != null) {
			int pos = -1;
			for(int i=0; i<playlist.size(); i++) {
				if(playlist.get(i).equals(path)) {
					pos = i;
					break;
				}
			}
			if(pos >= 0) {
				Collections.shuffle(playlist);
				Collections.swap(playlist, 0, pos);
				currentIndex = 0;
			}
		}
	}
	
	private void shufflePlay() {
		if(playlist != null) {
			do {
				Collections.shuffle(playlist);
			} while (playlist.get(0).equals(path));

			currentIndex = 0;
			play(playlist.get(0));
		}
	}
	
	private void pause() {
		player.pause();
		sendStatus();
		removePlayingNotification();
		startDeathTimer();
	}
	
	private void seek(int newtime) {
		if(0 <= newtime && newtime <= player.getDuration()) {
			player.seekTo(newtime);
			sendStatus();
		}
	}
	
	private void setRepeatMode(@NonNull String mode) {
		if(mode.equals("off") || mode.equals("loop") || mode.equals("one")) {
			repeatMode = mode;
			sendStatus();
			
			PreferenceManager.getDefaultSharedPreferences(this)
					.edit()
					.putString("repeat_mode", repeatMode)
					.apply();
		}
	}
	
	private void setShuffleMode(boolean mode) {
		if(playlist != null) {
			if (!shuffleMode && mode) {
				shuffleList();
			}
			if (shuffleMode && !mode) {
				Collections.sort(playlist);
				currentIndex = Arrays.binarySearch(playlist.toArray(), path);
			}
		}

		shuffleMode = mode;
		sendStatus();

		PreferenceManager.getDefaultSharedPreferences(this)
				.edit()
				.putBoolean("shuffle_mode", shuffleMode)
				.apply();
	}
	
	private void next() {
		if(playlist != null) {
			if (currentIndex + 1 < playlist.size()) {
				play(playlist.get(currentIndex + 1));
			} else if (repeatMode.equals("loop")) {
				if (shuffleMode) {
					shufflePlay();
				} else {
					play(playlist.get(0));
				}
			} else {
				showToast(getString(R.string.last_of_directory));
			}
		}
	}
	
	private void prev() {
		if(playlist != null) {
			if(player.getCurrentPosition() >= 3000) {
				seek(0);
			}else {
				if (currentIndex > 0) {
					play(playlist.get(currentIndex - 1));
				} else if (repeatMode.equals("loop")) {
					if (shuffleMode) {
						shufflePlay();
					} else {
						play(playlist.get(playlist.size() - 1));
					}
				} else {
					showToast(getString(R.string.first_of_directory));
				}
			}
		}
	}
	
	private void showToast(@NonNull final String message) {
		final Handler handler = new Handler();
		(new Thread(new Runnable() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(RuuService.this, message, Toast.LENGTH_LONG).show();
					}
				});
			}
		})).start();
	}
	
	private void startDeathTimer() {
		stopDeathTimer();

		final Handler handler = new Handler();
		deathTimer = new Timer(true);
		deathTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						RuuService.this.stopSelf();
					}
				});
			}
		}, 5 * 60 * 1000);
	}
	
	private void stopDeathTimer() {
		if(deathTimer != null) {
			deathTimer.cancel();
			deathTimer = null;
		}
	}
}
