package jp.blanktar.ruumusic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

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
				String path_to = (String) lv.getItemAtPosition(position);
				if (path_to != "/") {
					File file = new File(current, path_to);
					if (file.isDirectory()) {
						changeDir(file);
					} else {
						changeMusic(file.getPath());
					}
				} else {
					SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
					changeDir(new File(preference.getString("root_directory", "/")));
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
			Log.e("RuuMusic playlist", "access to out of root: " + dir.getPath());
		}else if(!dir.isDirectory()) {
			Toast.makeText(getActivity(), dir.getPath() + " is not directory", Toast.LENGTH_LONG).show();
		}else{
			File[] files = dir.listFiles();
			
			if(files == null) {
				Toast.makeText(getActivity(), "can't open " + dir.getPath(), Toast.LENGTH_LONG).show();
			}else{
				current = dir;
				if(current.getPath().equals("")) {
					current = new File(rootDirectory);
				}
				
				if(((MainActivity)getActivity()).getCurrentPage() == 1) {
					updateTitle();
				}
				
				adapter.clear();
				if(current.getParentFile() != null && !current.equals(rootfile)) {
					adapter.add("/");
					if (!current.getParentFile().equals(rootfile)) {
						adapter.add("../");
					}
				}

				Arrays.sort(files);
				String before = "";
				for (File file : files) {
					String name = file.getName();
					if(name.length() > 0 && name.charAt(0) != '.') {
						if (file.isDirectory()) {
							adapter.add(name + "/");
						}else if(FileTypeUtil.isSupported(file.getName())) {
							name = name.substring(0, name.lastIndexOf("."));

							if(!name.equals(before)) {
								adapter.add(name);
								before = name;
							}
						}
					}
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
