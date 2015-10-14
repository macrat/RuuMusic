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
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;
import jp.blanktar.ruumusic.util.RuuDirectory;
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
		}catch(RuuFileBase.CanNotOpen e){
			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
					.putString("root_directory", "/")
					.apply();
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
		}else{
			viewPager.setCurrentItem(PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
					.getInt("last_view_page", 1));
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle state){
		super.onSaveInstanceState(state);

		if(player != null){
			getSupportFragmentManager().putFragment(state, "player_fragment", player);
		}
		if(playlist != null){
			getSupportFragmentManager().putFragment(state, "playlist_fragment", playlist);
		}
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

		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
				.putInt("last_view_page", getCurrentPage())
				.apply();
	}

	@Override
	public boolean onCreateOptionsMenu(@Nullable Menu menu){
		getMenuInflater().inflate(R.menu.menu_main, menu);
		this.menu = menu;

		updateTitleAndMenu();

		searchView = (SearchView)MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));
		if(searchView != null){
			searchView.setOnQueryTextListener(playlist);
			searchView.setOnCloseListener(playlist);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item){
		int id = item.getItemId();

		if(id == R.id.action_set_root || id == R.id.action_unset_root){
			String rootPath = "/";
			if(id == R.id.action_set_root){
				rootPath = playlist.current.path.getFullPath();
			}
			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
					.putString("root_directory", rootPath)
					.apply();

			playlist.updateRoot();
			player.updateRoot();

			return true;
		}

		if(id == R.id.action_recursive_play){
			startService((new Intent(getApplicationContext(), RuuService.class))
					.setAction(RuuService.ACTION_PLAY_RECURSIVE)
					.putExtra("path", playlist.current.path.getFullPath()));

			moveToPlayer();
		}

		if(id == R.id.action_search_play){
			startService((new Intent(getApplicationContext(), RuuService.class))
					.setAction(RuuService.ACTION_PLAY_SEARCH)
					.putExtra("path", playlist.current.path.getFullPath())
					.putExtra("query", playlist.searchQuery == null ? null : playlist.searchQuery));

			moveToPlayer();
		}

		if(id == R.id.action_audio_preference){
			startActivity(new Intent(getApplicationContext(), AudioPreferenceActivity.class));
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
			if(menu != null){
				playlist.updateMenu(this);
			}
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
		if(viewPager.getCurrentItem() == 0 || !playlist.onBackKey()){
			super.onBackPressed();
		}
	}

    @Override
    public boolean onKeyLongPress(int keyCode, @Nullable KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            super.onBackPressed();
			return false;
        }
        return super.onKeyLongPress(keyCode, event);
    }

	@Override
	public boolean onKeyUp(int keyCode, @NonNull KeyEvent event){
		if(keyCode == KeyEvent.KEYCODE_SEARCH){
			viewPager.setCurrentItem(1);
			searchView.setIconified(false);
		}
		return super.onKeyUp(keyCode, event);
	}
}