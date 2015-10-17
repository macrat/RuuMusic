package jp.blanktar.ruumusic.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;
import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;


@UiThread
public class PlaylistFragment extends Fragment implements SearchView.OnQueryTextListener, SearchView.OnCloseListener{
	private RuuAdapter adapter;
	private final Stack<DirectoryInfo> directoryCache = new Stack<>();
	@Nullable DirectoryInfo current;
	@Nullable public String searchQuery = null;


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
				RuuFileBase selected = (RuuFileBase)lv.getItemAtPosition(position);
				if(selected.isDirectory()){
					changeDir((RuuDirectory)selected);
				}else{
					changeMusic((RuuFile)selected);
				}
			}
		});

		String currentPath = PreferenceManager.getDefaultSharedPreferences(getContext())
				.getString("current_view_path", null);
		try{
			if(currentPath != null){
				changeDir(RuuDirectory.getInstance(getContext(), currentPath));
			}else{
				changeDir(RuuDirectory.getInstance(getContext(), Environment.getExternalStorageDirectory().getPath()));
			}
		}catch(RuuFileBase.CanNotOpen err){
			try{
				changeDir(RuuDirectory.rootDirectory(getContext()));
			}catch(RuuFileBase.CanNotOpen e){
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), e.path), Toast.LENGTH_LONG).show();
				PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
						.remove("root_directory")
						.apply();
			}
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
		if(current != null){
			changeDir(current.path);
		}
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
			menu.findItem(R.id.action_search_play).setVisible(searchQuery != null && adapter.musicNum > 0);
			menu.findItem(R.id.action_recursive_play).setVisible(searchQuery == null && adapter.musicNum + adapter.directoryNum > 0);
			menu.findItem(R.id.menu_search).setVisible(true);
			menu.findItem(R.id.action_audio_preference).setVisible(false);
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
		if(search != null && !search.isIconified()){
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

		if(current == null){
			return false;
		}else if(current.path.equals(root)){
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
	public boolean onQueryTextChange(@NonNull String text){
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(@NonNull String text){
		if(current == null){
			return false;
		}
		if(TextUtils.isEmpty(text)){
			onClose();
			return false;
		}
		
		((MainActivity)getActivity()).searchView.clearFocus();

		String[] queries = TextUtils.split(text.toLowerCase(), " \t");

		ArrayList<RuuFileBase> filtered = new ArrayList<>();
		try{
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
		}catch(RuuFileBase.CanNotOpen e){
			Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), e.path), Toast.LENGTH_LONG).show();
			return false;
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
	private class RuuAdapter extends ArrayAdapter<RuuFileBase>{
		@Nullable private DirectoryInfo dirInfo;
		int musicNum = 0;
		int directoryNum = 0;

		RuuAdapter(@NonNull Context context){
			super(context, R.layout.list_item);
		}

		void setRuuFiles(@NonNull DirectoryInfo dirInfo) throws RuuFileBase.CanNotOpen{
			clear();
			searchQuery = null;
			this.dirInfo = dirInfo;

			RuuDirectory rootDirectory = RuuDirectory.rootDirectory(getContext());
			if(!rootDirectory.equals(dirInfo.path) && rootDirectory.contains(dirInfo.path)){
				add(dirInfo.path.getParent());
			}

			List<RuuDirectory> directories = dirInfo.path.getDirectories();
			directoryNum = directories.size();
			for(RuuDirectory dir: directories){
				add(dir);
			}

			List<RuuFile> musics = dirInfo.path.getMusics();
			musicNum = musics.size();
			for(RuuFile music: musics){
				add(music);
			}

			ListView listView = (ListView)getActivity().findViewById(R.id.playlist);
			if(listView != null){
				listView.setSelection(dirInfo.selection);
			}
		}

		void setSearchResults(@NonNull List<RuuFileBase> results){
			if(dirInfo != null){
				dirInfo.selection = ((ListView)getActivity().findViewById(R.id.playlist)).getFirstVisiblePosition();
			}
			clear();
			musicNum = 0;
			directoryNum = 0;

			for(RuuFileBase result: results){
				add(result);
			}

			for(RuuFileBase file: results){
				if(file.isDirectory()){
					directoryNum++;
				}else{
					musicNum++;
				}
			}

			ListView listView = (ListView)getActivity().findViewById(R.id.playlist);
			if(listView != null){
				listView.setSelection(0);
			}
		}

		void resumeFromSearch() throws RuuFileBase.CanNotOpen{
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
				}catch(RuuFileBase.CanNotOpen | RuuFileBase.OutOfRootDirectory e){
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

