package jp.blanktar.ruumusic;

import android.support.annotation.NonNull;
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


public class MainActivity extends AppCompatActivity { 
	private ViewPager viewPager;
	Menu menu;
	private PlayerFragment player;
	private PlaylistFragment playlist;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

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
	public boolean onCreateOptionsMenu(Menu menu) {
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
		
		return super.onOptionsItemSelected(item);
	}

	private void updateTitleAndMenu() {
		if (getCurrentPage() == 0) {
			MainActivity.this.setTitle(R.string.app_name);
			if(menu != null) {
				menu.findItem(R.id.action_unset_root).setVisible(false);
				menu.findItem(R.id.action_set_root).setVisible(false);
			}
		} else if (playlist != null) {
			playlist.updateTitle(MainActivity.this);
			if(menu != null) {
				playlist.updateMenu(MainActivity.this);
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

	final private class RuuPager extends FragmentPagerAdapter {
		RuuPager() {
			super(MainActivity.this.getSupportFragmentManager());
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					return (player = new PlayerFragment());
				case 1:
					return (playlist = new PlaylistFragment());
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}
	}
}
