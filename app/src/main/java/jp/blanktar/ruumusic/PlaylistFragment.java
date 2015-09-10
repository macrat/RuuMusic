package jp.blanktar.ruumusic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import android.support.annotation.NonNull;
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


public class PlaylistFragment extends Fragment {
	private ArrayAdapter<String> adapter;
	public File current;
	private final Stack<DirectoryCache> directoryCache = new Stack<>();
	private DirectoryCache currentCache;

	private class DirectoryCache {
		public final File path;
		public final ArrayList<String> files = new ArrayList<>();
		public int selection = 0;

		public DirectoryCache(@NonNull File path) {
			this.path = path;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_playlist, container, false);
	
		adapter = new ArrayAdapter<>(view.getContext(), R.layout.list_item);
		
		final ListView lv = (ListView)view.findViewById(R.id.playlist);
		lv.setAdapter(adapter);
		
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File file = new File(current, (String) lv.getItemAtPosition(position));
				if (file.isDirectory()) {
					changeDir(file);
				} else {
					changeMusic(file.getPath());
				}
			}
		});

		if(savedInstanceState == null) {
			changeDir(FileTypeUtil.getRootDirectory(getActivity()));
		}else{
			String currentPath = savedInstanceState.getString("CURRENT_PATH");
			if(currentPath != null) {
				changeDir(new File(currentPath));
			}else {
				changeDir(FileTypeUtil.getRootDirectory(getActivity()));
			}
		}
		
		return view;
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle state) {
		super.onSaveInstanceState(state);
		
		state.putString("CURRENT_PATH", current.getPath());
	}
	
	public void updateTitle(@NonNull Activity activity) {
		if(current == null) {
			activity.setTitle("");
		}else{
			activity.setTitle(FileTypeUtil.getPathFromRoot(getActivity(), current));
		}
	}
	private void updateTitle() {
		updateTitle(getActivity());
	}
	
	public void updateRoot() {
		updateTitle();
		updateMenu();
		changeDir(current);
	}
	
	protected void changeDir(@NonNull File dir){
		try {
			dir = dir.getCanonicalFile();
		}catch(IOException e) {
			Toast.makeText(getActivity(), String.format(getString(R.string.failed_get_canonical_path), dir.getPath()), Toast.LENGTH_LONG).show();
			return;
		}

		File rootDirectory = FileTypeUtil.getRootDirectory(getActivity());
		
		if(!dir.equals(rootDirectory)
		&& !dir.getPath().startsWith(rootDirectory.getPath())) {
			Toast.makeText(getActivity(), String.format(getString(R.string.out_of_root), dir.getPath()), Toast.LENGTH_LONG).show();
		}else if(!dir.isDirectory()) {
			Toast.makeText(getActivity(), String.format(getString(R.string.is_not_directory), dir.getPath()), Toast.LENGTH_LONG).show();
		}else if(!directoryCache.empty() && directoryCache.peek().path.equals(dir)) {
			currentCache = directoryCache.pop();
			
			current = dir;
			
			if(((MainActivity)getActivity()).getCurrentPage() == 1) {
				updateTitle();
			}
			
			adapter.clear();
			if(current.getParentFile() != null && !current.equals(rootDirectory)) {
				adapter.add("../");
			}
			for(String file: currentCache.files) {
				adapter.add(file);
			}
			
			((ListView) getActivity().findViewById(R.id.playlist)).setSelection(currentCache.selection);

			updateMenu();
		}else{
			File[] files = dir.listFiles();
			
			if(files == null) {
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), dir.getPath()), Toast.LENGTH_LONG).show();
			}else {
				if(!directoryCache.empty() && !currentCache.path.equals(dir.getParentFile())){
					while(!directoryCache.empty()) {
						directoryCache.pop();
					}
				}else if(currentCache != null) {
					currentCache.selection = ((ListView) getActivity().findViewById(R.id.playlist)).getFirstVisiblePosition();
					directoryCache.push(currentCache);
				}
				currentCache = new DirectoryCache(dir);

				current = dir;
				if(current.getPath().equals("")) {
					current = rootDirectory;
				}
				
				if(((MainActivity)getActivity()).getCurrentPage() == 1) {
					updateTitle();
				}
				
				adapter.clear();
				
				if(current.getParentFile() != null && !current.equals(rootDirectory)) {
					adapter.add("../");
				}
				
				for(File directory: FileTypeUtil.getDirectories(files)) {
					adapter.add(directory.getName() + "/");
					currentCache.files.add(directory.getName() + "/");
				}
				
				for(File music: FileTypeUtil.getMusics(files)) {
					adapter.add(music.getName());
					currentCache.files.add(music.getName());
				}

				ListView lv = (ListView)getActivity().findViewById(R.id.playlist);
				if(lv != null) {
					lv.setSelection(0);
				}
				
				updateMenu();
			}
		}
	}
	
	public void updateMenu(@NonNull MainActivity activity) {
		Menu menu = activity.menu;
		if (menu != null) {
			File rootDirectory = FileTypeUtil.getRootDirectory(getActivity());

			menu.findItem(R.id.action_set_root).setVisible(!rootDirectory.equals(current));
			menu.findItem(R.id.action_unset_root).setVisible(!rootDirectory.equals(new File("/")));
		}
	}
	
	private void updateMenu() {
		updateMenu((MainActivity)getActivity());
	}
	
	private void changeMusic(@NonNull String file) {
		Intent intent = new Intent(getActivity(), RuuService.class);
		intent.setAction("RUU_PLAY");
		intent.putExtra("path", file);
		getActivity().startService(intent);
		
		((MainActivity)getActivity()).moveToPlayer();
	}
	
	public boolean onBackKey() {
		if(current.equals(FileTypeUtil.getRootDirectory(getActivity()))) {
			return false;
		}else{
			changeDir(current.getParentFile());
			return true;
		}
	}
}

