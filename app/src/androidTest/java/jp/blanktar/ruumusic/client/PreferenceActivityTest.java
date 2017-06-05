package jp.blanktar.ruumusic.client;

import jp.blanktar.ruumusic.test.TestBase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

import android.support.test.rule.ActivityTestRule;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.media.audiofx.PresetReverb;

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.util.Preference;


public class PreferenceActivityTest extends TestBase{
	private PreferenceActivity activity;
	private Preference preference;
	
    @Rule
    public ActivityTestRule<PreferenceActivity> activityRule = new ActivityTestRule<>(PreferenceActivity.class);

	@Before
	public void setUp(){
		activity = activityRule.getActivity();
		preference = new Preference(context);

		if(preference.BassBoostEnabled.get()){
			onView(withId(R.id.bass_boost_switch)).perform(click());
		}
		if(preference.ReverbEnabled.get()){
			onView(withId(R.id.reverb_switch)).perform(click());
		}
		if(preference.LoudnessEnabled.get()){
			onView(withId(R.id.loudness_switch)).perform(click());
		}
		if(preference.EqualizerEnabled.get()){
			onView(withId(R.id.equalizer_switch)).perform(click());
		}
	}

	@Test
	public void enable_disable(){
		onView(withId(R.id.bass_boost_switch)).perform(click());
		assertTrue(preference.BassBoostEnabled.get());
		assertTrue(activity.findViewById(R.id.bass_boost_level).isEnabled());

		onView(withId(R.id.bass_boost_switch)).perform(click());
		assertFalse(preference.BassBoostEnabled.get());
		assertFalse(activity.findViewById(R.id.bass_boost_level).isEnabled());


		onView(withId(R.id.reverb_switch)).perform(click());
		assertTrue(preference.ReverbEnabled.get());
		assertTrue(activity.findViewById(R.id.reverb_spinner).isEnabled());

		onView(withId(R.id.reverb_switch)).perform(click());
		assertFalse(preference.ReverbEnabled.get());
		assertFalse(activity.findViewById(R.id.reverb_spinner).isEnabled());


		onView(withId(R.id.loudness_switch)).perform(click());
		assertTrue(preference.LoudnessEnabled.get());
		assertTrue(activity.findViewById(R.id.loudness_level).isEnabled());

		onView(withId(R.id.loudness_switch)).perform(click());
		assertFalse(preference.LoudnessEnabled.get());
		assertFalse(activity.findViewById(R.id.loudness_level).isEnabled());


		onView(withId(R.id.equalizer_switch)).perform(click());
		assertTrue(preference.EqualizerEnabled.get());
		assertTrue(activity.findViewById(R.id.equalizer_container).isEnabled());

		onView(withId(R.id.equalizer_switch)).perform(click());
		assertFalse(preference.EqualizerEnabled.get());
		assertFalse(activity.findViewById(R.id.equalizer_container).isEnabled());
	}

	@Test
	public void reverb(){
		onView(withId(R.id.reverb_switch)).perform(click());

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Large Hall"))).perform(click());
		assertEquals(preference.ReverbType.get().shortValue(), PresetReverb.PRESET_LARGEHALL);

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Medium Hall"))).perform(click());
		assertEquals(preference.ReverbType.get().shortValue(), PresetReverb.PRESET_MEDIUMHALL);

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Large Room"))).perform(click());
		assertEquals(preference.ReverbType.get().shortValue(), PresetReverb.PRESET_LARGEROOM);

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Medium Room"))).perform(click());
		assertEquals(preference.ReverbType.get().shortValue(), PresetReverb.PRESET_MEDIUMROOM);

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Small Room"))).perform(click());
		assertEquals(preference.ReverbType.get().shortValue(), PresetReverb.PRESET_SMALLROOM);
	}
}