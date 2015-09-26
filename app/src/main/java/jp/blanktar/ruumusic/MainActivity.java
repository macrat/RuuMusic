package jp.blanktar.ruumusic;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.IntRange;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.support.v7.widget.SearchView;
import android.support.v4.view.MenuItemCompat;


@UiThread
public class MainActivity extends AppCompatActivity{
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

		if("jp.blanktar.ruumusic.OPEN_PLAYER".equals(getIntent().getAction())){
			viewPager.setCurrentItem(0);
		}else{
			viewPager.setCurrentItem(PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
					.getInt("last_view_page", 1));
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
		searchView.setOnQueryTextListener(playlist);
		searchView.setOnCloseListener(playlist);

		return true;
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

			startService((new Intent(getApplicationContext(), RuuService.class))
					.setAction(RuuService.ACTION_ROOT_CHANGED));

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
					.putExtra("query", "" + searchView.getQuery()));

			moveToPlayer();
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
			}
		}else if(playlist != null){
			playlist.updateTitle(this);
			if(menu != null){
				playlist.updateMenu(this);
				menu.findItem(R.id.action_recursive_play).setVisible(true);
				menu.findItem(R.id.menu_search).setVisible(true);
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