package jp.blanktar.ruumusic;

import java.util.Timer;
import java.util.TimerTask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.FloatRange;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Handler;
import android.widget.TextView;
import android.widget.SeekBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;


public class PlayerFragment extends Fragment {
	private RuuFile currentMusic;
	private boolean playing;
	private int duration = -1;
	private long basetime = -1;
	private int current = -1;
	private String repeatMode;
	private boolean shuffleMode;
	private Timer updateProgressTimer;
	private boolean firstMessage = true;
	private boolean seeking = false;
	
	@Override
	@NonNull
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_player, container, false);
		
		view.findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(@Nullable View view) {
				startRuuService(playing ? RuuService.ACTION_PAUSE : RuuService.ACTION_PLAY);
			}
		});

		view.findViewById(R.id.nextButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(@Nullable View view) {
				startRuuService(RuuService.ACTION_NEXT);
			}
		});

		view.findViewById(R.id.prevButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(@Nullable View view) {
				startRuuService(RuuService.ACTION_PREV);
			}
		});

		view.findViewById(R.id.repeatButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(@Nullable View view) {
				Intent intent = new Intent(getActivity(), RuuService.class);
				intent.setAction(RuuService.ACTION_REPEAT);
				if (repeatMode != null && repeatMode.equals("loop")) {
					intent.putExtra("mode", "one");
				} else if (repeatMode != null && repeatMode.equals("one")) {
					intent.putExtra("mode", "off");
				} else {
					intent.putExtra("mode", "loop");
				}
				getActivity().startService(intent);
			}
		});

		view.findViewById(R.id.shuffleButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(@Nullable View view) {
				Intent intent = new Intent(getActivity(), RuuService.class);
				intent.setAction(RuuService.ACTION_SHUFFLE);
				intent.putExtra("mode", !shuffleMode);
				getActivity().startService(intent);
			}
		});
		
		view.findViewById(R.id.musicPath).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(@Nullable View view) {
				if(currentMusic != null) {
					MainActivity main = (MainActivity) getActivity();
					if (main != null) {
						RuuDirectory parent = null;
						try {
							parent = currentMusic.getParent();
						}catch(RuuFileBase.CanNotOpen e) {
							Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), currentMusic.path.getParent()), Toast.LENGTH_LONG).show();
						}
						if(parent != null) {
							main.moveToPlaylist(parent);
						}
					}
				}
			}
		});
		
		((SeekBar)view.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private int progress = 0;
			private boolean needResume = false;
	
			@Override
			public void onProgressChanged(@Nullable SeekBar seekBar, int progress, boolean fromUser) {
				if(seeking) {
					this.progress = progress;
					updateProgress(progress);
				}
			}

			@Override
			public void onStartTrackingTouch(@Nullable SeekBar seekBar) {
				seeking = true;
				needResume = playing;
				startRuuService(RuuService.ACTION_PAUSE);
			}

			@Override
			public void onStopTrackingTouch(@Nullable SeekBar seekBar) {
				seeking = false;
	
				Intent intent = new Intent(getActivity(), RuuService.class);
				intent.setAction(RuuService.ACTION_SEEK);
				intent.putExtra("newtime", progress);
				getActivity().startService(intent);

				if(needResume) {
					startRuuService(RuuService.ACTION_PLAY);
				}
			}
		});
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();

		startRuuService(RuuService.ACTION_PING);
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(RuuService.ACTION_STATUS);
		intentFilter.addAction(RuuService.ACTION_FAILED_PLAY);
		intentFilter.addAction(RuuService.ACTION_NOT_FOUND);
		getActivity().registerReceiver(receiver, intentFilter);

		final Handler handler = new Handler();
		updateProgressTimer = new Timer(true);
		updateProgressTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						if(!seeking) {
							updateProgress();
						}
					}
				});
			}
		}, 0, 300);
	}
	
	@Override
	public void onPause() {
		super.onPause();

		getActivity().unregisterReceiver(receiver);
		
		updateProgressTimer.cancel();
	}
	
	final private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(@Nullable Context context, @NonNull Intent intent) {
			switch(intent.getAction()) {
				case RuuService.ACTION_FAILED_PLAY:
					onFailPlay(R.string.failed_play, intent.getStringExtra("path"));
					break;
				case RuuService.ACTION_NOT_FOUND:
					onFailPlay(R.string.music_not_found, intent.getStringExtra("path"));
					break;
				case RuuService.ACTION_STATUS:
					onReceiveStatus(intent);
					break;
			}
		}
	};

	private void onReceiveStatus(@NonNull Intent intent) {
		playing = intent.getBooleanExtra("playing", false);
		duration = intent.getIntExtra("duration", -1);
		basetime = intent.getLongExtra("basetime", -1);
		current = intent.getIntExtra("current", -1);
		repeatMode = intent.getStringExtra("repeat");
		shuffleMode = intent.getBooleanExtra("shuffle", false);

		View view = getView();
		if(view != null) {
			ImageButton playButton = (ImageButton) view.findViewById(R.id.playButton);
			if (playing) {
				playButton.setImageResource(R.drawable.ic_pause);
			} else {
				playButton.setImageResource(R.drawable.ic_play);
			}

			ImageButton repeatButton = (ImageButton) view.findViewById(R.id.repeatButton);
			switch (repeatMode) {
				case "loop":
					repeatButton.setImageResource(R.drawable.ic_repeat_all);
					break;
				case "one":
					repeatButton.setImageResource(R.drawable.ic_repeat_one);
					break;
				default:
					repeatButton.setImageResource(R.drawable.ic_repeat_off);
					break;
			}

			ImageButton shuffleButton = (ImageButton) view.findViewById(R.id.shuffleButton);
			if (shuffleMode) {
				shuffleButton.setImageResource(R.drawable.ic_shuffle_on);
			} else {
				shuffleButton.setImageResource(R.drawable.ic_shuffle_off);
			}

			((SeekBar) view.findViewById(R.id.seekBar)).setMax(duration);
		}

		String path = intent.getStringExtra("path");
		if(path == null) {
			if(firstMessage) {
				((MainActivity) getActivity()).moveToPlaylist();
				firstMessage = false;
			}
			currentMusic = null;
		}else{
			try {
				currentMusic = new RuuFile(getContext(), path);
			}catch(RuuFileBase.CanNotOpen e) {
			}
		}
		updateRoot();
	}

	public void updateRoot() {
		String path = "";
		String name = "";

		if(currentMusic != null) {
			try {
				path = currentMusic.getParent().getRuuPath();
			}catch(RuuFileBase.CanNotOpen | RuuFileBase.OutOfRootDirectory e) {
			}
			name = currentMusic.getName();
		}

		View view = getView();
		if(view != null) {
			((TextView) view.findViewById(R.id.musicPath)).setText(path);
			((TextView) view.findViewById(R.id.musicName)).setText(name);
		}
	}

	@NonNull
	private String msec2str(@FloatRange(from=0) long msec) {
		return ((int)Math.floor(msec/1000/60)) + ":" + String.format("%02d", Math.round(msec/1000)%60);
	}

	private void updateProgress() {
		updateProgress(-1);
	}

	private void updateProgress(int time) {
		if(getView() == null) {
			return;
		}

		TextView text = (TextView)getView().findViewById(R.id.progress);
		SeekBar bar = (SeekBar)getView().findViewById(R.id.seekBar);

		String currentStr = "-";
		if(time >= 0) {
			currentStr = msec2str(time);
			bar.setProgress(time);
		}else if(playing && basetime >= 0) {
			if(duration >= 0) {
				currentStr = msec2str(Math.min(duration, System.currentTimeMillis() - basetime));
			}else {
				currentStr = msec2str(System.currentTimeMillis() - basetime);
			}
			bar.setProgress((int) (System.currentTimeMillis() - basetime));
		}else if(!playing && current >= 0) {
			currentStr = msec2str(current);
			bar.setProgress(current);
		}else {
			bar.setProgress(0);
		}

		String durationStr = "-";
		if(duration >= 0) {
			durationStr = msec2str(duration);
		}

		text.setText(currentStr + " / " + durationStr);
	}

	private void onFailPlay(@StringRes final int messageId, @NonNull final String path) {
		((ImageButton) getActivity().findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play);

		(new AlertDialog.Builder(getActivity()))
				.setTitle(getString(messageId))
				.setMessage(path)
				.setPositiveButton(
						getString(R.string.on_error_next_music),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startRuuService(RuuService.ACTION_NEXT);
							}
						}
				)
				.setNegativeButton(
						getString(R.string.on_error_cancel),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) { }
						}
				)
				.create().show();
	}
	
	private void startRuuService(@NonNull String action) {
		Intent intent = new Intent(getActivity(), RuuService.class);
		intent.setAction(action);
		getActivity().startService(intent);
	}
}