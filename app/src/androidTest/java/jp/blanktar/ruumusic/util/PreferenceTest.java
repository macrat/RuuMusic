package jp.blanktar.ruumusic.util;

import android.support.test.runner.AndroidJUnit4;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import android.support.test.InstrumentationRegistry;
import android.content.Context;


@RunWith(AndroidJUnit4.class)
public class PreferenceTest{
	Context context;
	
	@Before
	public void before(){
		context = InstrumentationRegistry.getTargetContext();
	}

	@Test
	public void intSettings(){
		Preference.Int.LAST_PLAY_POSITION.set(context, 10);
		assertEquals(Preference.Int.LAST_PLAY_POSITION.get(context), 10);

		Preference.Int.LAST_PLAY_POSITION.remove(context);
		assertEquals(Preference.Int.LAST_PLAY_POSITION.get(context), 0);

		Preference.Int.LAST_VIEW_PAGE.remove(context);
		assertEquals(Preference.Int.LAST_VIEW_PAGE.get(context), 1);
	}

	@Test
	public void strSettings(){
		Preference.Str.REPEAT_MODE.set(context, "loop");
		assertEquals(Preference.Str.REPEAT_MODE.get(context), "loop");

		Preference.Str.REPEAT_MODE.remove(context);
		assertEquals(Preference.Str.REPEAT_MODE.get(context), "off");

		Preference.Str.RECURSIVE_PATH.set(context, "test");
		assertEquals(Preference.Str.RECURSIVE_PATH.get(context), "test");

		Preference.Str.RECURSIVE_PATH.set(context, null);
		assertEquals(Preference.Str.RECURSIVE_PATH.get(context), null);

		Preference.Str.RECURSIVE_PATH.remove(context);
		assertEquals(Preference.Str.RECURSIVE_PATH.get(context), null);
	}

	@Test
	public void boolSettings(){
		Preference.Bool.SHUFFLE_MODE.set(context, false);
		assertEquals(Preference.Bool.SHUFFLE_MODE.get(context), false);

		Preference.Bool.SHUFFLE_MODE.set(context, true);
		assertEquals(Preference.Bool.SHUFFLE_MODE.get(context), true);
	}
	
	@Test
	public void IntArraySettings(){
		Preference.IntArray.EQUALIZER_LEVEL.set(context, 0, 10);
		Preference.IntArray.EQUALIZER_LEVEL.set(context, 1, 20);
		Preference.IntArray.EQUALIZER_LEVEL.set(context, 2, -1);

		assertEquals(Preference.IntArray.EQUALIZER_LEVEL.get(context, 0), 10);
		assertEquals(Preference.IntArray.EQUALIZER_LEVEL.get(context, 1), 20);
		assertEquals(Preference.IntArray.EQUALIZER_LEVEL.get(context, 2), -1);
	}
}
