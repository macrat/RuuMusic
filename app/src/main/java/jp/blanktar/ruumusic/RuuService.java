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

		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if(repeatMode.equals("one")) {
					player.pause();
					play();
				}else {
					if(playlist == null || currentIndex + 1 >= playlist.size() && repeatMode.equals("off")) {
						pause();
					}else {
						if(shuffleMode) {
							shufflePlay();
						}else {
							play(playlist.get((currentIndex + 1)%playlist.size()));
						}
					}
				}
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
		}
		return START_NOT_STICKY;
	}
	
	private void sendStatus() {
		Intent sendIntent = new Intent();
		
		sendIntent.setAction("RUU_STATUS");
		sendIntent.putExtra("path", path);
		sendIntent.putExtra("repeat", repeatMode);
		sendIntent.putExtra("shuffle", shuffleMode);
		
		if(ready) {
			/***
			 * Cause SIGABRT if can't decode medatata.

			MediaMetadataRetriever metadata = new MediaMetadataRetriever();
			metadata.setDataSource(FileTypeUtil.detectRealName(path));

			String artist = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
			if(artist == null) {
				artist = "";
			}
			sendIntent.putExtra("artist", artist);

			 *
			 ***/
			
			sendIntent.putExtra("playing", player.isPlaying());
			sendIntent.putExtra("duration", player.getDuration());
			sendIntent.putExtra("current", player.getCurrentPosition());
			sendIntent.putExtra("basetime", System.currentTimeMillis() - player.getCurrentPosition());
		}
		
		getBaseContext().sendBroadcast(sendIntent);
	}
	
	private void play() {
		player.start();
		sendStatus();
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
			showToast("failed loading music: " + realName);
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
				showToast("here is last of directory");
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
				showToast("here is first of directory");
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
