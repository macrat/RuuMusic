package jp.blanktar.ruumusic.client.main;

import java.util.List;
import java.util.Stack;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import jp.blanktar.ruumusic.R;
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


	@Override
	@NonNull
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fragment_playlist, container, false);

		preference = new Preference(view.getContext());
		client = new RuuClient(getContext());

		filer = (FilerView)view.findViewById(R.id.filer);

		filer.setOnEventListener(new FilerView.OnEventListener(){
			@Override
			public void onMusicClick(@NonNull RuuFile music){
				changeMusic(music);
			}

			@Override
			public void onDirectoryClick(@NonNull RuuDirectory dir){
				changeDir(dir);
			}

			@Override
			public void onParentClick(){
				try{
					changeDir(current.path.getParent());
				}catch(RuuFileBase.OutOfRootDirectory e){
				}
			}

			@Override
			public void onMusicLongClick(@NonNull RuuFile music, @NonNull ContextMenu menu){
				final boolean openable = getActivity().getPackageManager().queryIntentActivities(music.toIntent(), 0).size() > 0;

				menu.setHeaderTitle(music.getName());
				getActivity().getMenuInflater().inflate(R.menu.music_context_menu, menu);
				menu.findItem(R.id.action_open_music_with_other_app).setVisible(openable);
			}

			@Override
			public void onDirectoryLongClick(@NonNull RuuDirectory dir, @NonNull ContextMenu menu){
				final boolean openable = getActivity().getPackageManager().queryIntentActivities(dir.toIntent(), 0).size() > 0;

				menu.setHeaderTitle(dir.getName() + "/");
				getActivity().getMenuInflater().inflate(R.menu.directory_context_menu, menu);
				menu.findItem(R.id.action_open_dir_with_other_app).setVisible(openable);
			}
	
			@Override
			public void onParentLongClick(@NonNull ContextMenu menu){
				RuuDirectory dir;
				try{
					dir = current.path.getParent();
				}catch(RuuFileBase.OutOfRootDirectory e){
					return;
				}
				final boolean openable = getActivity().getPackageManager().queryIntentActivities(dir.toIntent(), 0).size() > 0;

				menu.setHeaderTitle(dir.getName() + "/");
				getActivity().getMenuInflater().inflate(R.menu.directory_context_menu, menu);
				menu.findItem(R.id.action_open_dir_with_other_app).setVisible(openable);
			}
		});

		String currentPath = preference.CurrentViewPath.get();
		try{
			if(currentPath != null){
				changeDir(RuuDirectory.getInstance(getContext(), currentPath));

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
		}

		return view;
	}

	@Override
	public void onDestroy(){
		client.release();
		super.onDestroy();
	}

	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item){
		RuuFileBase file = filer.getFiles().get(((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position);

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

	public void updateTitle(@NonNull Toolbar toolbar){
		RuuDirectory root = null;
		try{
			root = RuuDirectory.rootDirectory(getContext());
		}catch(RuuDirectory.NotFound e){
		}

		RuuDirectory parent = null;
		try{
			parent = current.path.getParent();
		}catch(RuuDirectory.OutOfRootDirectory e){
		}

		if(current == null){
			toolbar.setTitle("");
			toolbar.setSubtitle("");
		}else if(current.path == root){
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
			changeDir(current.path);
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
				current.state = filer.getLayoutState();
				directoryCache.push(current);
			}

			current = new DirectoryInfo(dir);
		}

		preference.CurrentViewPath.set(current.path.getFullPath());

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
			RuuDirectory rootDirectory;
			try{
				rootDirectory = RuuDirectory.rootDirectory(getContext());
			}catch(RuuFileBase.NotFound e){
				Toast.makeText(getActivity(), getString(R.string.cant_open_dir, "root directory"), Toast.LENGTH_LONG).show();
				return;
			}

			menu.findItem(R.id.action_set_root).setVisible(current != null && !rootDirectory.equals(current.path) && searchQuery == null);
			menu.findItem(R.id.action_unset_root).setVisible(!rootDirectory.getFullPath().equals("/") && searchQuery == null);
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
		changeDir(current.path);

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
