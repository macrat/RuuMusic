package jp.blanktar.ruumusic;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;


public class AudioPreferenceActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
	public final static String PREFERENCE_PREFIX = "audio_";

	public final static String PREFERENCE_BASSBOOST_ENABLED = PREFERENCE_PREFIX + "bassboost_enabled";
	public final static String PREFERENCE_BASSBOOST_LEVEL = PREFERENCE_PREFIX + "bassboost_level";

	public final static String PREFERENCE_REVERB_ENABLED = PREFERENCE_PREFIX + "reverb_enabled";
	public final static String PREFERENCE_REVERB_TYPE = PREFERENCE_PREFIX + "reverb_type";

	public final static String PREFERENCE_LOUDNESS_ENABLED = PREFERENCE_PREFIX + "loudness_enabled";
	public final static String PREFERENCE_LOUDNESS_LEVEL = PREFERENCE_PREFIX + "loudness_level";

	public final static String PREFERENCE_EQUALIZER_ENABLED = PREFERENCE_PREFIX + "equalizer_enabled";
	public final static String PREFERENCE_EQUALIZER_VALUE = PREFERENCE_PREFIX + "equalizer_value_";


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_preference);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);


		((SwitchCompat)findViewById(R.id.bass_boost_switch)).setOnCheckedChangeListener(this);
		((SwitchCompat)findViewById(R.id.bass_boost_switch)).setChecked(getPreference(PREFERENCE_BASSBOOST_ENABLED, false));

		findViewById(R.id.bass_boost_level).setEnabled(getPreference(PREFERENCE_BASSBOOST_ENABLED, false));
		((SeekBar)findViewById(R.id.bass_boost_level)).setProgress(getPreference(PREFERENCE_BASSBOOST_LEVEL, 0));
		((SeekBar)findViewById(R.id.bass_boost_level)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(@Nullable SeekBar seekBar, int progress, boolean fromUser){
				putPreference(PREFERENCE_BASSBOOST_LEVEL, progress);
			}

			@Override
			public void onStartTrackingTouch(@Nullable SeekBar seekBar){
			}

			@Override
			public void onStopTrackingTouch(@Nullable SeekBar seekBar){
			}
		});


		((SwitchCompat)findViewById(R.id.reverb_switch)).setOnCheckedChangeListener(this);
		((SwitchCompat)findViewById(R.id.reverb_switch)).setChecked(getPreference(PREFERENCE_REVERB_ENABLED, false));
		findViewById(R.id.reverb_spinner).setEnabled(getPreference(PREFERENCE_REVERB_ENABLED, false));

		ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.reverb_options, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Spinner spinner = (Spinner)findViewById(R.id.reverb_spinner);
		spinner.setAdapter(adapter);
		switch(getPreference(PREFERENCE_REVERB_TYPE, 0)){
			case PresetReverb.PRESET_LARGEHALL:
				spinner.setSelection(0);
				break;
			case PresetReverb.PRESET_MEDIUMHALL:
				spinner.setSelection(1);
				break;
			case PresetReverb.PRESET_LARGEROOM:
				spinner.setSelection(2);
				break;
			case PresetReverb.PRESET_MEDIUMROOM:
				spinner.setSelection(3);
				break;
			case PresetReverb.PRESET_SMALLROOM:
				spinner.setSelection(4);
				break;
		}
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(@Nullable AdapterView<?> parent, @Nullable View view, int position, long id){
				switch(position){
					case 0:
						putPreference(PREFERENCE_REVERB_TYPE, PresetReverb.PRESET_LARGEHALL);
						break;
					case 1:
						putPreference(PREFERENCE_REVERB_TYPE, PresetReverb.PRESET_MEDIUMHALL);
						break;
					case 2:
						putPreference(PREFERENCE_REVERB_TYPE, PresetReverb.PRESET_LARGEROOM);
						break;
					case 3:
						putPreference(PREFERENCE_REVERB_TYPE, PresetReverb.PRESET_MEDIUMROOM);
						break;
					case 4:
						putPreference(PREFERENCE_REVERB_TYPE, PresetReverb.PRESET_SMALLROOM);
						break;
				}
			}

			@Override
			public void onNothingSelected(@Nullable AdapterView<?> parent) {
			}
		});


		if(Build.VERSION.SDK_INT < 19){
			findViewById(R.id.loudness_wrapper).setVisibility(View.GONE);
		}else{
			((SwitchCompat)findViewById(R.id.loudness_switch)).setOnCheckedChangeListener(this);
			((SwitchCompat)findViewById(R.id.loudness_switch)).setChecked(getPreference(PREFERENCE_LOUDNESS_ENABLED, false));
			findViewById(R.id.loudness_level).setEnabled(getPreference(PREFERENCE_LOUDNESS_ENABLED, false));
			
			((SeekBar)findViewById(R.id.loudness_level)).setProgress(getPreference(PREFERENCE_LOUDNESS_LEVEL, 0));
			((SeekBar)findViewById(R.id.loudness_level)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
				@Override
				public void onProgressChanged(@Nullable SeekBar seekBar, int progress, boolean fromUser){
					putPreference(PREFERENCE_LOUDNESS_LEVEL, progress);
				}

				@Override
				public void onStartTrackingTouch(@Nullable SeekBar seekBar){
				}

				@Override
				public void onStopTrackingTouch(@Nullable SeekBar seekBar){
				}
			});
		}


		((SwitchCompat)findViewById(R.id.equalizer_switch)).setOnCheckedChangeListener(this);
		((SwitchCompat)findViewById(R.id.equalizer_switch)).setChecked(getPreference(PREFERENCE_EQUALIZER_ENABLED, false));

		Equalizer eq = new Equalizer(0, (new MediaPlayer()).getAudioSessionId());
		final int equalizer_min = eq.getBandLevelRange()[0];
		final int equalizer_max = eq.getBandLevelRange()[1];
		for(short i=0; i<eq.getNumberOfBands(); i++){
			ViewGroup table = (ViewGroup)getLayoutInflater().inflate(R.layout.equalizer_preference_row, (ViewGroup)findViewById(R.id.equalizer_container));
			ViewGroup newview = (ViewGroup)table.getChildAt(table.getChildCount()-1);
			((TextView)newview.findViewById(R.id.equalizer_freq)).setText(eq.getCenterFreq(i)/1000 + "Hz");

			SeekBar seekBar = (SeekBar)newview.findViewById(R.id.equalizer_bar);
			seekBar.setMax(equalizer_max - equalizer_min);
			seekBar.setProgress(getPreference(PREFERENCE_EQUALIZER_VALUE + i, (equalizer_max + equalizer_min)/2) - equalizer_min);
			seekBar.setOnSeekBarChangeListener((new SeekBar.OnSeekBarChangeListener(){
				private int id;

				@Override
				public void onProgressChanged(@Nullable SeekBar seekBar, int progress, boolean fromUser){
					putPreference(PREFERENCE_EQUALIZER_VALUE + id, progress + equalizer_min);
				}

				@Override
				public void onStartTrackingTouch(@Nullable SeekBar seekBar){
				}

				@Override
				public void onStopTrackingTouch(@Nullable SeekBar seekBar){
				}

				SeekBar.OnSeekBarChangeListener setId(int id){
					this.id = id;
					return this;
				}
			}).setId(i));
		}
		eq.release();

		setEqualizerEnabled(getPreference(PREFERENCE_EQUALIZER_ENABLED, false));
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus){
		super.onWindowFocusChanged(hasFocus);
		int maxWidth = (int)(600 * getResources().getDisplayMetrics().density);
		int currentWidth = findViewById(R.id.bass_boost_level).getWidth();
		if(currentWidth > maxWidth){
			findViewById(R.id.bass_boost_level).setPadding(currentWidth - maxWidth, 0, 0, 0);
			findViewById(R.id.reverb_spinner).setPadding(currentWidth - maxWidth, 0, 0, 0);
			findViewById(R.id.loudness_level).setPadding(currentWidth - maxWidth, 0, 0, 0);
			findViewById(R.id.equalizer_container).setPadding(currentWidth - maxWidth, 0, 0, 0);
		}
	}

	@Override
	public void onCheckedChanged(@NonNull CompoundButton button, boolean isChecked){
		switch(button.getId()){
			case R.id.bass_boost_switch:
				findViewById(R.id.bass_boost_level).setEnabled(isChecked);
				putPreference(PREFERENCE_BASSBOOST_ENABLED, isChecked);
				break;
			case R.id.reverb_switch:
				findViewById(R.id.reverb_spinner).setEnabled(isChecked);
				putPreference(PREFERENCE_REVERB_ENABLED, isChecked);
				break;
			case R.id.loudness_switch:
				findViewById(R.id.loudness_level).setEnabled(isChecked);
				putPreference(PREFERENCE_LOUDNESS_ENABLED, isChecked);
				break;
			case R.id.equalizer_switch:
				setEqualizerEnabled(isChecked);
				putPreference(PREFERENCE_EQUALIZER_ENABLED, isChecked);
				break;
		}
	}

	private void setEqualizerEnabled(boolean enabled){
		ViewGroup container = (ViewGroup)findViewById(R.id.equalizer_container);
		for(int i=0; i<container.getChildCount(); i++){
			ViewGroup row = (ViewGroup)container.getChildAt(i);
			for(int j=0; j<row.getChildCount(); j++){
				row.getChildAt(j).setEnabled(enabled);
			}
			row.setEnabled(enabled);
		}
		container.setEnabled(enabled);
	}

	private void putPreference(@NonNull String key, boolean value){
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
				.putBoolean(key, value)
				.apply();
	}

	private void putPreference(@NonNull String key, int value){
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
				.putInt(key, value)
				.apply();
	}

	private boolean getPreference(@NonNull String key, boolean default_value){
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(key, default_value);
	}

	private int getPreference(@NonNull String key, int default_value){
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(key, default_value);
	}
}