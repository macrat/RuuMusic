package jp.blanktar.ruumusic.client.main;

import java.util.Timer;
import java.util.TimerTask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
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
import jp.blanktar.ruumusic.util.PlayingStatus;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.util.RuuClient;
import jp.blanktar.ruumusic.util.RuuFileBase;
import jp.blanktar.ruumusic.view.ShrinkTextView;


@UiThread
public class PlayerFragment extends Fragment{
	private RuuClient client;

	private boolean seeking = false;

	@Nullable private Timer updateProgressTimer;


	@Override
	@NonNull
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fragment_player, container, false);

		client = new RuuClient(getContext());

		view.findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				if (client.status.playing) {
					client.pause();
				} else {
					client.play();
				}
			}
		});

		view.findViewById(R.id.nextButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				client.next();
			}
		});

		view.findViewById(R.id.prevButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				client.prev();
			}
		});

		view.findViewById(R.id.repeatButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				client.repeat(client.status.repeatMode.getNext());
			}
		});

		view.findViewById(R.id.shuffleButton).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				client.shuffle(!client.status.shuffleMode);
			}
		});

		registerForContextMenu(view.findViewById(R.id.musicPath));
		view.findViewById(R.id.musicPath).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				if(client.status.currentMusic != null){
					MainActivity main = (MainActivity)getActivity();
					if(main != null){
						main.moveToPlaylist(client.status.currentMusic.getParent());
					}
				}
			}
		});

		registerForContextMenu(view.findViewById(R.id.musicName));

		registerForContextMenu(view.findViewById(R.id.status_indicator));
		view.findViewById(R.id.status_indicator).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(@Nullable View view){
				if(client.status.recursivePath != null){
					MainActivity main = (MainActivity)getActivity();
					if(main != null){
						main.moveToPlaylist(client.status.recursivePath);
					}
				}
				if(client.status.searchPath != null && client.status.searchQuery != null){
					MainActivity main = (MainActivity)getActivity();
					if(main != null){
						main.moveToPlaylistSearch(client.status.searchPath, client.status.searchQuery);
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
				needResume = client.status.playing;
				client.pauseTransient();
			}

			@Override
			public void onStopTrackingTouch(@Nullable SeekBar seekBar){
				seeking = false;

				client.seek(progress);

				if(needResume){
					client.play();
				}
			}
		});

		client.setEventListener(new RuuClient.EventListener(){
			@Override
			public void onFailedPlay(@NonNull String path) {
				noticeFailedPlay(R.string.failed_play, path);
			}

			@Override
			public void onMusicNotFound(@NonNull String path) {
				noticeFailedPlay(R.string.music_not_found, path);
			}

			@Override
			public void onUpdatedStatus(@NonNull PlayingStatus status) {
				View view = getView();
				if(view != null){
					((ImageButton)view.findViewById(R.id.playButton))
							.setImageResource(client.status.playing ? R.drawable.ic_pause : R.drawable.ic_play);

					ImageButton repeatButton = (ImageButton)view.findViewById(R.id.repeatButton);
					switch(client.status.repeatMode){
						case LOOP:
							repeatButton.setImageResource(R.drawable.ic_repeat_all);
							break;
						case SINGLE:
							repeatButton.setImageResource(R.drawable.ic_repeat_one);
							break;
						case OFF:
							repeatButton.setImageResource(R.drawable.ic_repeat_off);
							break;
					}

					((ImageButton)view.findViewById(R.id.shuffleButton))
							.setImageResource(client.status.shuffleMode ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);

					((SeekBar)view.findViewById(R.id.seekBar)).setMax((int)client.status.duration);
				}

				updateRoot();
			}
		});

		return view;
	}

	@Override
	public void onDestroy(){
		client.release();
		super.onDestroy();
	}

	@Override
	public void onResume(){
		super.onResume();

		Preference pref = new Preference(getContext());

		ShrinkTextView musicPath = (ShrinkTextView)getView().findViewById(R.id.musicPath);
		float pathSize = pref.PlayerMusicPathSize.get().floatValue();

		musicPath.setMaxTextSize(pathSize);
		musicPath.setMinTextSize(pathSize / 2);
		musicPath.setResizingEnabled(pref.PlayerAutoShrinkEnabled.get());

		ShrinkTextView musicName = (ShrinkTextView)getView().findViewById(R.id.musicName);
		float nameSize = pref.PlayerMusicNameSize.get().floatValue();

		musicName.setMaxTextSize(nameSize);
		musicName.setMinTextSize(nameSize / 2);
		musicName.setResizingEnabled(pref.PlayerAutoShrinkEnabled.get());

		client.ping();

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

		if(updateProgressTimer != null){
			updateProgressTimer.cancel();
		}
	}

	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View view, @NonNull ContextMenu.ContextMenuInfo info){
		super.onCreateContextMenu(menu, view, info);

		if(client.status.currentMusic == null){
			return;
		}

		switch(view.getId()){
			case R.id.musicPath:
				menu.setHeaderTitle(client.status.currentMusic.getParent().getName() + "/");
				getActivity().getMenuInflater().inflate(R.menu.directory_context_menu, menu);
				menu.findItem(R.id.action_open_dir_with_other_app).setVisible(
					getActivity().getPackageManager().queryIntentActivities(client.status.currentMusic.getParent().toIntent(), 0).size() > 0
				);
				break;
			case R.id.musicName:
				menu.setHeaderTitle(client.status.currentMusic.getName());
				getActivity().getMenuInflater().inflate(R.menu.music_context_menu, menu);
				menu.findItem(R.id.action_open_music).setVisible(false);
				menu.findItem(R.id.action_open_music_with_other_app).setVisible(
					getActivity().getPackageManager().queryIntentActivities(client.status.currentMusic.toIntent(), 0).size() > 0
				);
				break;
			case R.id.status_indicator:
				if(client.status.recursivePath != null){
					menu.setHeaderTitle(client.status.recursivePath.getName() + "/");
					getActivity().getMenuInflater().inflate(R.menu.indicator_recursive_context_menu, menu);
					menu.findItem(R.id.action_open_recursive_with_other_app).setVisible(
						getActivity().getPackageManager().queryIntentActivities(client.status.recursivePath.toIntent(), 0).size() > 0
					);
				}else if(client.status.searchQuery != null){
					menu.setHeaderTitle(client.status.searchQuery);
					getActivity().getMenuInflater().inflate(R.menu.indicator_search_context_menu, menu);
				}
				break;
		}
	}

	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item){
		switch(item.getItemId()){
			case R.id.action_open_directory:
				assert client.status.currentMusic != null;
				((MainActivity)getActivity()).moveToPlaylist(client.status.currentMusic.getParent());
				return true;
			case R.id.action_open_recursive:
				assert client.status.recursivePath != null;
				((MainActivity)getActivity()).moveToPlaylist(client.status.recursivePath);
				return true;
			case R.id.action_open_search:
				assert client.status.searchPath != null && client.status.searchQuery != null;
				((MainActivity)getActivity()).moveToPlaylistSearch(client.status.searchPath, client.status.searchQuery);
				return true;
			case R.id.action_open_dir_with_other_app:
				assert client.status.currentMusic != null;
				startActivity(client.status.currentMusic.getParent().toIntent());
				return true;
			case R.id.action_open_music_with_other_app:
				assert client.status.currentMusic != null;
				startActivity(client.status.currentMusic.toIntent());
				return true;
			case R.id.action_open_recursive_with_other_app:
				assert client.status.recursivePath != null;
				startActivity(client.status.recursivePath.toIntent());
				return true;
			case R.id.action_web_search_dir:
				assert client.status.currentMusic != null;
				startActivity((new Intent(Intent.ACTION_WEB_SEARCH)).putExtra(SearchManager.QUERY, client.status.currentMusic.getParent().getName()));
				return true;
			case R.id.action_web_search_music:
				assert client.status.currentMusic != null;
				startActivity((new Intent(Intent.ACTION_WEB_SEARCH)).putExtra(SearchManager.QUERY, client.status.currentMusic.getName()));
				return true;
			case R.id.action_web_search_recursive:
				assert client.status.recursivePath != null;
				startActivity((new Intent(Intent.ACTION_WEB_SEARCH)).putExtra(SearchManager.QUERY, client.status.recursivePath.getName()));
				return true;
			case R.id.action_web_search_search:
				assert client.status.searchQuery != null;
				startActivity((new Intent(Intent.ACTION_WEB_SEARCH)).putExtra(SearchManager.QUERY, client.status.searchQuery));
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	void updateRoot(){
		String path = "";
		String name = "";

		if(client.status.currentMusic != null){
			try{
				path = client.status.currentMusic.getParent().getRuuPath();
			}catch(RuuFileBase.OutOfRootDirectory e){
				path = "";
			}
			name = client.status.currentMusic.getName();
		}

		View view = getView();
		if(view != null){
			((ShrinkTextView)view.findViewById(R.id.musicPath)).setText(path);
			((ShrinkTextView)view.findViewById(R.id.musicName)).setText(name);

			if(client.status.recursivePath != null){
				try{
					((TextView)view.findViewById(R.id.status_indicator)).setText(getString(R.string.recursive, client.status.recursivePath.getRuuPath()));
				}catch(RuuFileBase.OutOfRootDirectory e){
					((TextView)view.findViewById(R.id.status_indicator)).setText("");
				}
			}else if(client.status.searchQuery != null && !client.status.searchQuery.equals("")){
				((TextView)view.findViewById(R.id.status_indicator)).setText(getString(R.string.search_play, client.status.searchQuery));
			}else{
				((TextView)view.findViewById(R.id.status_indicator)).setText("");
			}
		}
	}

	@NonNull
	private String msec2str(long msec){
		int sec = Math.round(msec / 1000);
		return ((int)Math.floor(sec / 60)) + ":" + String.format("%02d", sec % 60);
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

		String currentStr;
		if(time >= 0){
			currentStr = msec2str(time);
			bar.setProgress(time);
		}else{
			currentStr = client.status.getCurrentTimeStr();
			bar.setProgress((int)(client.status.getCurrentTime()));
		}

		text.setText(currentStr + " / " + client.status.getDurationStr());
	}

	private void noticeFailedPlay(@StringRes final int messageId, @NonNull final String path){
		((ImageButton)getActivity().findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play);

		(new AlertDialog.Builder(getActivity()))
				.setTitle(getString(messageId))
				.setMessage(path)
				.setPositiveButton(
						getString(R.string.on_error_next_music),
						new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which){
								client.next();
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
}
