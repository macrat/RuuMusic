package jp.blanktar.ruumusic.client.main;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.IntRange;
import android.support.annotation.UiThread;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.client.preference.PreferenceActivity;
import jp.blanktar.ruumusic.util.DynamicShortcuts;
import jp.blanktar.ruumusic.util.PermissionManager;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.util.RuuClient;
import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


@UiThread
public class MainActivity extends PermissionManager.Activity {
	public final static String ACTION_OPEN_PLAYER = "jp.blanktar.ruumusic.OPEN_PLAYER";
	public final static String ACTION_OPEN_PLAYLIST = "jp.blanktar.ruumusic.OPEN_PLAYLIST";
	public final static String ACTION_START_PLAY = "jp.blanktar.ruumusic.START_PLAY_WITH_ACTIVITY";

	private Preference preference;
	private RuuClient client;

	private ViewPager viewPager;
	private PlayerFragment player;
	private PlaylistFragment playlist;
	Menu menu;
	SearchView searchView;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		preference = new Preference(getApplicationContext());
		client = new RuuClient(getApplicationContext());

		preference.RootDirectory.setOnChangeListener(new Preference.OnChangeListener(){
			@Override
			public void onChange(){
				player.updateRoot();
				playlist.updateRoot();
			}
		});

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		viewPager = (ViewPager)findViewById(R.id.viewPager);

		viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()){
			@Override
			@NonNull
			public Fragment getItem(@IntRange(from=0, to=1) int position){
				if(position == 0){
					return (player = new PlayerFragment());
				}else{
					playlist = new PlaylistFragment();
					playlist.permissionManager = getPermissionManager();
					return playlist;
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

		if(getPermissionManager().getHasPermission()){
			checkRootDirectory();
		}else{
			getPermissionManager().setOnResultListener(new PermissionManager.OnResultListener(){
				@Override
				public void onGranted(){
					checkRootDirectory();
					if(playlist != null){
						playlist.onPermissionGranted();
					}
				}

				@Override
				public void onDenied(){
					(new AlertDialog.Builder(MainActivity.this))
							.setTitle(getString(R.string.permission_denied_title))
							.setMessage(getString(R.string.permission_denied_message))
							.setNegativeButton(
									getString(R.string.permission_denied_close),
									new DialogInterface.OnClickListener(){
										@Override
										public void onClick(DialogInterface dialog, int which){
											finish();
										}
									}
							)
							.setPositiveButton(
									getString(R.string.permission_denied_ok),
									new DialogInterface.OnClickListener(){
										@Override
										public void onClick(DialogInterface dialog, int which){
											getPermissionManager().request();
										}
									}
							)
							.create().show();
				}
			});
			getPermissionManager().request();
		}

		onNewIntent(getIntent());
	}

	private void checkRootDirectory(){
		try{
			RuuDirectory.rootDirectory(getApplicationContext());
		}catch(RuuFileBase.NotFound e){
			preference.RootDirectory.remove();
		}

		try{
			RuuDirectory.getInstance(getApplicationContext(), "/");
		}catch(RuuFileBase.NotFound e){
			(new AlertDialog.Builder(this))
					.setTitle(getString(R.string.empty_device_title))
					.setMessage(getString(R.string.empty_device_message))
					.setPositiveButton(
							getString(R.string.empty_device_button),
							new DialogInterface.OnClickListener(){
								@Override
								public void onClick(DialogInterface dialog, int which){
									finish();
								}
							}
					)
					.create().show();
		}
	}

	@Override
	public void onNewIntent(@NonNull Intent intent) {
		super.onNewIntent(intent);

		new DynamicShortcuts(getApplicationContext()).reportShortcutUsed(intent.getStringExtra(DynamicShortcuts.EXTRA_SHORTCUT_ID));

		switch(intent.getAction()){
			case ACTION_OPEN_PLAYER:
				moveToPlayer();
				break;
			case ACTION_OPEN_PLAYLIST:
				if(intent.getData() != null){
					String path = intent.getData().getPath();
					try{
						RuuDirectory dir = RuuDirectory.getInstance(getApplicationContext(), path);
						dir.getRuuPath();
						if(playlist != null){
							moveToPlaylist(dir);
						}else{
							preference.CurrentViewPath.set(dir.getFullPath());
							moveToPlaylist();
						}
						return;
					}catch(RuuFileBase.NotFound e){
						Toast.makeText(getApplicationContext(), getString(R.string.cant_open_dir), Toast.LENGTH_LONG).show();
						viewPager.setCurrentItem(preference.LastViewPage.get());
					}catch(RuuFileBase.OutOfRootDirectory e){
						Toast.makeText(getApplicationContext(), getString(R.string.out_of_root, path), Toast.LENGTH_LONG).show();
						viewPager.setCurrentItem(preference.LastViewPage.get());
					}
				}
				moveToPlaylist();
				break;
			case Intent.ACTION_SEARCH:
				moveToPlaylist();
				preference.CurrentViewPath.set(preference.RootDirectory.get());
				preference.LastSearchQuery.set(intent.getStringExtra(SearchManager.QUERY));
				break;
			case ACTION_START_PLAY:
			case Intent.ACTION_VIEW:
				if(intent.getData() != null){
					String path = intent.getData().getPath();
					try{
						RuuFile file = RuuFile.getInstance(getApplicationContext(), path.substring(0, path.lastIndexOf(".")));
						file.getRuuPath();
						client.play(file);
						moveToPlayer();
					}catch(RuuFileBase.NotFound e){
						Toast.makeText(getApplicationContext(), getString(R.string.music_not_found), Toast.LENGTH_LONG).show();
						viewPager.setCurrentItem(preference.LastViewPage.get());
					}catch(RuuFileBase.OutOfRootDirectory e){
						Toast.makeText(getApplicationContext(), getString(R.string.out_of_root, path), Toast.LENGTH_LONG).show();
						viewPager.setCurrentItem(preference.LastViewPage.get());
					}
					break;
				}
			case MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH:
				moveToPlayer();
				try{
					client.playSearch(RuuDirectory.getInstance(getApplicationContext(), preference.RootDirectory.get()), intent.getStringExtra(SearchManager.QUERY));
				}catch(RuuFileBase.NotFound err){
					try{
						client.playSearch(RuuDirectory.rootDirectory(getApplicationContext()), intent.getStringExtra(SearchManager.QUERY));
					}catch(RuuFileBase.NotFound e){
						Toast.makeText(getApplicationContext(), getString(R.string.cant_open_dir, "/"), Toast.LENGTH_LONG).show();
					}
				}
				break;
			default:
				viewPager.setCurrentItem(preference.LastViewPage.get());
		}
	}

	@Override
	public void onDestroy(){
		client.release();
		super.onDestroy();
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
	public void onPause(){
		super.onPause();

		preference.LastViewPage.set(getCurrentPage().ordinal());
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus){
		playlist.updateStatus();
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

		String query = preference.LastSearchQuery.get();
		if(query != null){
			searchView.setIconified(false);
			searchView.setQuery(query, true);
			menu.findItem(R.id.menu_search).setVisible(viewPager.getCurrentItem() == 1);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item){
		int id = item.getItemId();

		if(id == R.id.action_recursive_play && playlist.current != null){
			client.playRecursive(playlist.current.path);

			moveToPlayer();

			return true;
		}

		if(id == R.id.action_search_play && playlist.current != null){
			client.playSearch(playlist.current.path, playlist.searchQuery);

			moveToPlayer();

			return true;
		}

		if(id == R.id.action_preference){
			startActivity(new Intent(getApplicationContext(), PreferenceActivity.class));

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateTitleAndMenu(){
		Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);

		if(getCurrentPage() == Page.PLAYER){
			toolbar.setTitle(R.string.app_name);
			toolbar.setSubtitle("");
			if(menu != null){
				menu.findItem(R.id.action_recursive_play).setVisible(false);
				menu.findItem(R.id.menu_search).setVisible(false);
				menu.findItem(R.id.action_search_play).setVisible(false);
			}
		}else if(playlist != null){
			playlist.updateTitle(toolbar);
			playlist.updateMenu(this);
		}
	}

	public void moveToPlayer(){
		viewPager.setCurrentItem(0);
	}

	private void moveToPlaylist(){
		viewPager.setCurrentItem(1);
	}

	public void moveToPlaylist(@NonNull RuuDirectory path){
		playlist.changeDir(path);
		moveToPlaylist();
	}

	public void moveToPlaylistSearch(@NonNull RuuDirectory path, @NonNull String query){
		playlist.setSearchQuery(path, query);
		moveToPlaylist();
	}

	public Page getCurrentPage(){
		return Page.values()[viewPager.getCurrentItem()];
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
			moveToPlaylist();
			searchView.setIconified(false);
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_BACK && (event.isCanceled() || getCurrentPage() != Page.PLAYLIST || !playlist.onBackKey())){
			super.onBackPressed();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}


	enum Page{
		PLAYER,
		PLAYLIST
	}
}

