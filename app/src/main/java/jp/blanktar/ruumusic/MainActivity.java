package jp.blanktar.ruumusic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.KeyEvent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.Activity;

import android.util.Log;


public class MainActivity extends AppCompatActivity { 
	private ViewPager viewPager;
	private long lastBackClicked;
	protected Menu menu;
	private PlayerFragment player;
	private PlaylistFragment playlist;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		FragmentManager manager = getSupportFragmentManager();
		RuuPager adapter = new RuuPager(manager);

		viewPager = (ViewPager)findViewById(R.id.viewPager);
		viewPager.setAdapter(adapter);

		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position){
				if(position == 0) {
					MainActivity.this.setTitle(R.string.app_name);
					menu.findItem(R.id.action_unset_root).setVisible(false);
					menu.findItem(R.id.action_set_root).setVisible(false);
				}else {
					if(playlist != null) {
						playlist.updateTitle((Activity) MainActivity.this);
						playlist.updateMenu(MainActivity.this);
					}
				}
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

			@Override
			public void onPageScrollStateChanged(int state) { }
		});
	}

	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		getSupportFragmentManager().putFragment(state, "player_fragment", (Fragment)player);
		getSupportFragmentManager().putFragment(state, "playlist_fragment", (Fragment)playlist);
	}

	@Override
	public void onRestoreInstanceState(Bundle state) {
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
	
	public int getCurrentPage() {
		return viewPager.getCurrentItem();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && viewPager.getCurrentItem() == 1 && playlist.onBackKey()){
			return false;
		}else {
			return super.onKeyDown(keyCode, event);
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
