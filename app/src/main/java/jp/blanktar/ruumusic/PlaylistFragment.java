package jp.blanktar.ruumusic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.view.Menu;

import android.util.Log;


public class PlaylistFragment extends Fragment {
	private ArrayAdapter<String> adapter;
	public File current;
	private Stack<DirectoryCache> directoryCache = new Stack<DirectoryCache>();
	private DirectoryCache currentCache;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_playlist, container, false);
	
		adapter = new ArrayAdapter<String>(view.getContext(), R.layout.list_item);
		
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

		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());

		if(savedInstanceState == null) {
			changeDir(new File(preference.getString("root_directory", "/")));
		}else{
			changeDir(new File(savedInstanceState.getString("CURRENT_PATH")));
		}
		
		return view;
	}
	
	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		
		state.putString("CURRENT_PATH", current.getPath());
	}
	
	public void updateTitle(Activity activity) {
		if(current == null) {
			activity.setTitle("");
		}else{
			String newtitle = current.getPath();

			SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(activity);
			String rootDirectory = preference.getString("root_directory", "/");

			if (newtitle.equals(rootDirectory)) {
				newtitle = "/";
			} else {
				newtitle = newtitle.substring(rootDirectory.length());
				if (!newtitle.startsWith("/")) {
					newtitle = "/" + newtitle;
				}
			}
			activity.setTitle(newtitle);
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
	
	private void changeDir(File dir){
		try {
			dir = dir.getCanonicalFile();
		}catch(IOException e) {
		}

		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String rootDirectory = preference.getString("root_directory", "/");
		File rootfile = new File(rootDirectory);
		
		if(!dir.equals(rootfile)
		&& !dir.getPath().startsWith(rootfile.getPath())) {
			Toast.makeText(getActivity(), String.format(getString(R.string.out_of_root), dir.getPath()), Toast.LENGTH_LONG).show();
			return;
		}else if(!dir.isDirectory()) {
			Toast.makeText(getActivity(), String.format(getString(R.string.is_not_directory), dir.getPath()), Toast.LENGTH_LONG).show();
			return;
		}else if(!directoryCache.empty() && directoryCache.peek().path.equals(dir)) {
			currentCache = directoryCache.pop();
			
			current = dir;
			
			if(((MainActivity)getActivity()).getCurrentPage() == 1) {
				updateTitle();
			}
			
			adapter.clear();
			if(current.getParentFile() != null && !current.equals(rootfile)) {
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
					current = new File(rootDirectory);
				}
				
				if(((MainActivity)getActivity()).getCurrentPage() == 1) {
					updateTitle();
				}

				Arrays.sort(files);
				
				adapter.clear();
				
				if(current.getParentFile() != null && !current.equals(rootfile)) {
					adapter.add("../");
				}
				
				String before = "";
				for (File file : files) {
					String name = file.getName();
					if(name.length() > 0 && name.charAt(0) != '.') {
						if (file.isDirectory()) {
							adapter.add(name + "/");
							currentCache.files.add(name + "/");
						}else if(FileTypeUtil.isSupported(file.getName())) {
							name = name.substring(0, name.lastIndexOf("."));

							if(!name.equals(before)) {
								adapter.add(name);
								currentCache.files.add(name);
								before = name;
							}
						}
					}
				}

				ListView lv = (ListView)getActivity().findViewById(R.id.playlist);
				if(lv != null) {
					lv.setSelection(0);
				}
				
				updateMenu();
			}
		}
	}
	
	public void updateMenu(MainActivity activity) {
		Menu menu = activity.menu;
		if (menu != null) {
			SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(activity);
			File rootDirectory = new File(preference.getString("root_directory", "/"));

			menu.findItem(R.id.action_set_root).setVisible(!rootDirectory.equals(current));
			menu.findItem(R.id.action_unset_root).setVisible(!rootDirectory.equals(new File("/")));
		}
	}
	
	private void updateMenu() {
		updateMenu((MainActivity)getActivity());
	}
	
	private void changeMusic(String file) {
		Intent intent = new Intent(getActivity(), RuuService.class);
		intent.setAction("RUU_PLAY");
		intent.putExtra("path", file);
		getActivity().startService(intent);
		
		((MainActivity)getActivity()).moveToPlayer();
	}
	
	public boolean onBackKey() {
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
		
		if(current.equals(new File(preference.getString("root_directory", "/")))) {
			return false;
		}else{
			changeDir(current.getParentFile());
			return true;
		}
	}
}


class DirectoryCache {
	public File path;
	public ArrayList<String> files = new ArrayList<String>();
	public int selection = 0;
	
	public DirectoryCache(File path) {
		this.path = path;
	}
}

