package jp.blanktar.ruumusic.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.service.RuuService;
import jp.blanktar.ruumusic.util.Preference;
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

		registerForContextMenu(lv);

		String currentPath = Preference.Str.CURRENT_VIEW_PATH.get(getContext());
		try{
			if(currentPath != null){
				changeDir(RuuDirectory.getInstance(getContext(), currentPath));
			}else{
				changeDir(RuuDirectory.getInstance(getContext(), Environment.getExternalStorageDirectory()));
			}
		}catch(RuuFileBase.CanNotOpen err){
			try{
				changeDir(RuuDirectory.rootDirectory(getContext()));
			}catch(RuuFileBase.CanNotOpen e){
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), e.path), Toast.LENGTH_LONG).show();
				Preference.Str.ROOT_DIRECTORY.remove(getContext());
			}
		}

		return view;
	}

	@Override
	public void onPause(){
		super.onPause();

		if(current != null){
			Preference.Str.CURRENT_VIEW_PATH.set(getContext(), current.path.getFullPath());
		}
	}

	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View view, @NonNull ContextMenu.ContextMenuInfo info){
		super.onCreateContextMenu(menu, view, info);
		RuuFileBase file = adapter.getItem(((AdapterView.AdapterContextMenuInfo)info).position);

		if(file.isDirectory()){
			menu.setHeaderTitle(file.getName() + "/");
			getActivity().getMenuInflater().inflate(R.menu.directory_context_menu, menu);
		}else{
			menu.setHeaderTitle(file.getName());
			getActivity().getMenuInflater().inflate(R.menu.music_context_menu, menu);
		}

		try{
			if(getActivity().getPackageManager().queryIntentActivities(getOpenFileIntent(file), 0).size() == 0){
				menu.findItem(R.id.action_open_with_other_app).setVisible(false);
			}
		}catch(RuuFileBase.CanNotOpen e){
			menu.findItem(R.id.action_open_with_other_app).setVisible(false);
		}
	}

	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item){
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		RuuFileBase file = adapter.getItem(info.position);

		switch(item.getItemId()){
			case R.id.action_open_directory:
				changeDir((RuuDirectory)file);
				return true;
			case R.id.action_open_music:
				changeMusic((RuuFile)file);
				return true;
			case R.id.action_open_with_other_app:
				try{
					startActivity(getOpenFileIntent(file));
				}catch(RuuFileBase.CanNotOpen e){
					Toast.makeText(getActivity(), getString(R.string.music_not_found), Toast.LENGTH_LONG).show();
				}
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	@NonNull
	private Intent getOpenFileIntent(@NonNull RuuFileBase ruufile) throws RuuFileBase.CanNotOpen{
		File file;
		String mimetype;
		if(ruufile.isDirectory()){
			file = ruufile.path;
			mimetype = "text/directory";
		}else{
			file = new File(((RuuFile)ruufile).getRealPath());
			mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName().substring(ruufile.getName().length()+1));
		}
		return (new Intent(Intent.ACTION_VIEW)).setDataAndType(Uri.fromFile(file), mimetype).putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
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
			menu.findItem(R.id.action_recursive_play).setVisible(searchQuery == null && adapter.getCount() > 1);
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
			for(int i=1;; i++){
				try{
					changeDir(current.path.getParent(i));
					return true;
				}catch(RuuFileBase.CanNotOpen e){
					if(e.path == null){
						return false;
					}
				}
			}
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
			Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), e.path), Toast.LENGTH_LONG).show();
			adapter.clear();
		}

		searchQuery = null;
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

		RuuAdapter(@NonNull Context context){
			super(context, R.layout.list_item);
		}

		void setRuuFiles(@NonNull DirectoryInfo dirInfo) throws RuuFileBase.CanNotOpen{
			clear();
			searchQuery = null;
			this.dirInfo = dirInfo;
			musicNum = 0;

			RuuDirectory rootDirectory = RuuDirectory.rootDirectory(getContext());
			if(!rootDirectory.equals(dirInfo.path) && rootDirectory.contains(dirInfo.path)){
				for(int i=1; ; i++){
					try{
						add(dirInfo.path.getParent(i));
						break;
					}catch(RuuFileBase.CanNotOpen e){
						if(e.path == null){
							break;
						}
					}
				}
			}

			List<RuuDirectory> directories = dirInfo.path.getDirectories();
			for(RuuDirectory dir: directories){
				add(dir);
			}

			List<RuuFile> musics = dirInfo.path.getMusics();
			musicNum = musics.size();
			for(RuuFile music: musics){
				add(music);
				musicNum++;
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

			for(RuuFileBase result: results){
				add(result);

				if(!result.isDirectory()){
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

