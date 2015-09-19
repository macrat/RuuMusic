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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.media.AudioManager;


@UiThread
public class MainActivity extends AppCompatActivity { 
	private ViewPager viewPager;
	Menu menu;
	private PlayerFragment player;
	private PlaylistFragment playlist;
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		try {
			RuuDirectory.rootDirectory(this);
		}catch(RuuFileBase.CanNotOpen e){
			PreferenceManager.getDefaultSharedPreferences(this)
					.edit()
					.putString("root_directory", "/")
					.apply();
		}
		
		viewPager = (ViewPager)findViewById(R.id.viewPager);
		viewPager.setAdapter(new RuuPager());

		viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				updateTitleAndMenu();
			}
		});
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle state) {
		super.onSaveInstanceState(state);
		if(player != null) {
			getSupportFragmentManager().putFragment(state, "player_fragment", player);
		}
		if(playlist != null) {
			getSupportFragmentManager().putFragment(state, "playlist_fragment", playlist);
		}
	}

	@Override
	public void onRestoreInstanceState(@NonNull Bundle state) {
		super.onRestoreInstanceState(state);
		player = (PlayerFragment)getSupportFragmentManager().getFragment(state, "player_fragment");
		playlist = (PlaylistFragment)getSupportFragmentManager().getFragment(state, "playlist_fragment");
		
		updateTitleAndMenu();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		RuuService.MediaButtonReceiver.onStartActivity(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		RuuService.MediaButtonReceiver.onStopActivity(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(@Nullable Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		this.menu = menu;

		updateTitleAndMenu();

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();
		
		if(id == R.id.action_set_root || id == R.id.action_unset_root) {
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
			if (id == R.id.action_set_root) {
				editor.putString("root_directory", playlist.current.path.getFullPath());
			}
			if (id == R.id.action_unset_root) {
				editor.putString("root_directory", "/");
			}
			editor.apply();
			
			playlist.updateRoot();
			player.updateRoot();
			
			Intent intent = new Intent(this, RuuService.class);
			intent.setAction(RuuService.ACTION_ROOT_CHANGED);
			startService(intent);

			return true;
		}

		if(id == R.id.action_recursive_play) {
			Intent intent = new Intent(this, RuuService.class);
			intent.setAction(RuuService.ACTION_PLAY_RECURSIVE);
			intent.putExtra("path", playlist.current.path.getFullPath());
			startService(intent);
			
			moveToPlayer();
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateTitleAndMenu() {
		if (getCurrentPage() == 0) {
			MainActivity.this.setTitle(R.string.app_name);
			if(menu != null) {
				menu.findItem(R.id.action_unset_root).setVisible(false);
				menu.findItem(R.id.action_set_root).setVisible(false);
				menu.findItem(R.id.action_recursive_play).setVisible(false);
			}
		} else if (playlist != null) {
			playlist.updateTitle(MainActivity.this);
			if(menu != null) {
				playlist.updateMenu(MainActivity.this);
				menu.findItem(R.id.action_recursive_play).setVisible(true);
			}
		}
	}
	
	public void moveToPlayer() {
		viewPager.setCurrentItem(0);
	}

	public void moveToPlaylist() {
		viewPager.setCurrentItem(1);
	}
	
	public void moveToPlaylist(@NonNull RuuDirectory path) {
		playlist.changeDir(path);
		moveToPlaylist();
	}
	
	public int getCurrentPage() {
		return viewPager.getCurrentItem();
	}
	
	@Override
	public void onBackPressed() {
		if(viewPager.getCurrentItem() == 0 || !playlist.onBackKey()){
			super.onBackPressed();
		}
	}

	@UiThread
	final private class RuuPager extends FragmentPagerAdapter {
		RuuPager() {
			super(MainActivity.this.getSupportFragmentManager());
		}

		@Override
		@NonNull
		@IntRange(from=0,to=1)
		public Fragment getItem(int position) {
			if(position == 0) {
				return (player = new PlayerFragment());
			}else{
				return (playlist = new PlaylistFragment());
			}
		}

		@Override
		public int getCount() {
			return 2;
		}
	}
}
