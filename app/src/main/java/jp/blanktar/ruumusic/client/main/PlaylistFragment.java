package jp.blanktar.ruumusic.client.main;

import java.util.List;
import java.util.Stack;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.util.DynamicShortcuts;
import jp.blanktar.ruumusic.util.PermissionManager;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.util.RuuClient;
import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;
import jp.blanktar.ruumusic.util.RuuFileBase;
import jp.blanktar.ruumusic.view.FilerView;


@UiThread
public class PlaylistFragment extends Fragment implements SearchView.OnQueryTextListener, SearchView.OnCloseListener{
	private Preference preference;
	private RuuClient client;

	private FilerView filer;
	private final Stack<DirectoryInfo> directoryCache = new Stack<>();
	@NonNull private ListStatus status = ListStatus.LOADING;

	@Nullable DirectoryInfo current;
	@Nullable public String searchQuery = null;

	PermissionManager permissionManager;


	@Override
	@NonNull
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fragment_playlist, container, false);

		preference = new Preference(view.getContext());
		client = new RuuClient(getContext());

		filer = view.findViewById(R.id.filer);

		filer.setOnEventListener(new FilerView.OnEventListener(){
			@Override
			public void onClick(@NonNull RuuFileBase file){
				if(file.isDirectory()){
					changeDir((RuuDirectory)file);
				}else{
					changeMusic((RuuFile)file);
				}
			}

			@Override
			public void onLongClick(@NonNull final RuuFileBase file, @NonNull ContextMenu menu){
				menu.setHeaderTitle(file.getName() + (file.isDirectory() ? "/" : ""));
				if(file.isDirectory()){
					getActivity().getMenuInflater().inflate(R.menu.directory_context_menu, menu);
					menu.findItem(R.id.action_open_dir_with_other_app).setVisible(getActivity().getPackageManager().queryIntentActivities(file.toIntent(), 0).size() > 0);
				}else{
					getActivity().getMenuInflater().inflate(R.menu.music_context_menu, menu);
					menu.findItem(R.id.action_open_music_with_other_app).setVisible(getActivity().getPackageManager().queryIntentActivities(file.toIntent(), 0).size() > 0);
				}

				MenuItem item;

				item = menu.findItem(R.id.action_open_directory);
				if(item != null){
					item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							changeDir((RuuDirectory)file);
							return true;
						}
					});
				}

				item = menu.findItem(R.id.action_open_music);
				if(item != null){
					item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							changeMusic((RuuFile)file);
							return true;
						}
					});
				}

				item = menu.findItem(R.id.action_open_music_with_other_app);
				if(item != null){
					final Intent intent = Build.VERSION.SDK_INT >= 24 ? ((RuuFile)file).toContentIntent() : file.toIntent();
					item.setEnabled(getActivity().getPackageManager().queryIntentActivities(intent, 0).size() > 0);
					item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							startActivity(intent);
							return true;
						}
					});
				}

				item = menu.findItem(R.id.action_open_dir_with_other_app);
				if(item != null){
					if(Build.VERSION.SDK_INT >= 24){
						item.setVisible(false);
					}else{
						item.setVisible(true);
						item.setEnabled(getActivity().getPackageManager().queryIntentActivities(file.toIntent(), 0).size() > 0);
						item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								startActivity(file.toIntent());
								return true;
							}
						});
					}
				}

				item = menu.findItem(R.id.action_web_search_music);
				if(item != null){
					item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							startActivity((new Intent(Intent.ACTION_WEB_SEARCH)).putExtra(SearchManager.QUERY, file.getName()));
							return true;
						}
					});
				}

				item = menu.findItem(R.id.action_web_search_dir);
				if(item != null){
					item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							startActivity((new Intent(Intent.ACTION_WEB_SEARCH)).putExtra(SearchManager.QUERY, file.getName()));
							return true;
						}
					});
				}

				item = menu.findItem(R.id.action_pin_shortcut_music);
				if(item != null){
					if (!(new DynamicShortcuts(getContext()).isRequestPinSupported())) {
						item.setVisible(false);
					}else{
						item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								new DynamicShortcuts(getContext()).requestPin(getContext(), file);
								return true;
							}
						});
					}
				}

				item = menu.findItem(R.id.action_pin_shortcut_directory);
				if(item != null){
					if (!(new DynamicShortcuts(getContext()).isRequestPinSupported())) {
						item.setVisible(false);
					}else{
						item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								new DynamicShortcuts(getContext()).requestPin(getContext(), file);
								return true;
							}
						});
					}
				}
			}
		});

		if(permissionManager != null && permissionManager.getHasPermission()){
			resumeDirectory();
		}

		return view;
	}

	public void onPermissionGranted() {
		if(current == null){
			resumeDirectory();
		}
	}

	public void resumeDirectory() {
		RuuDirectory currentPath = preference.CurrentViewPath.get();
		try{
			if(currentPath != null){
				changeDir(currentPath);

				searchQuery = preference.LastSearchQuery.get();
				if(searchQuery != null){
					onQueryTextSubmit(searchQuery);
				}
			}else{
				RuuDirectory dir = RuuDirectory.rootCandidate(getContext());
				RuuDirectory root = RuuDirectory.rootDirectory(getContext());
				if(!root.contains(dir)){
					changeDir(root);
				}else{
					changeDir(dir);
				}
			}
		}catch(RuuFileBase.NotFound err){
			try{
				changeDir(RuuDirectory.rootDirectory(getContext()));
			}catch(RuuFileBase.NotFound e){
				Toast.makeText(getActivity(), getString(R.string.cant_open_dir, e.path), Toast.LENGTH_LONG).show();
				preference.RootDirectory.remove();
			}
		}catch(SecurityException e){
		}
	}

	@Override
	public void onDestroy(){
		client.release();
		super.onDestroy();
	}

	public void updateTitle(@NonNull Toolbar toolbar){
		RuuDirectory root = null;
		try{
			root = RuuDirectory.rootDirectory(getContext());
		}catch(RuuDirectory.NotFound e){
		}catch(SecurityException e){
			return;
		}

		RuuDirectory parent = null;
		try{
			parent = current.path.getParent();
		}catch(RuuDirectory.OutOfRootDirectory | NullPointerException e){
		}

		if(current == null){
			toolbar.setTitle("");
			toolbar.setSubtitle("");
		}else if(current.path.equals(root)){
			toolbar.setTitle("/");
			toolbar.setSubtitle("");
		}else if(parent == root){
			toolbar.setTitle("/" + current.path.getName() + "/");
			toolbar.setSubtitle("");
		}else{
			toolbar.setTitle(current.path.getName() + "/");
			try{
				toolbar.setSubtitle(parent.getRuuPath());
			}catch(RuuFileBase.OutOfRootDirectory e){
				toolbar.setSubtitle("");
			}
		}
	}

	private void updateTitle(){
		updateTitle((Toolbar)getActivity().findViewById(R.id.toolbar));
	}

	public void updateRoot(){
		updateTitle();
		updateMenu();
		if(current != null){
			try{
				RuuDirectory root = RuuDirectory.rootDirectory(getContext());
				if(root.contains(current.path)){
					if(searchQuery != null){
						onQueryTextSubmit(searchQuery);
					}else{
						changeDir(current.path);
					}
				}else{
					changeDir(root);
				}
			}catch(RuuFileBase.NotFound e){
			}
		}
	}

	private void updateStatus(@NonNull ListStatus status){
		this.status = status;

		try{
			switch(status){
				case SHOWN:
					filer.setLoading(false);
					getActivity().findViewById(R.id.empty_list).setVisibility(View.GONE);
					getActivity().findViewById(R.id.filer).setVisibility(View.VISIBLE);
					break;
				case LOADING:
					filer.setLoading(true);
					getActivity().findViewById(R.id.empty_list).setVisibility(View.GONE);
					getActivity().findViewById(R.id.filer).setVisibility(View.VISIBLE);
					break;
				case EMPTY:
					getActivity().findViewById(R.id.filer).setVisibility(View.GONE);
					getActivity().findViewById(R.id.empty_list).setVisibility(View.VISIBLE);
					break;
			}
		}catch(NullPointerException e) {
		}
	}

	public void updateStatus(){
		updateStatus(status);
	}

	void changeDir(@NonNull RuuDirectory dir){
		try{
			dir.getRuuPath();
		}catch(RuuFileBase.OutOfRootDirectory e){
			Toast.makeText(getActivity(), getString(R.string.out_of_root, dir.getFullPath()), Toast.LENGTH_LONG).show();
			return;
		}

		if(searchQuery != null){
			SearchView search = ((MainActivity)getActivity()).searchView;
			if(search != null && !search.isIconified()){
				searchQuery = null;
				search.setQuery("", false);
				search.setIconified(true);
			}
		}

		while(!directoryCache.empty() && !directoryCache.peek().path.contains(dir)){
			directoryCache.pop();
		}

		if(!directoryCache.empty() && directoryCache.peek().path.equals(dir)){
			current = directoryCache.pop();
		}else{
			if(current != null){
				current.state = filer.getLayoutState();
				directoryCache.push(current);
			}

			current = new DirectoryInfo(dir);
		}

		preference.CurrentViewPath.set(current.path);

		if(((MainActivity)getActivity()).getCurrentPage() == MainActivity.Page.PLAYLIST){
			updateTitle();
		}

		filer.setShowPath(false);
		filer.changeDirectory(current.path);
		if(current.state != null){
			filer.setLayoutState(current.state);
		}
		updateStatus(ListStatus.SHOWN);
	}

	public void updateMenu(@NonNull MainActivity activity){
		if(activity.getCurrentPage() != MainActivity.Page.PLAYLIST){
			return;
		}

		Menu menu = activity.menu;
		if(menu != null){
			menu.findItem(R.id.action_search_play).setVisible(searchQuery != null);
			menu.findItem(R.id.action_search_play).setEnabled(filer.getHasContent());
			menu.findItem(R.id.action_recursive_play).setVisible(searchQuery == null && filer.getHasContent());
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
				final List<RuuFileBase> filtered = current.path.search(text);

				handler.post(new Runnable(){
					@Override
					public void run(){
						searchQuery = text;
						filer.setShowPath(true);
						filer.changeFiles(filtered, null);
						if(filer.getHasContent()){
							updateStatus(ListStatus.SHOWN);
						}else{
							updateStatus(ListStatus.EMPTY);
						}
						preference.LastSearchQuery.set(text);
					}
				});
			}
		})).start();

		return false;
	}

	@Override
	public boolean onClose(){
		if(searchQuery != null){
			changeDir(current.path);
		}

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
		public Parcelable state;

		public DirectoryInfo(@NonNull RuuDirectory path){
			this.path = path;
		}
	}
}
