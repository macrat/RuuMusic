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
import android.media.MediaMetadataRetriever;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.Notification;
import android.support.v4.app.NotificationCompat;
import android.app.NotificationManager;
import android.content.Context;
import android.app.PendingIntent;

import android.util.Log;


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
				showToast(String.format(getString(R.string.failed_open_music), FileTypeUtil.detectRealName(path)));
				return true;
			}
		});
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null) {
			if (intent.getAction().equals("RUU_PLAY")) {
				play(intent.getStringExtra("path"));
			}
			if (intent.getAction().equals("RUU_PAUSE")) {
				pause();
			}
			if (intent.getAction().equals("RUU_SEEK")) {
				seek(intent.getIntExtra("newtime", -1));
			}
			if(intent.getAction().equals("RUU_REPEAT")) {
				setRepeatMode(intent.getStringExtra("mode"));
			}
			if(intent.getAction().equals("RUU_SHUFFLE")) {
				setShuffleMode(intent.getBooleanExtra("mode", false));
			}
			if(intent.getAction().equals("RUU_PING")) {
				sendStatus();
			}
			if(intent.getAction().equals("RUU_NEXT")) {
				next();
			}
			if(intent.getAction().equals("RUU_PREV")) {
				prev();
			}
			if(intent.getAction().equals("RUU_ROOT_CHANGE")) {
				updatePlayingNotification();
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

	private void updatePlayingNotification(){
		if(!player.isPlaying()) {
			return;
		}
		
		File pathfile = new File(path);
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
		String musicPath = pathfile.getParent().substring(preference.getString("root_directory", "/").length()) + "/";
		if (!musicPath.startsWith("/")) {
			musicPath = "/" + musicPath;
		}
		
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new NotificationCompat.Builder(getApplicationContext())
				.setSmallIcon(R.drawable.ic_play_arrow)
				.setTicker(pathfile.getName())
				.setContentTitle(pathfile.getName())
				.setContentText(musicPath)
				.setContentIntent(contentIntent)
				.setOngoing(true)
				.build();
		notificationManager.notify(1, notification);
	}

	private void removePlayingNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(1);
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
		if(this.path == path) {
			player.pause();
			player.seekTo(0);
			play();
			return;
		}
		
		ready = false;
		player.reset();
		this.path = path;
	
		String realName = FileTypeUtil.detectRealName(path);
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
			SharedPreferences.Editor editor = preference.edit();
			editor.putString("repeat_mode", repeatMode);
			editor.apply();
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
		SharedPreferences.Editor editor = preference.edit();
		editor.putBoolean("shuffle_mode", shuffleMode);
		editor.apply();
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
