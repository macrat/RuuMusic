package jp.blanktar.ruumusic.client;

import java.util.Timer;
import java.util.TimerTask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.FloatRange;
import android.support.annotation.UiThread;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;
import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


@UiThread
public class PlayerFragment extends Fragment{
	@Nullable private RuuFile currentMusic;
	@Nullable private String searchQuery = null;
	@Nullable private RuuDirectory searchPath = null;
	@Nullable private RuuDirectory recursivePath = null;

	private int duration = -1;
	private long basetime = -1;
	private int current = -1;

	private boolean playing = false;
	@NonNull private String repeatMode = "off";
	private boolean shuffleMode = false;
	private boolean seeking = false;

	@Nullable private Timer updateProgressTimer;


	@Override
	@NonNull
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fragment_player, container, false);

		view.findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				startRuuService(playing ? RuuService.ACTION_PAUSE : RuuService.ACTION_PLAY);
			}
		});

		view.findViewById(R.id.nextButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				startRuuService(RuuService.ACTION_NEXT);
			}
		});

		view.findViewById(R.id.prevButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				startRuuService(RuuService.ACTION_PREV);
			}
		});

		view.findViewById(R.id.repeatButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				Intent intent = new Intent(getActivity(), RuuService.class);
				intent.setAction(RuuService.ACTION_REPEAT);

				switch(repeatMode){
					case "loop":
						intent.putExtra("mode", "one");
						break;
					case "one":
						intent.putExtra("mode", "off");
						break;
					default:
						intent.putExtra("mode", "loop");
						break;
				}
	
				getActivity().startService(intent);
			}
		});

		view.findViewById(R.id.shuffleButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				getActivity().startService((new Intent(getActivity(), RuuService.class))
						.setAction(RuuService.ACTION_SHUFFLE)
						.putExtra("mode", !shuffleMode));
			}
		});

		registerForContextMenu(view.findViewById(R.id.musicPath));
		view.findViewById(R.id.musicPath).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				if(currentMusic != null){
					MainActivity main = (MainActivity)getActivity();
					if(main != null){
						main.moveToPlaylist(currentMusic.getParent());
					}
				}
			}
		});

		registerForContextMenu(view.findViewById(R.id.musicName));

		view.findViewById(R.id.status_indicator).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				if(recursivePath != null){
					MainActivity main = (MainActivity)getActivity();
					if(main != null){
						main.moveToPlaylist(recursivePath);
					}
				}
				if(searchPath != null && searchQuery != null){
					MainActivity main = (MainActivity)getActivity();
					if(main != null){
						main.moveToPlaylistSearch(searchPath, searchQuery);
					}
				}
			}
		});

		((SeekBar)view.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			private int progress = 0;
			private boolean needResume = false;

			@Override
			public void onProgressChanged(@Nullable SeekBar seekBar, int progress, boolean fromUser){
				if(seeking){
					this.progress = progress;
					updateProgress(progress);
				}
			}

			@Override
			public void onStartTrackingTouch(@Nullable SeekBar seekBar){
				seeking = true;
				needResume = playing;
				startRuuService(RuuService.ACTION_PAUSE_TRANSIENT);
			}

			@Override
			public void onStopTrackingTouch(@Nullable SeekBar seekBar){
				seeking = false;

				getActivity().startService((new Intent(getActivity(), RuuService.class))
						.setAction(RuuService.ACTION_SEEK)
						.putExtra("newtime", progress));

				if(needResume){
					startRuuService(RuuService.ACTION_PLAY);
				}
			}
		});

		return view;
	}

	@Override
	public void onResume(){
		super.onResume();

		startRuuService(RuuService.ACTION_PING);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(RuuService.ACTION_STATUS);
		intentFilter.addAction(RuuService.ACTION_FAILED_PLAY);
		intentFilter.addAction(RuuService.ACTION_NOT_FOUND);
		getActivity().registerReceiver(receiver, intentFilter);

		final Handler handler = new Handler();
		updateProgressTimer = new Timer(true);
		updateProgressTimer.schedule(new TimerTask(){
			@Override
			public void run(){
				handler.post(new Runnable(){
					@Override
					public void run(){
						if(!seeking){
							updateProgress();
						}
					}
				});
			}
		}, 0, 300);
	}

	@Override
	public void onPause(){
		super.onPause();

		getActivity().unregisterReceiver(receiver);

		if(updateProgressTimer != null){
			updateProgressTimer.cancel();
		}
	}

	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View view, @NonNull ContextMenu.ContextMenuInfo info){
		super.onCreateContextMenu(menu, view, info);


		switch(view.getId()){
			case R.id.musicPath:
				menu.setHeaderTitle(currentMusic.getParent().getName() + "/");
				getActivity().getMenuInflater().inflate(R.menu.directory_context_menu, menu);
				menu.findItem(R.id.action_open_dir_with_other_app).setVisible(
					getActivity().getPackageManager().queryIntentActivities(currentMusic.getParent().toIntent(), 0).size() > 0
				);
				break;
			case R.id.musicName:
				menu.setHeaderTitle(currentMusic.getName());
				getActivity().getMenuInflater().inflate(R.menu.music_context_menu, menu);
				menu.findItem(R.id.action_open_music).setVisible(false);
				menu.findItem(R.id.action_open_music_with_other_app).setVisible(
					getActivity().getPackageManager().queryIntentActivities(currentMusic.toIntent(), 0).size() > 0
				);
				break;
		}
	}

	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item){
		switch(item.getItemId()){
			case R.id.action_open_directory:
				((MainActivity)getActivity()).moveToPlaylist(currentMusic.getParent());
				return true;
			case R.id.action_open_dir_with_other_app:
				startActivity(currentMusic.getParent().toIntent());
				return true;
			case R.id.action_open_music_with_other_app:
				startActivity(currentMusic.toIntent());
				return true;
			case R.id.action_web_search_dir:
				startActivity((new Intent(Intent.ACTION_WEB_SEARCH)).putExtra(SearchManager.QUERY, currentMusic.getParent().getName()));
				return true;
			case R.id.action_web_search_music:
				startActivity((new Intent(Intent.ACTION_WEB_SEARCH)).putExtra(SearchManager.QUERY, currentMusic.getName()));
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	final private BroadcastReceiver receiver = new BroadcastReceiver(){
		@Override
		public void onReceive(@Nullable Context context, @NonNull Intent intent){
			switch(intent.getAction()){
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

	private void onReceiveStatus(@NonNull Intent intent){
		playing = intent.getBooleanExtra("playing", false);
		duration = intent.getIntExtra("duration", -1);
		basetime = intent.getLongExtra("basetime", -1);
		current = intent.getIntExtra("current", -1);
		repeatMode = intent.getStringExtra("repeat");
		shuffleMode = intent.getBooleanExtra("shuffle", false);

		searchQuery = intent.getStringExtra("searchQuery");

		try{
			searchPath = RuuDirectory.getInstance(getContext(), intent.getStringExtra("searchPath"));
		}catch(RuuFileBase.NotFound | NullPointerException e){
			searchPath = null;
		}

		try{
			recursivePath = RuuDirectory.getInstance(getContext(), intent.getStringExtra("recursivePath"));
		}catch(RuuFileBase.NotFound | NullPointerException e){
			recursivePath = null;
		}

		View view = getView();
		if(view != null){
			((ImageButton)view.findViewById(R.id.playButton))
					.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);

			ImageButton repeatButton = (ImageButton)view.findViewById(R.id.repeatButton);
			switch(repeatMode){
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

			((ImageButton)view.findViewById(R.id.shuffleButton))
					.setImageResource(shuffleMode ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);

			((SeekBar)view.findViewById(R.id.seekBar)).setMax(duration);
		}

		if(intent.getStringExtra("path") != null){
			try{
				currentMusic = RuuFile.getInstance(getContext(), intent.getStringExtra("path"));
			}catch(RuuFileBase.NotFound e){
				currentMusic = null;
			}
		}
		updateRoot();
	}

	void updateRoot(){
		String path = "";
		String name = "";

		if(currentMusic != null){
			try{
				path = currentMusic.getParent().getRuuPath();
			}catch(RuuFileBase.OutOfRootDirectory e){
				path = "";
			}
			name = currentMusic.getName();
		}

		View view = getView();
		if(view != null){
			((TextView)view.findViewById(R.id.musicPath)).setText(path);
			((TextView)view.findViewById(R.id.musicName)).setText(name);

			if(recursivePath != null){
				try{
					((TextView)view.findViewById(R.id.status_indicator)).setText(String.format(getString(R.string.recursive), recursivePath.getRuuPath()));
				}catch(RuuFileBase.OutOfRootDirectory e){
					((TextView)view.findViewById(R.id.status_indicator)).setText("");
				}
			}else if(searchQuery != null && !searchQuery.equals("")){
				((TextView)view.findViewById(R.id.status_indicator)).setText(String.format(getString(R.string.search_play), searchQuery));
			}else{
				((TextView)view.findViewById(R.id.status_indicator)).setText("");
			}
		}
	}

	@NonNull
	private String msec2str(@FloatRange(from = 0) long msec){
		return ((int)Math.floor(msec / 1000 / 60)) + ":" + String.format("%02d", Math.round(msec / 1000) % 60);
	}

	private void updateProgress(){
		updateProgress(-1);
	}

	private void updateProgress(int time){
		if(getView() == null){
			return;
		}

		TextView text = (TextView)getView().findViewById(R.id.progress);
		SeekBar bar = (SeekBar)getView().findViewById(R.id.seekBar);

		String currentStr = "-";
		if(time >= 0){
			currentStr = msec2str(time);
			bar.setProgress(time);
		}else if(playing && basetime >= 0){
			if(duration >= 0){
				currentStr = msec2str(Math.min(duration, System.currentTimeMillis() - basetime));
			}else{
				currentStr = msec2str(System.currentTimeMillis() - basetime);
			}
			bar.setProgress((int)(System.currentTimeMillis() - basetime));
		}else if(!playing && current >= 0){
			currentStr = msec2str(current);
			bar.setProgress(current);
		}else{
			bar.setProgress(0);
		}

		String durationStr = "-";
		if(duration >= 0){
			durationStr = msec2str(duration);
		}

		text.setText(currentStr + " / " + durationStr);
	}

	private void onFailPlay(@StringRes final int messageId, @NonNull final String path){
		((ImageButton)getActivity().findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play);

		(new AlertDialog.Builder(getActivity()))
				.setTitle(getString(messageId))
				.setMessage(path)
				.setPositiveButton(
						getString(R.string.on_error_next_music),
						new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which){
								startRuuService(RuuService.ACTION_NEXT);
							}
						}
				)
				.setNegativeButton(
						getString(R.string.on_error_cancel),
						new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which){ }
						}
				)
				.create().show();
	}

	private void startRuuService(@NonNull String action){
		Intent intent = new Intent(getActivity(), RuuService.class);
		intent.setAction(action);
		getActivity().startService(intent);
	}
}
