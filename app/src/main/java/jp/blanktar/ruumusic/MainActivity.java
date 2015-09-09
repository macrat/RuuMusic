package jp.blanktar.ruumusic;

import java.io.File;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
		if(!(new File(preference.getString("root_directory", "/"))).isDirectory()) {
			preference.edit()
					.putString("root_directory", "/")
					.apply();
		}
		
		FragmentManager manager = getSupportFragmentManager();
		RuuPager adapter = new RuuPager(manager);

		viewPager = (ViewPager)findViewById(R.id.viewPager);
		viewPager.setAdapter(adapter);

		viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if (position == 0) {
					MainActivity.this.setTitle(R.string.app_name);
					menu.findItem(R.id.action_unset_root).setVisible(false);
					menu.findItem(R.id.action_set_root).setVisible(false);
				} else {
					if (playlist != null) {
						playlist.updateTitle(MainActivity.this);
						playlist.updateMenu(MainActivity.this);
					}
				}
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		getSupportFragmentManager().putFragment(state, "player_fragment", player);
		getSupportFragmentManager().putFragment(state, "playlist_fragment", playlist);
	}

	@Override
	public void onRestoreInstanceState(@NonNull Bundle state) {
		super.onRestoreInstanceState(state);
		player = (PlayerFragment)getSupportFragmentManager().getFragment(state, "player_fragment");
		playlist = (PlaylistFragment)getSupportFragmentManager().getFragment(state, "playlist_fragment");
		
		if(viewPager.getCurrentItem() == 1) {
			playlist.updateTitle(this);
			playlist.updateMenu(this);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		this.menu = menu;

		if(viewPager.getCurrentItem() == 0) {
			menu.findItem(R.id.action_set_root).setVisible(false);
			menu.findItem(R.id.action_unset_root).setVisible(false);
		}else {
			playlist.updateMenu(MainActivity.this);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);

		//noinspection SimplifiableIfStatement
		if(id == R.id.action_set_root || id == R.id.action_unset_root) {
			SharedPreferences.Editor editor = preference.edit();
			if (id == R.id.action_set_root) {
				editor.putString("root_directory", playlist.current.getPath());
			}
			if (id == R.id.action_unset_root) {
				editor.putString("root_directory", "/");
			}
			editor.apply();
			
			playlist.updateRoot();
			player.updateRoot();
			
			Intent intent = new Intent(this, RuuService.class);
			intent.setAction("RUU_ROOT_CHANGE");
			startService(intent);

			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	public void moveToPlayer() {
		viewPager.setCurrentItem(0);
	}

	public void moveToPlaylist() {
		viewPager.setCurrentItem(1);
	}
	
	public void moveToPlaylist(File path) {
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

	public class RuuPager extends FragmentPagerAdapter {
		public RuuPager(FragmentManager fm) {
			super(fm);
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
