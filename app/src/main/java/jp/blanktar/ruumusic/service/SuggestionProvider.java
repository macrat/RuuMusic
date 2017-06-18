package jp.blanktar.ruumusic.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;

import jp.blanktar.ruumusic.util.RuuDirectory;
import jp.blanktar.ruumusic.util.RuuFile;


public class SuggestionProvider extends ContentProvider{
	public boolean onCreate(){
		return true;
	}

	private boolean isMatch(@NonNull RuuFile music, @NonNull String[] queries){
		for(String query: queries){
			if(!music.getName().contains(query)){
				return false;
			}
		}
		return true;
	}

	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder){
		if(selectionArgs == null || selectionArgs.length <= 0 || TextUtils.isEmpty(selectionArgs[0]) || getContext() == null){
			return null;
		}

		RuuDirectory rootDir;
		try{
			rootDir = RuuDirectory.rootDirectory(getContext());
		}catch(RuuDirectory.NotFound e){
			return null;
		}

		String[] queries = TextUtils.split(selectionArgs[0].toLowerCase(), "\\s");

		RuuCursor result = new RuuCursor();
		for(RuuFile music: rootDir.getMusicsRecursive()){
			if(isMatch(music, queries)){
				try{
					result.addRuuFile(music);
				}catch(RuuDirectory.OutOfRootDirectory e){
				}
			}
		}
		return result;
	}

	public Uri insert(@NonNull Uri uri, @Nullable ContentValues values){
		return uri;
	}

	public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs){
		return 0;
	}

	public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs){
		return 0;
	}

	public String getType(@NonNull Uri uri){
		return null;
	}


	private class RuuCursor extends MatrixCursor{
		private int count = 0;
	

		private RuuCursor(){
			super(new String[]{
				"_id",
				SearchManager.SUGGEST_COLUMN_TEXT_1,
				SearchManager.SUGGEST_COLUMN_TEXT_2,
				SearchManager.SUGGEST_COLUMN_INTENT_DATA,
			});
		}

		private void addRuuFile(@NonNull RuuFile music) throws RuuDirectory.OutOfRootDirectory{
			addRow(new Object[]{
				count,
				music.getName(),
				music.getParent().getRuuPath(),
				(new Uri.Builder()).scheme("file").path(music.getRealPath()).toString()
			});
			count++;
		}
	}
}
