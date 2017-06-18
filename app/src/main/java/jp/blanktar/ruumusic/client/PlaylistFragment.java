package jp.blanktar.ruumusic.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


@UiThread
public class PlaylistFragment extends Fragment implements SearchView.OnQueryTextListener, SearchView.OnCloseListener{
	private Preference preference;
	private RuuClient client;

	private RuuAdapter adapter;
	private final Stack<DirectoryInfo> directoryCache = new Stack<>();
	@NonNull private ListStatus status = ListStatus.LOADING;

	@Nullable DirectoryInfo current;
	@Nullable public String searchQuery = null;


	@Override
	@NonNull
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fragment_playlist, container, false);

		preference = new Preference(view.getContext());
		client = new RuuClient(getContext());

		adapter = new RuuAdapter(view.getContext());

		final ListView lv = (ListView)view.findViewById(R.id.playlist);
		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(@NonNull AdapterView<?> parent, @Nullable View view, int position, long id){
				RuuFileBase selected = (RuuFileBase)lv.getItemAtPosition(position);
				if(selected.isDirectory()){
					changeDir((RuuDirectory)selected);
				}else{
					changeMusic((RuuFile)selected);
				}
			}
		});

		registerForContextMenu(lv);

		String currentPath = preference.CurrentViewPath.get();
		try{
			if(currentPath != null){
				changeDir(RuuDirectory.getInstance(getContext(), currentPath));

				searchQuery = preference.LastSearchQuery.get();
				if(searchQuery != null){
					onQueryTextSubmit(searchQuery);
				}
			}else{
				changeDir(RuuDirectory.rootCandidate(getContext()));
			}
		}catch(RuuFileBase.NotFound err){
			try{
				changeDir(RuuDirectory.rootDirectory(getContext()));
			}catch(RuuFileBase.NotFound e){
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), e.path), Toast.LENGTH_LONG).show();
				preference.RootDirectory.remove();
			}
		}

		return view;
	}

	@Override
	public void onDestroy(){
		client.release();
		super.onDestroy();
	}

	@Override
	public void onPause(){
		super.onPause();

		if(current != null){
			preference.CurrentViewPath.set(current.path.getFullPath());
			preference.LastSearchQuery.set(searchQuery);
		}
	}

	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View view, @NonNull ContextMenu.ContextMenuInfo info){
		super.onCreateContextMenu(menu, view, info);
		RuuFileBase file = adapter.getItem(((AdapterView.AdapterContextMenuInfo)info).position);
		boolean openable = getActivity().getPackageManager().queryIntentActivities(file.toIntent(), 0).size() > 0;

		if(file.isDirectory()){
			menu.setHeaderTitle(file.getName() + "/");
			getActivity().getMenuInflater().inflate(R.menu.directory_context_menu, menu);
			menu.findItem(R.id.action_open_dir_with_other_app).setVisible(openable);
		}else{
			menu.setHeaderTitle(file.getName());
			getActivity().getMenuInflater().inflate(R.menu.music_context_menu, menu);
			menu.findItem(R.id.action_open_music_with_other_app).setVisible(openable);
		}
	}

	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item){
		RuuFileBase file = adapter.getItem(((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position);

		switch(item.getItemId()){
			case R.id.action_open_directory:
				changeDir((RuuDirectory)file);
				return true;
			case R.id.action_open_music:
				changeMusic((RuuFile)file);
				return true;
			case R.id.action_open_dir_with_other_app:
			case R.id.action_open_music_with_other_app:
				startActivity(file.toIntent());
				return true;
			case R.id.action_web_search_dir:
			case R.id.action_web_search_music:
				startActivity((new Intent(Intent.ACTION_WEB_SEARCH)).putExtra(SearchManager.QUERY, file.getName()));
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	public void updateTitle(@NonNull Activity activity){
		if(current == null){
			activity.setTitle("");
		}else{
			try{
				activity.setTitle(current.path.getRuuPath());
			}catch(RuuFileBase.OutOfRootDirectory e){
				activity.setTitle("");
				Toast.makeText(getActivity(), String.format(getString(R.string.out_of_root), current.path.getFullPath()), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void updateTitle(){
		updateTitle(getActivity());
	}

	public void updateRoot(){
		updateTitle();
		updateMenu();
		if(current != null){
			changeDir(current.path);
		}
	}

	private void updateStatus(@NonNull ListStatus status){
		this.status = status;
		try{
			switch(status){
				case LOADING:
					((TextView)getActivity().findViewById(R.id.playlist_message)).setText(R.string.loading_list);
					break;
				case EMPTY:
					((TextView)getActivity().findViewById(R.id.playlist_message)).setText(R.string.empty_list);
					break;
				case SHOWN:
					getActivity().findViewById(R.id.playlist).setVisibility(View.VISIBLE);
					getActivity().findViewById(R.id.playlist_message).setVisibility(View.GONE);
					break;
			}
		}catch(NullPointerException e){
			return;
		}

		if(status != ListStatus.SHOWN){
			final Handler handler = new Handler();
			(new Handler()).postDelayed(new Runnable(){
				@Override
				public void run(){
					if(PlaylistFragment.this.status != ListStatus.SHOWN){
						handler.post(new Runnable(){
							@Override
							public void run(){
								getActivity().findViewById(R.id.playlist_message).setVisibility(View.VISIBLE);
								getActivity().findViewById(R.id.playlist).setVisibility(View.GONE);
							}
						});
					}
				}
			}, 100);
		}
	}

	public void updateStatus(){
		updateStatus(status);
	}

	void changeDir(@NonNull RuuDirectory dir){
		try{
			dir.getRuuPath();
		}catch(RuuFileBase.OutOfRootDirectory e){
			Toast.makeText(getActivity(), String.format(getString(R.string.out_of_root), dir.getFullPath()), Toast.LENGTH_LONG).show();
			return;
		}

		MainActivity main = (MainActivity)getActivity();
		if(main.searchView != null && !main.searchView.isIconified()){
			main.searchView.setQuery("", false);
			main.searchView.setIconified(true);
		}

		while(!directoryCache.empty() && !directoryCache.peek().path.contains(dir)){
			directoryCache.pop();
		}

		if(!directoryCache.empty() && directoryCache.peek().path.equals(dir)){
			current = directoryCache.pop();
		}else{
			if(current != null){
				current.selection = ((ListView)getActivity().findViewById(R.id.playlist)).getFirstVisiblePosition();
				directoryCache.push(current);
			}

			current = new DirectoryInfo(dir);
		}

		if(((MainActivity)getActivity()).getCurrentPage() == MainActivity.Page.PLAYLIST){
			updateTitle();
		}

		adapter.setRuuFiles(current);
	}

	public void updateMenu(@NonNull MainActivity activity){
		if(activity.getCurrentPage() != MainActivity.Page.PLAYLIST){
			return;
		}

		Menu menu = activity.menu;
		if(menu != null){
			RuuDirectory rootDirectory;
			try{
				rootDirectory = RuuDirectory.rootDirectory(getContext());
			}catch(RuuFileBase.NotFound e){
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), "root directory"), Toast.LENGTH_LONG).show();
				return;
			}

			menu.findItem(R.id.action_set_root).setVisible(current != null && !rootDirectory.equals(current.path) && searchQuery == null);
			menu.findItem(R.id.action_unset_root).setVisible(!rootDirectory.getFullPath().equals("/") && searchQuery == null);
			menu.findItem(R.id.action_search_play).setVisible(searchQuery != null);
			menu.findItem(R.id.action_search_play).setEnabled(adapter.hasMusic);
			menu.findItem(R.id.action_recursive_play).setVisible(searchQuery == null && adapter.getCount() > 0);
			menu.findItem(R.id.menu_search).setVisible(true);
		}
	}

	private void updateMenu(){
		updateMenu((MainActivity)getActivity());
	}

	private void changeMusic(@NonNull RuuFile file){
		client.play(file);
		((MainActivity)getActivity()).moveToPlayer();
	}

	public boolean onBackKey(){
		SearchView search = ((MainActivity)getActivity()).searchView;
		if(search != null && !search.isIconified()){
			search.setQuery("", false);
			search.setIconified(true);
			onClose();
			return true;
		}

		RuuDirectory root;
		try{
			root = RuuDirectory.rootDirectory(getContext());
		}catch(RuuFileBase.NotFound e){
			return false;
		}

		if(current == null){
			return false;
		}else if(current.path.equals(root)){
			return false;
		}else{
			try{
				changeDir(current.path.getParent());
			}catch(RuuFileBase.OutOfRootDirectory e){
				return false;
			}
			return true;
		}
	}

	public void setSearchQuery(@NonNull RuuDirectory path, @NonNull String query){
		changeDir(path);
		((MainActivity)getActivity()).searchView.setIconified(false);
		((MainActivity)getActivity()).searchView.setQuery(query, true);
	}

	@Override
	public boolean onQueryTextChange(@NonNull String text){
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(@NonNull final String text){
		if(current == null){
			return false;
		}
		if(TextUtils.isEmpty(text)){
			onClose();
			return false;
		}

		SearchView sv = ((MainActivity)getActivity()).searchView;
		if(sv != null){
			sv.clearFocus();
		}

		updateStatus(ListStatus.LOADING);
		
		final Handler handler = new Handler();
		(new Thread(new Runnable(){
			@Override
			public void run(){
				final String[] queries = TextUtils.split(text.toLowerCase(), " \t");

				final ArrayList<RuuFileBase> filtered = new ArrayList<>();
				for(RuuFileBase file: current.path.getAllRecursive()){
					String name = file.getName().toLowerCase();
					boolean isOk = true;
					for(String query: queries){
						if(!name.contains(query)){
							isOk = false;
							break;
						}
					}
					if(isOk){
						filtered.add(file);
					}
				}

				handler.post(new Runnable(){
					@Override
					public void run(){
						searchQuery = text;
						adapter.setSearchResults(filtered);
					}
				});
			}
		})).start();

		return false;
	}

	@Override
	public boolean onClose(){
		adapter.resumeFromSearch();

		searchQuery = null;
		preference.LastSearchQuery.remove();
		updateMenu();

		return false;
	}


	enum ListStatus{
		LOADING,
		EMPTY,
		SHOWN
	}


	class DirectoryInfo{
		public final RuuDirectory path;
		public int selection = 0;

		public DirectoryInfo(@NonNull RuuDirectory path){
			this.path = path;
		}
	}


	@UiThread
	private class RuuAdapter extends ArrayAdapter<RuuFileBase>{
		@Nullable private DirectoryInfo dirInfo;
		boolean hasMusic = false;

		RuuAdapter(@NonNull Context context){
			super(context, R.layout.list_item);
		}

		void setRuuFiles(@NonNull final DirectoryInfo dirInfo){
			searchQuery = null;
			this.dirInfo = dirInfo;
	
			updateStatus(ListStatus.LOADING);

			final Handler handler = new Handler();
			(new Thread(new Runnable(){
				@Override
				public void run(){
					handler.post(new Runnable(){
						@Override
						public void run(){
							clear();

							try{
								RuuDirectory rootDirectory = RuuDirectory.rootDirectory(getContext());
								if(!rootDirectory.equals(dirInfo.path) && rootDirectory.contains(dirInfo.path)){
									add(dirInfo.path.getParent());
								}
							}catch(RuuFileBase.NotFound | RuuFileBase.OutOfRootDirectory e){
							}

							for(RuuDirectory dir: dirInfo.path.getDirectories()){
								add(dir);
							}

							hasMusic = false;
							for(RuuFile music: dirInfo.path.getMusics()){
								add(music);
								hasMusic = true;
							}

							ListView listView = (ListView)getActivity().findViewById(R.id.playlist);
							if(listView != null){
								listView.setSelection(dirInfo.selection);
							}

							updateStatus(ListStatus.SHOWN);
							updateMenu();
						}
					});
				}
			})).start();
		}

		void setSearchResults(@NonNull final List<RuuFileBase> results){
			updateStatus(ListStatus.LOADING);

			if(dirInfo != null && getActivity() != null && getActivity().findViewById(R.id.playlist) != null){
				dirInfo.selection = ((ListView)getActivity().findViewById(R.id.playlist)).getFirstVisiblePosition();
			}

			clear();

			if(results.size() == 0){
				updateStatus(ListStatus.EMPTY);
				return;
			}

			hasMusic = false;
			for(RuuFileBase result: results){
				add(result);

				if(!result.isDirectory()){
					hasMusic = true;
				}
			}

			ListView listView = (ListView)getActivity().findViewById(R.id.playlist);
			if(listView != null){
				listView.setSelection(0);
			}

			updateStatus(ListStatus.SHOWN);
			updateMenu();
		}

		void resumeFromSearch(){
			if(dirInfo != null){
				clear();
				setRuuFiles(dirInfo);
			}
		}

		@Override
		public int getViewTypeCount(){
			return 3;
		}

		@Override
		@IntRange(from=0, to=2)
		public int getItemViewType(int position){
			if(searchQuery != null){
				return 2;
			}

			if(getItem(position).isDirectory() && dirInfo != null && ((RuuDirectory)getItem(position)).contains(dirInfo.path)){
				return 1;
			}else{
				return 0;
			}
		}

		@Override
		@NonNull
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
			RuuFileBase item = getItem(position);

			if(searchQuery != null){
				if(convertView == null){
					convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_item_search, null);
				}

				((TextView)convertView.findViewById(R.id.search_name)).setText(item.getName() + (item.isDirectory() ? "/" : ""));
				try{
					((TextView)convertView.findViewById(R.id.search_path)).setText(item.getParent().getRuuPath());
				}catch(RuuFileBase.OutOfRootDirectory e){
					((TextView)convertView.findViewById(R.id.search_path)).setText("");
				}
			}else{
				assert dirInfo != null;
				if(item.isDirectory() && ((RuuDirectory)item).contains(dirInfo.path)){
					if(convertView == null){
						convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_item_upper, null);
					}
				}else{
					if(convertView == null){
						convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_item, null);
					}

					((TextView)convertView).setText(item.getName() + (item.isDirectory() ? "/" : ""));
				}
			}

			return convertView;
		}
	}
}
