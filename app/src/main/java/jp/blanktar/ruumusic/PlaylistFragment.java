package jp.blanktar.ruumusic;

import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.AdapterView;
import android.content.Intent;
import android.app.Activity;
import android.view.Menu;
import android.preference.PreferenceManager;
import android.content.Context;
import android.widget.TextView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;


@UiThread
public class PlaylistFragment extends Fragment implements SearchView.OnQueryTextListener, SearchView.OnCloseListener{
	private RuuAdapter adapter;
	private final Stack<DirectoryInfo> directoryCache = new Stack<>();
	DirectoryInfo current;
	private List<RuuFile> searchCache;
	private RuuDirectory searchPath;
	public String searchQuery = null;


	@Override
	@NonNull
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fragment_playlist, container, false);

		adapter = new RuuAdapter(view.getContext());

		final ListView lv = (ListView)view.findViewById(R.id.playlist);
		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(@NonNull AdapterView<?> parent, @Nullable View view, int position, long id){
				RuuListItem selected = (RuuListItem)lv.getItemAtPosition(position);
				if(selected.file.isDirectory()){
					changeDir((RuuDirectory)selected.file);
				}else{
					changeMusic((RuuFile)selected.file);
				}
			}
		});

		String currentPath = PreferenceManager.getDefaultSharedPreferences(getContext())
				.getString("current_view_path", null);
		try{
			if(currentPath == null){
				changeDir(RuuDirectory.rootDirectory(getContext()));
			}else{
				try{
					changeDir(RuuDirectory.getInstance(getContext(), currentPath));
				}catch(RuuFileBase.CanNotOpen e){
					changeDir(RuuDirectory.rootDirectory(getContext()));
				}
			}
		}catch(RuuFileBase.CanNotOpen err){
			PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
					.putString("root_directory", "/")
					.apply();
		}

		return view;
	}

	@Override
	public void onPause(){
		super.onPause();

		if(current != null){
			PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
					.putString("current_view_path", current.path.getFullPath())
					.apply();
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
		changeDir(current.path);
	}

	void changeDir(@NonNull RuuDirectory dir){
		RuuDirectory rootDirectory;
		try{
			rootDirectory = RuuDirectory.rootDirectory(getContext());
		}catch(RuuFileBase.CanNotOpen e){
			Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), "root directory"), Toast.LENGTH_LONG).show();
			return;
		}

		if(!rootDirectory.contains(dir)){
			Toast.makeText(getActivity(), String.format(getString(R.string.out_of_root), dir.getFullPath()), Toast.LENGTH_LONG).show();
			return;
		}

		if(((MainActivity)getActivity()).searchView != null){
			((MainActivity)getActivity()).searchView.setQuery("", false);
			((MainActivity)getActivity()).searchView.setIconified(true);
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

		if(((MainActivity)getActivity()).getCurrentPage() == 1){
			updateTitle();
		}

		try{
			adapter.setRuuFiles(current);
		}catch(RuuFileBase.CanNotOpen e){
			Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), e.path), Toast.LENGTH_LONG).show();
			return;
		}

		ListView lv = (ListView)getActivity().findViewById(R.id.playlist);
		if(lv != null){
			lv.setSelection(current.selection);
		}

		updateMenu();
	}

	public void updateMenu(@NonNull MainActivity activity){
		Menu menu = activity.menu;
		if(menu != null){
			RuuDirectory rootDirectory;
			try{
				rootDirectory = RuuDirectory.rootDirectory(getContext());
			}catch(RuuFileBase.CanNotOpen e){
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), "root directory"), Toast.LENGTH_LONG).show();
				return;
			}

			menu.findItem(R.id.action_set_root).setVisible(current != null && !rootDirectory.equals(current.path) && searchQuery == null);
			menu.findItem(R.id.action_unset_root).setVisible(!rootDirectory.getFullPath().equals("/") && searchQuery == null);
			menu.findItem(R.id.action_search_play).setVisible(searchQuery != null && adapter.getCount() > 0);
			menu.findItem(R.id.action_recursive_play).setVisible(searchQuery == null);
			menu.findItem(R.id.menu_search).setVisible(true);
		}
	}

	private void updateMenu(){
		updateMenu((MainActivity)getActivity());
	}

	private void changeMusic(@NonNull RuuFile file){
		Intent intent = new Intent(getActivity(), RuuService.class);
		intent.setAction(RuuService.ACTION_PLAY);
		intent.putExtra("path", file.getFullPath());
		getActivity().startService(intent);

		((MainActivity)getActivity()).moveToPlayer();
	}

	public boolean onBackKey(){
		SearchView search = ((MainActivity)getActivity()).searchView;
		if(!search.isIconified()){
			search.setQuery("", false);
			search.setIconified(true);
			onClose();
			return true;
		}

		RuuDirectory root;
		try{
			root = RuuDirectory.rootDirectory(getContext());
		}catch(RuuFileBase.CanNotOpen e){
			return false;
		}

		if(current.path.equals(root)){
			return false;
		}else{
			try{
				changeDir(current.path.getParent());
			}catch(RuuDirectory.CanNotOpen e){
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), current.path.path.getParent()), Toast.LENGTH_LONG).show();
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
	public boolean onQueryTextChange(String text){
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String text){
		if(TextUtils.isEmpty(text)){
			onClose();
			return false;
		}
		
		((MainActivity)getActivity()).searchView.clearFocus();

		String[] queries = TextUtils.split(text.toLowerCase(), " \t");

		if(searchPath == null || searchCache == null || !searchPath.equals(current.path)){
			try{
				searchCache = current.path.getMusicsRecursive();
			}catch(RuuFileBase.CanNotOpen e){
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), e.path), Toast.LENGTH_LONG).show();
				return false;
			}
			searchPath = current.path;
		}

		ArrayList<RuuFile> filtered = new ArrayList<>();
		for(RuuFile music: searchCache){
			String name = music.getName().toLowerCase();
			boolean isOk = true;
			for(String query: queries){
				if(!name.contains(query)){
					isOk = false;
					break;
				}
			}
			if(isOk){
				filtered.add(music);
			}
		}

		adapter.setSearchResults(filtered);
		searchQuery = text;

		updateMenu();

		return false;
	}

	@Override
	public boolean onClose(){
		try{
			adapter.resumeFromSearch();
		}catch(RuuFileBase.CanNotOpen e){
			try{
				changeDir(RuuDirectory.rootDirectory(getContext()));
			}catch(RuuFileBase.CanNotOpen e2){
				adapter.clear();
			}
		}

		updateMenu();

		return false;
	}


	class DirectoryInfo{
		public final RuuDirectory path;
		public int selection = 0;

		public DirectoryInfo(@NonNull RuuDirectory path){
			this.path = path;
		}
	}


	@UiThread
	public class RuuListItem{
		public final RuuFileBase file;
		public final String text;
		public final boolean isUpperDir;

		public RuuListItem(@NonNull RuuFileBase file, @Nullable String text, boolean isUpperDir){
			this.file = file;
			this.text = text;
			this.isUpperDir = isUpperDir;
		}

		public RuuListItem(@NonNull RuuFile file){
			this(file, file.getName(), false);
		}

		public RuuListItem(@NonNull RuuDirectory dir){
			this(dir, dir.getName() + "/", false);
		}
	}


	@UiThread
	class RuuAdapter extends ArrayAdapter<RuuListItem>{
		private DirectoryInfo dirInfo;

		public RuuAdapter(@NonNull Context context){
			super(context, R.layout.list_item);
		}

		public void setRuuFiles(@NonNull DirectoryInfo dirInfo) throws RuuFileBase.CanNotOpen{
			clear();
			searchQuery = null;
			this.dirInfo = dirInfo;

			RuuDirectory rootDirectory = RuuDirectory.rootDirectory(getContext());
			if(!rootDirectory.equals(dirInfo.path) && rootDirectory.contains(dirInfo.path)){
				add(new RuuListItem(dirInfo.path.getParent(), null, true));
			}

			for(RuuDirectory dir: dirInfo.path.getDirectories()){
				add(new RuuListItem(dir));
			}

			for(RuuFile file: dirInfo.path.getMusics()){
				add(new RuuListItem(file));
			}
		}

		public void setSearchResults(@NonNull List<RuuFile> results){
			clear();

			for(RuuFile file: results){
				add(new RuuListItem(file));
			}
		}

		public void resumeFromSearch() throws RuuFileBase.CanNotOpen{
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
		public int getItemViewType(int position){
			if(searchQuery != null){
				return 2;
			}
	
			RuuListItem item = getItem(position);
			if(item.isUpperDir){
				return 1;
			}else{
				return 0;
			}
		}

		@Override
		@NonNull
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
			RuuListItem item = getItem(position);

			if(searchQuery != null){
				if(convertView == null){
					convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_item_search, null);
				}

				((TextView)convertView.findViewById(R.id.search_name)).setText(item.text);
				try{
					((TextView)convertView.findViewById(R.id.search_path)).setText(item.file.getParent().getRuuPath());
				}catch(RuuFileBase.CanNotOpen | RuuFileBase.OutOfRootDirectory e){
					((TextView)convertView.findViewById(R.id.search_path)).setText("");
				}
			}else{
				if(item.isUpperDir){
					if(convertView == null){
						convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_item_upper, null);
					}
				}else{
					if(convertView == null){
						convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_item, null);
					}

					((TextView)convertView).setText(item.text);
				}
			}

			return convertView;
		}
	}
}
