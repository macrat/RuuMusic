package jp.blanktar.ruumusic;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

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


public class RuuService extends Service {
	private String path;
	private MediaPlayer player;
	private String repeatMode = "off";
	private boolean shuffleMode = false;
	private boolean ready = false;
	
	private List<String> playlist;
	private int currentIndex;
	private File currentDir;
	
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
				String realName = FileTypeUtil.detectRealName(path);
				if(realName != null) {
					showToast(String.format(getString(R.string.failed_open_music), realName));
				}else {
					showToast(String.format(getString(R.string.failed_open_music), path));
				}
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
					updatePlayingNotification();
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
		sendIntent.putExtra("path", path);
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
		File pathfile = new File(path);
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
		String musicPath = pathfile.getParent().substring(preference.getString("root_directory", "/").length()) + "/";
		if (!musicPath.startsWith("/")) {
			musicPath = "/" + musicPath;
		}

		int playpause_icon = player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow;
		String playpause_text = player.isPlaying() ? "pause" : "play";
		PendingIntent playpause_pi = PendingIntent.getService(this, 0, (new Intent(this, RuuService.class)).setAction(player.isPlaying() ? "RUU_PAUSE" : "RUU_PLAY"), 0);
		
		PendingIntent prev_pi = PendingIntent.getService(this, 0, (new Intent(this, RuuService.class)).setAction("RUU_PREV"), 0);
		PendingIntent next_pi = PendingIntent.getService(this, 0, (new Intent(this, RuuService.class)).setAction("RUU_NEXT"), 0);

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
		Notification notification = new NotificationCompat.Builder(getApplicationContext())
				.setSmallIcon(R.drawable.ic_play_notification)
				.setColor(0xff333333)
				.setTicker(pathfile.getName())
				.setContentTitle(pathfile.getName())
				.setContentText(musicPath)
				.setContentIntent(contentIntent)
				.setPriority(Notification.PRIORITY_MIN)
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.addAction(R.drawable.ic_skip_previous, "prev", prev_pi)
				.addAction(playpause_icon, playpause_text, playpause_pi)
				.addAction(R.drawable.ic_skip_next, "next", next_pi)
				.build();
		
		return notification;
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
		
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, makeNotification());
	}
	
	private void play() {
		player.start();
		sendStatus();
		updatePlayingNotification();
	}
	
	private void play(String path) {
		if(path == null) {
			play();
			return;
		}
		if(this.path != null && this.path.equals(path)) {
			player.pause();
			player.seekTo(0);
			play();
			return;
		}
		
		ready = false;
		player.reset();
		this.path = path;
	
		String realName = FileTypeUtil.detectRealName(path);
		if(realName == null) {
			showToast(String.format(getString(R.string.music_not_found), path));
			return;
		}
		try {
			player.setDataSource(realName);
		}catch(IOException e) {
			showToast(String.format(getString(R.string.failed_open_music), realName));
			return;
		}

		File newparent = (new File(path)).getParentFile();
		if(currentDir == null || !currentDir.equals(newparent)) {
			currentDir = newparent;
	
			playlist = FileTypeUtil.getMusics(currentDir);

			if(shuffleMode) {
				shuffleList();
			}else {
				Collections.sort(playlist);
				currentIndex = Arrays.binarySearch(playlist.toArray(), path);
			}
		}else {
			currentIndex = playlist.indexOf(path);
		}
		
		player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				ready = true;
				play();
			}
		});
		player.prepareAsync();
	}
	
	private void shuffleList() {
		if(playlist != null) {
			Collections.shuffle(playlist);
			Collections.swap(playlist, 0, playlist.indexOf(path));
			currentIndex = 0;
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
	}
	
	private void seek(int newtime) {
		if(0 <= newtime && newtime <= player.getDuration()) {
			player.seekTo(newtime);
			sendStatus();
		}
	}
	
	private void setRepeatMode(String mode) {
		if(mode.equals("off") || mode.equals("loop") || mode.equals("one")) {
			repeatMode = mode;
			sendStatus();
			
			SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
			preference.edit()
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

		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
		preference.edit()
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
	
	private void showToast(final String message) {
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
}
