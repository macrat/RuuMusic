package jp.blanktar.ruumusic.util;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.IntRange;
import android.preference.PreferenceManager;
import android.content.Context;


public class Preference{
	@NonNull public final static String AUDIO_PREFIX = "audio_";


	public enum Int{
		LAST_PLAY_POSITION("last_play_position", 0),
		BASSBOOST_LEVEL(AUDIO_PREFIX + "bassboost_level", 0),
		REVERB_TYPE(AUDIO_PREFIX + "reverb_type", 0),
		LOUDNESS_LEVEL(AUDIO_PREFIX + "loudness_level", 0),
		LAST_VIEW_PAGE("last_view_page", 1);


		private final int defaultValue;
		@NonNull private final String key;

		Int(@NonNull String key, int defaultValue){
			this.key = key;
			this.defaultValue = defaultValue;
		}

		@CheckResult
		public int get(@NonNull Context context){
			return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, defaultValue);
		}

		public void set(@NonNull Context context, int value){
			PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
		}

		public void remove(@NonNull Context context){
			PreferenceManager.getDefaultSharedPreferences(context).edit().remove(key).apply();
		}
	}

	public enum Str{
		REPEAT_MODE("repeat_mode", "off"),
		RECURSIVE_PATH("recursive_path", null),
		SEARCH_PATH("search_path", null),
		SEARCH_QUERY("search_query", null),
		LAST_PLAY_MUSIC("last_play_music", null),
		ROOT_DIRECTORY("root_directory", null),
		CURRENT_VIEW_PATH("current_view_path", null);


		@Nullable private final String defaultValue;
		@NonNull private final String key;

		Str(@NonNull String key, @Nullable String defaultValue){
			this.key = key;
			this.defaultValue = defaultValue;
		}

		@Nullable
		@CheckResult
		public String get(@NonNull Context context){
			return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
		}

		public void set(@NonNull Context context, @Nullable String value){
			PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
		}

		public void remove(@NonNull Context context){
			PreferenceManager.getDefaultSharedPreferences(context).edit().remove(key).apply();
		}
	}

	public enum Bool{
		SHUFFLE_MODE("shuffle_mode", false),
		BASSBOOST_ENABLED(AUDIO_PREFIX + "basboost_enabled", false),
		REVERB_ENABLED(AUDIO_PREFIX + "reverb_enabled", false),
		LOUDNESS_ENABLED(AUDIO_PREFIX + "loudness_enabled", false),
		EQUALIZER_ENABLED(AUDIO_PREFIX + "equalizer_enabled", false);


		private final boolean defaultValue;
		@NonNull private final String key;

		Bool(@NonNull String key, boolean defaultValue){
			this.key = key;
			this.defaultValue = defaultValue;
		}

		@CheckResult
		public boolean get(@NonNull Context context){
			return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
		}

		public void set(@NonNull Context context, boolean value){
			PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
		}
	}

	public static class IntArray{
		public static class EQUALIZER_LEVEL{
			@NonNull private final static String key = AUDIO_PREFIX + "equalizer_level_";

			@CheckResult
			public static int get(@NonNull Context context, @IntRange(from=0) int index){
				return PreferenceManager.getDefaultSharedPreferences(context).getInt(key + index, 0);
			}

			public static void set(@NonNull Context context, @IntRange(from=0) int index, int value){
				PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key + index, value).apply();
			}
		}
	}
}

