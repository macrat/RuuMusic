package jp.blanktar.ruumusic;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.content.DialogInterface;


public class PlayerFragment extends Fragment {
	private File currentMusicPath;
	private boolean playing;
	private int duration = -1;
	private long basetime = -1;
	private int current = -1;
	private String repeatMode;
	private boolean shuffleMode;
	private Timer updateProgressTimer;
	private boolean firstMessage = true;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_player, container, false);
		
		view.findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getActivity(), RuuService.class);
				if (playing) {
					intent.setAction("RUU_PAUSE");
				} else {
					intent.setAction("RUU_PLAY");
				}
				getActivity().startService(intent);
			}
		});

		view.findViewById(R.id.nextButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getActivity(), RuuService.class);
				intent.setAction("RUU_NEXT");
				getActivity().startService(intent);
			}
		});

		view.findViewById(R.id.prevButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getActivity(), RuuService.class);
				intent.setAction("RUU_PREV");
				getActivity().startService(intent);
			}
		});

		view.findViewById(R.id.repeatButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getActivity(), RuuService.class);
				intent.setAction("RUU_REPEAT");
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
			public void onClick(View view) {
				Intent intent = new Intent(getActivity(), RuuService.class);
				intent.setAction("RUU_SHUFFLE");
				intent.putExtra("mode", !shuffleMode);
				getActivity().startService(intent);
			}
		});
		
		view.findViewById(R.id.musicPath).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(currentMusicPath != null) {
					MainActivity main = (MainActivity) getActivity();
					if (main != null) {
						main.moveToPlaylist(currentMusicPath.getParentFile());
					}
				}
			}
		});
		
		((SeekBar)view.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					Intent intent = new Intent(getActivity(), RuuService.class);
					intent.setAction("RUU_SEEK");
					intent.putExtra("newtime", progress);
					getActivity().startService(intent);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();

		Intent intent = new Intent(getActivity(), RuuService.class);
		intent.setAction("RUU_PING");
		getActivity().startService(intent);
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("RUU_STATUS");
		intentFilter.addAction("RUU_FAILED_OPEN");
		intentFilter.addAction("RUU_NOT_FOUND");
		getActivity().registerReceiver(receiver, intentFilter);

		final Handler handler = new Handler();
		updateProgressTimer = new Timer(true);
		updateProgressTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						updateProgress();
					}
				});
			}
		}, 500, 500);
	}
	
	@Override
	public void onPause() {
		super.onPause();

		getActivity().unregisterReceiver(receiver);
		
		updateProgressTimer.cancel();
	}
	
	final private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("RUU_FAILED_OPEN")) {
				onFailPlay(R.string.failed_open_music, intent.getStringExtra("path"));
			}
			if(intent.getAction().equals("RUU_NOT_FOUND")) {
				onFailPlay(R.string.music_not_found, intent.getStringExtra("path"));
			}
			if(intent.getAction().equals("RUU_STATUS")) {
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
						playButton.setImageResource(R.drawable.ic_play_arrow);
					}

					ImageButton repeatButton = (ImageButton) view.findViewById(R.id.repeatButton);
					switch (repeatMode) {
						case "loop":
							repeatButton.setImageResource(R.drawable.ic_repeat);
							break;
						case "one":
							repeatButton.setImageResource(R.drawable.ic_repeat_one);
							break;
						default:
							repeatButton.setImageResource(R.drawable.ic_trending_flat);
							break;
					}

					ImageButton shuffleButton = (ImageButton) view.findViewById(R.id.shuffleButton);
					if (shuffleMode) {
						shuffleButton.setImageResource(R.drawable.ic_shuffle);
					} else {
						shuffleButton.setImageResource(R.drawable.ic_reorder);
					}

					((SeekBar) view.findViewById(R.id.seekBar)).setMax(duration);
				}

				String path = intent.getStringExtra("path");
				if(path == null) {
					if(firstMessage) {
						((MainActivity) getActivity()).moveToPlaylist();
						firstMessage = false;
					}
				}else{
					currentMusicPath = new File(path);
					updateRoot();
				}
			}
		}
	};
	
	public void updateRoot() {
		if(currentMusicPath != null) {
			SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
			String path = currentMusicPath.getParent().substring(preference.getString("root_directory", "").length()) + "/";
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			
			View view = getView();
			if(view != null) {
				((TextView) view.findViewById(R.id.musicPath)).setText(path);
				((TextView) view.findViewById(R.id.musicName)).setText(currentMusicPath.getName());
			}
		}
	}
	
	private String msec2str(long msec) {
		return ((int)Math.floor(msec/1000/60)) + ":" + String.format("%02d", Math.round(msec/1000)%60);
	}
	
	private void updateProgress() {
		if(getView() == null) {
			return;
		}
		
		TextView text = (TextView)getView().findViewById(R.id.progress);
		SeekBar bar = (SeekBar)getView().findViewById(R.id.seekBar);
		
		String currentStr = "-";
		if(playing && basetime >= 0) {
			currentStr = msec2str(System.currentTimeMillis() - basetime);
			bar.setProgress((int)(System.currentTimeMillis() - basetime));
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

	private void onFailPlay(final int messageId, final String path) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
		alertDialog.setTitle(getString(messageId));
		alertDialog.setMessage(path);
		alertDialog.setPositiveButton(
				getString(R.string.on_error_next_music),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int wich) {
						Intent intent = new Intent(getActivity(), RuuService.class);
						intent.setAction("RUU_NEXT");
						getActivity().startService(intent);
					}
				}
		);
		alertDialog.setNegativeButton(
				getString(R.string.on_error_cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int wich) {
					}
				}
		);
		alertDialog.create().show();
	}
}
