package jp.blanktar.ruumusic.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.IntRange;
import android.support.annotation.UiThread;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


@UiThread
public class MainActivity extends AppCompatActivity{
	public final static String ACTION_OPEN_PLAYER = "jp.blanktar.ruumusic.OPEN_PLAYER";

	private ViewPager viewPager;
	private PlayerFragment player;
	private PlaylistFragment playlist;
	Menu menu;
	SearchView searchView;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		try{
			RuuDirectory.rootDirectory(getApplicationContext());
		}catch(RuuFileBase.NotFound e){
			Preference.Str.ROOT_DIRECTORY.remove(getApplicationContext());
		}

		viewPager = (ViewPager)findViewById(R.id.viewPager);

		viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()){
			@Override
			@NonNull
			public Fragment getItem(@IntRange(from=0, to=1) int position){
				if(position == 0){
					return (player = new PlayerFragment());
				}else{
					return (playlist = new PlaylistFragment());
				}
			}

			@Override
			public int getCount(){
				return 2;
			}
		});

		viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
			@Override
			public void onPageSelected(int position){
				updateTitleAndMenu();
			}
		});

		if(ACTION_OPEN_PLAYER.equals(getIntent().getAction())){
			viewPager.setCurrentItem(0);
		}else if(Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData() != null){
			String path = getIntent().getData().getPath();
			try{
				RuuFile file = RuuFile.getInstance(getApplicationContext(), path.substring(0, path.lastIndexOf(".")));
				file.getRuuPath();
				startService(new Intent(getApplicationContext(), RuuService.class)
								.setAction(RuuService.ACTION_PLAY)
								.putExtra("path", file.getFullPath())
				);
				viewPager.setCurrentItem(0);
			}catch(RuuFileBase.NotFound e){
				Toast.makeText(getApplicationContext(), getString(R.string.music_not_found), Toast.LENGTH_LONG).show();
				viewPager.setCurrentItem(Preference.Int.LAST_VIEW_PAGE.get(getApplicationContext()));
			}catch(RuuFileBase.OutOfRootDirectory e){
				Toast.makeText(getApplicationContext(), String.format(getString(R.string.out_of_root), path), Toast.LENGTH_LONG).show();
				viewPager.setCurrentItem(Preference.Int.LAST_VIEW_PAGE.get(getApplicationContext()));
			}
		}else{
			viewPager.setCurrentItem(Preference.Int.LAST_VIEW_PAGE.get(getApplicationContext()));
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle state){
		super.onSaveInstanceState(state);

		getSupportFragmentManager().putFragment(state, "player_fragment", player);
		getSupportFragmentManager().putFragment(state, "playlist_fragment", playlist);
	}

	@Override
	public void onRestoreInstanceState(@NonNull Bundle state){
		super.onRestoreInstanceState(state);

		player = (PlayerFragment)getSupportFragmentManager().getFragment(state, "player_fragment");
		playlist = (PlaylistFragment)getSupportFragmentManager().getFragment(state, "playlist_fragment");

		updateTitleAndMenu();
	}

	@Override
	public void onResume(){
		super.onResume();

		RuuService.MediaButtonReceiver.onStartActivity(getApplicationContext());
	}

	@Override
	public void onPause(){
		super.onPause();

		RuuService.MediaButtonReceiver.onStopActivity(getApplicationContext());
		Preference.Int.LAST_VIEW_PAGE.set(getApplicationContext(), getCurrentPage());
	}

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu){
		getMenuInflater().inflate(R.menu.menu_main, menu);
		this.menu = menu;

		updateTitleAndMenu();

		MenuItem searchMenu = menu.findItem(R.id.menu_search);
		assert searchMenu != null;
		searchView = (SearchView)MenuItemCompat.getActionView(searchMenu);
		assert searchView != null;
		searchView.setOnQueryTextListener(playlist);
		searchView.setOnCloseListener(playlist);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item){
		int id = item.getItemId();

		if((id == R.id.action_set_root || id == R.id.action_unset_root) && playlist.current != null){
			String rootPath = "/";
			if(id == R.id.action_set_root){
				rootPath = playlist.current.path.getFullPath();
			}
			Preference.Str.ROOT_DIRECTORY.set(getApplicationContext(), rootPath);

			playlist.updateRoot();
			player.updateRoot();

			return true;
		}

		if(id == R.id.action_recursive_play && playlist.current != null){
			startService((new Intent(getApplicationContext(), RuuService.class))
					.setAction(RuuService.ACTION_PLAY_RECURSIVE)
					.putExtra("path", playlist.current.path.getFullPath()));

			moveToPlayer();

			return true;
		}

		if(id == R.id.action_search_play && playlist.current != null){
			startService((new Intent(getApplicationContext(), RuuService.class))
					.setAction(RuuService.ACTION_PLAY_SEARCH)
					.putExtra("path", playlist.current.path.getFullPath())
					.putExtra("query", playlist.searchQuery == null ? null : playlist.searchQuery));

			moveToPlayer();

			return true;
		}

		if(id == R.id.action_audio_preference){
			startActivity(new Intent(getApplicationContext(), AudioPreferenceActivity.class));

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateTitleAndMenu(){
		if(getCurrentPage() == 0){
			setTitle(R.string.app_name);
			if(menu != null){
				menu.findItem(R.id.action_unset_root).setVisible(false);
				menu.findItem(R.id.action_set_root).setVisible(false);
				menu.findItem(R.id.action_recursive_play).setVisible(false);
				menu.findItem(R.id.menu_search).setVisible(false);
				menu.findItem(R.id.action_search_play).setVisible(false);
				menu.findItem(R.id.action_audio_preference).setVisible(true);
			}
		}else if(playlist != null){
			playlist.updateTitle(this);
			playlist.updateMenu(this);
		}
	}

	public void moveToPlayer(){
		viewPager.setCurrentItem(0);
	}

	public void moveToPlaylist(@NonNull RuuDirectory path){
		playlist.changeDir(path);
		viewPager.setCurrentItem(1);
	}

	public void moveToPlaylistSearch(@NonNull RuuDirectory path, @NonNull String query){
		playlist.setSearchQuery(path, query);
		viewPager.setCurrentItem(1);
	}

	public int getCurrentPage(){
		return viewPager.getCurrentItem();
	}

	@Override
	public void onBackPressed(){
	}

    @Override
    public boolean onKeyLongPress(int keyCode, @Nullable KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
			finish();
			return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

	@Override
	public boolean onKeyUp(int keyCode, @NonNull KeyEvent event){
		if(keyCode == KeyEvent.KEYCODE_SEARCH){
			viewPager.setCurrentItem(1);
			searchView.setIconified(false);
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_BACK && (viewPager.getCurrentItem() == 0 || !playlist.onBackKey())){
			finish();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}

