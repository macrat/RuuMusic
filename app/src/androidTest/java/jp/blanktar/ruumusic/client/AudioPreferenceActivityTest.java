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


public class AudioPreferenceActivityTest extends TestBase{
	private AudioPreferenceActivity activity;
	
    @Rule
    public ActivityTestRule<AudioPreferenceActivity> activityRule = new ActivityTestRule<>(AudioPreferenceActivity.class);

	@Before
	public void setUp(){
		activity = activityRule.getActivity();

		if(Preference.Bool.BASSBOOST_ENABLED.get(context)){
			onView(withId(R.id.bass_boost_switch)).perform(click());
		}
		if(Preference.Bool.REVERB_ENABLED.get(context)){
			onView(withId(R.id.reverb_switch)).perform(click());
		}
		if(Preference.Bool.LOUDNESS_ENABLED.get(context)){
			onView(withId(R.id.loudness_switch)).perform(click());
		}
		if(Preference.Bool.EQUALIZER_ENABLED.get(context)){
			onView(withId(R.id.equalizer_switch)).perform(click());
		}
	}

	@Test
	public void enable_disable(){
		onView(withId(R.id.bass_boost_switch)).perform(click());
		assertTrue(Preference.Bool.BASSBOOST_ENABLED.get(context));
		assertTrue(activity.findViewById(R.id.bass_boost_level).isEnabled());

		onView(withId(R.id.bass_boost_switch)).perform(click());
		assertFalse(Preference.Bool.BASSBOOST_ENABLED.get(context));
		assertFalse(activity.findViewById(R.id.bass_boost_level).isEnabled());


		onView(withId(R.id.reverb_switch)).perform(click());
		assertTrue(Preference.Bool.REVERB_ENABLED.get(context));
		assertTrue(activity.findViewById(R.id.reverb_spinner).isEnabled());

		onView(withId(R.id.reverb_switch)).perform(click());
		assertFalse(Preference.Bool.REVERB_ENABLED.get(context));
		assertFalse(activity.findViewById(R.id.reverb_spinner).isEnabled());


		onView(withId(R.id.loudness_switch)).perform(click());
		assertTrue(Preference.Bool.LOUDNESS_ENABLED.get(context));
		assertTrue(activity.findViewById(R.id.loudness_level).isEnabled());

		onView(withId(R.id.loudness_switch)).perform(click());
		assertFalse(Preference.Bool.LOUDNESS_ENABLED.get(context));
		assertFalse(activity.findViewById(R.id.loudness_level).isEnabled());


		onView(withId(R.id.equalizer_switch)).perform(click());
		assertTrue(Preference.Bool.EQUALIZER_ENABLED.get(context));
		assertTrue(activity.findViewById(R.id.equalizer_container).isEnabled());

		onView(withId(R.id.equalizer_switch)).perform(click());
		assertFalse(Preference.Bool.EQUALIZER_ENABLED.get(context));
		assertFalse(activity.findViewById(R.id.equalizer_container).isEnabled());
	}

	@Test
	public void reverb(){
		onView(withId(R.id.reverb_switch)).perform(click());

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Large Hall"))).perform(click());
		assertEquals(Preference.Int.REVERB_TYPE.get(context), PresetReverb.PRESET_LARGEHALL);

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Medium Hall"))).perform(click());
		assertEquals(Preference.Int.REVERB_TYPE.get(context), PresetReverb.PRESET_MEDIUMHALL);

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Large Room"))).perform(click());
		assertEquals(Preference.Int.REVERB_TYPE.get(context), PresetReverb.PRESET_LARGEROOM);

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Medium Room"))).perform(click());
		assertEquals(Preference.Int.REVERB_TYPE.get(context), PresetReverb.PRESET_MEDIUMROOM);

		onView(withId(R.id.reverb_spinner)).perform(click());
		onData(allOf(is("Small Room"))).perform(click());
		assertEquals(Preference.Int.REVERB_TYPE.get(context), PresetReverb.PRESET_SMALLROOM);
	}
}