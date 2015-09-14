package jp.blanktar.ruumusic;

import java.io.File;
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
import android.preference.PreferenceManager;


public class PlaylistFragment extends Fragment {
	private ArrayAdapter<String> adapter;
	private final Stack<DirectoryInfo> directoryCache = new Stack<>();
	DirectoryInfo current;

	class DirectoryInfo {
		public final RuuDirectory path;
		public final ArrayList<RuuDirectory> directories = new ArrayList<>();
		public final ArrayList<RuuFile> files = new ArrayList<>();
		public int selection = 0;

		public DirectoryInfo(@NonNull RuuDirectory path) {
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
				String selected = (String) lv.getItemAtPosition(position);
				try {
					if ((new File(current.path.getFullPath(), selected)).isDirectory()) {
						changeDir(new RuuDirectory(getContext(), current.path.getFullPath() + selected));
					} else {
						changeMusic(new RuuFile(getContext(), current.path.getFullPath() + selected));
					}
				} catch (RuuFileBase.CanNotOpen e) {
				}
			}
		});

		try {
			if (savedInstanceState == null) {
				changeDir(RuuDirectory.rootDirectory(getContext()));
			} else {
				String currentPath = savedInstanceState.getString("CURRENT_PATH");
				if (currentPath != null) {
					try {
						changeDir(new RuuDirectory(getContext(), currentPath));
					}catch(RuuFileBase.CanNotOpen e) {
						changeDir(RuuDirectory.rootDirectory(getContext()));
					}
				} else {
					changeDir(RuuDirectory.rootDirectory(getContext()));
				}
			}
		}catch(RuuFileBase.CanNotOpen err) {
			PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
					.putString("root_directory", "/")
					.apply();
		}
		
		return view;
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle state) {
		super.onSaveInstanceState(state);
		
		state.putString("CURRENT_PATH", current.path.getFullPath());
	}
	
	public void updateTitle(@NonNull Activity activity) {
		if(current == null) {
			activity.setTitle("");
		}else{
			try {
				activity.setTitle(current.path.getRuuPath());
			}catch(RuuFileBase.OutOfRootDirectory e) {
				activity.setTitle("");
				Toast.makeText(getActivity(), String.format(getString(R.string.out_of_root), current.path.getFullPath()), Toast.LENGTH_LONG).show();
			}
		}
	}
	private void updateTitle() {
		updateTitle(getActivity());
	}
	
	public void updateRoot() {
		updateTitle();
		updateMenu();
		changeDir(current.path);
	}
	
	void changeDir(@NonNull RuuDirectory dir){
		RuuDirectory rootDirectory;
		try {
			rootDirectory = RuuDirectory.rootDirectory(getContext());
		}catch(RuuFileBase.CanNotOpen e) {
			Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), "root directory"), Toast.LENGTH_LONG).show();
			return;
		}
		
		if(!rootDirectory.contains(dir)) {
			Toast.makeText(getActivity(), String.format(getString(R.string.out_of_root), dir.getFullPath()), Toast.LENGTH_LONG).show();
		}else if(!directoryCache.empty() && directoryCache.peek().path.equals(dir)) {
			current = directoryCache.pop();

			if(((MainActivity)getActivity()).getCurrentPage() == 1) {
				updateTitle();
			}

			adapter.clear();
			if(!rootDirectory.equals(current.path) && rootDirectory.contains(current.path)) {
				adapter.add("../");
			}
			for(RuuDirectory directory: current.directories) {
				adapter.add(directory.getName() + "/");
			}
			for(RuuFile file: current.files) {
				adapter.add(file.getName());
			}

			((ListView) getActivity().findViewById(R.id.playlist)).setSelection(current.selection);

			updateMenu();
		}else{
			RuuDirectory parent;
			try {
				parent = dir.getParent();
			}catch(RuuFileBase.CanNotOpen e) {
				parent = null;
			}
		
			if(!directoryCache.empty() && (parent == null || current != null && !current.path.equals(parent))){
				while(!directoryCache.empty()) {
					directoryCache.pop();
				}
			}else if(current != null) {
				current.selection = ((ListView) getActivity().findViewById(R.id.playlist)).getFirstVisiblePosition();
				directoryCache.push(current);
			}
			current = new DirectoryInfo(dir);

			if(((MainActivity)getActivity()).getCurrentPage() == 1) {
				updateTitle();
			}
			
			adapter.clear();
			
			if(!rootDirectory.equals(current.path) && rootDirectory.contains(current.path)) {
				adapter.add("../");
			}
			
			for(RuuDirectory directory: current.path.getDirectories()) {
				adapter.add(directory.getName() + "/");
				current.directories.add(directory);
			}
			
			for(RuuFile music: current.path.getMusics()) {
				adapter.add(music.getName());
				current.files.add(music);
			}

			ListView lv = (ListView)getActivity().findViewById(R.id.playlist);
			if(lv != null) {
				lv.setSelection(0);
			}
			
			updateMenu();
		}
	}
	
	public void updateMenu(@NonNull MainActivity activity) {
		Menu menu = activity.menu;
		if (menu != null) {
			RuuDirectory rootDirectory;
			try {
				rootDirectory = RuuDirectory.rootDirectory(getContext());
			}catch(RuuFileBase.CanNotOpen e) {
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), "root directory"), Toast.LENGTH_LONG).show();
				return;
			}
			
			RuuDirectory realRoot;
			try {
				realRoot = new RuuDirectory(getContext(), "/");
			}catch(RuuFileBase.CanNotOpen e) {
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), "/"), Toast.LENGTH_LONG).show();
				return;
			}

			menu.findItem(R.id.action_set_root).setVisible(!rootDirectory.equals(current.path));
			menu.findItem(R.id.action_unset_root).setVisible(!rootDirectory.equals(realRoot));
		}
	}
	
	private void updateMenu() {
		updateMenu((MainActivity)getActivity());
	}
	
	private void changeMusic(@NonNull RuuFile file) {
		Intent intent = new Intent(getActivity(), RuuService.class);
		intent.setAction("RUU_PLAY");
		intent.putExtra("path", file.getFullPath());
		getActivity().startService(intent);
		
		((MainActivity)getActivity()).moveToPlayer();
	}
	
	public boolean onBackKey() {
		RuuDirectory root;
		try {
			root = RuuDirectory.rootDirectory(getContext());
		}catch(RuuFileBase.CanNotOpen e) {
			return false;
		}

		if(current.path.equals(root)) {
			return false;
		}else{
			try {
				changeDir(current.path.getParent());
			}catch(RuuDirectory.CanNotOpen e) {
				Toast.makeText(getActivity(), String.format(getString(R.string.cant_open_dir), current.path.path.getParent()), Toast.LENGTH_LONG).show();
			}
			return true;
		}
	}
}

