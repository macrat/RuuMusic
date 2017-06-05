package jp.blanktar.ruumusic.util;

import jp.blanktar.ruumusic.test.TestBase;
import org.junit.Test;
import static org.junit.Assert.*;


public class PreferenceTest extends TestBase{
	@Test
	public void intSettings(){
		Preference preference = new Preference(context);

		preference.LastPlayPosition.set(10);
		assertEquals(preference.LastPlayPosition.get().intValue(), 10);

		preference.LastPlayPosition.remove();
		assertEquals(preference.LastPlayPosition.get().intValue(), 0);

		preference.LastViewPage.remove();
		assertEquals(preference.LastViewPage.get().intValue(), 1);
	}

	@Test
	public void strSettings(){
		Preference preference = new Preference(context);

		preference.RecursivePath.set("test");
		assertEquals(preference.RecursivePath.get(), "test");

		preference.RecursivePath.set(null);
		assertEquals(preference.RecursivePath.get(), null);

		preference.RecursivePath.remove();
		assertEquals(preference.RecursivePath.get(), null);
	}

	@Test
	public void enumSettings(){
		Preference preference = new Preference(context);

		preference.RepeatMode.set(RepeatModeType.SINGLE);
		assertEquals(preference.RepeatMode.get(), RepeatModeType.SINGLE);

		preference.RepeatMode.remove();
		assertEquals(preference.RepeatMode.get(), RepeatModeType.OFF);
	}

	@Test
	public void boolSettings(){
		Preference preference = new Preference(context);

		preference.ShuffleMode.set(false);
		assertEquals(preference.ShuffleMode.get(), false);

		preference.ShuffleMode.set(true);
		assertEquals(preference.ShuffleMode.get(), true);
	}
	
	@Test
	public void IntArraySettings(){
		Preference preference = new Preference(context);

		preference.EqualizerLevel.set(0, 10);
		preference.EqualizerLevel.set(1, 20);
		preference.EqualizerLevel.set(2, -1);

		assertEquals(preference.EqualizerLevel.get(0), 10);
		assertEquals(preference.EqualizerLevel.get(1), 20);
		assertEquals(preference.EqualizerLevel.get(2), -1);
	}
}
