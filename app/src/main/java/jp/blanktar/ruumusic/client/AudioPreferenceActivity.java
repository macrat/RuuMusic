package jp.blanktar.ruumusic.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.os.Build;
import android.os.Bundle;
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

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.util.Preference;


public class AudioPreferenceActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_preference);
		if(getSupportActionBar() != null){
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}


		((SwitchCompat)findViewById(R.id.bass_boost_switch)).setOnCheckedChangeListener(this);
		((SwitchCompat)findViewById(R.id.bass_boost_switch)).setChecked(Preference.Bool.BASSBOOST_ENABLED.get(getApplicationContext()));

		findViewById(R.id.bass_boost_level).setEnabled(Preference.Bool.BASSBOOST_ENABLED.get(getApplicationContext()));
		((SeekBar)findViewById(R.id.bass_boost_level)).setProgress(Preference.Int.BASSBOOST_LEVEL.get(getApplicationContext()));
		((SeekBar)findViewById(R.id.bass_boost_level)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(@Nullable SeekBar seekBar, int progress, boolean fromUser){
				Preference.Int.BASSBOOST_LEVEL.set(getApplicationContext(), progress);
			}

			@Override
			public void onStartTrackingTouch(@Nullable SeekBar seekBar){
			}

			@Override
			public void onStopTrackingTouch(@Nullable SeekBar seekBar){
			}
		});


		((SwitchCompat)findViewById(R.id.reverb_switch)).setOnCheckedChangeListener(this);
		((SwitchCompat)findViewById(R.id.reverb_switch)).setChecked(Preference.Bool.REVERB_ENABLED.get(getApplicationContext()));
		findViewById(R.id.reverb_spinner).setEnabled(Preference.Bool.REVERB_ENABLED.get(getApplicationContext()));

		ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.reverb_options, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Spinner spinner = (Spinner)findViewById(R.id.reverb_spinner);
		spinner.setAdapter(adapter);
		switch(Preference.Int.REVERB_TYPE.get(getApplicationContext())){
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
				int type = -1;
				switch(position){
					case 0:
						type = PresetReverb.PRESET_LARGEHALL;
						break;
					case 1:
						type = PresetReverb.PRESET_MEDIUMHALL;
						break;
					case 2:
						type = PresetReverb.PRESET_LARGEROOM;
						break;
					case 3:
						type = PresetReverb.PRESET_MEDIUMROOM;
						break;
					case 4:
						type = PresetReverb.PRESET_SMALLROOM;
						break;
				}
				if(type >= 0){
					Preference.Int.REVERB_TYPE.set(getApplicationContext(), type);
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
			((SwitchCompat)findViewById(R.id.loudness_switch)).setChecked(Preference.Bool.LOUDNESS_ENABLED.get(getApplicationContext()));
			findViewById(R.id.loudness_level).setEnabled(Preference.Bool.LOUDNESS_ENABLED.get(getApplicationContext()));

			((SeekBar)findViewById(R.id.loudness_level)).setProgress(Preference.Int.LOUDNESS_LEVEL.get(getApplicationContext()));
			((SeekBar)findViewById(R.id.loudness_level)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
				@Override
				public void onProgressChanged(@Nullable SeekBar seekBar, int progress, boolean fromUser){
					Preference.Int.LOUDNESS_LEVEL.set(getApplicationContext(), progress);
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
		((SwitchCompat)findViewById(R.id.equalizer_switch)).setChecked(Preference.Bool.EQUALIZER_ENABLED.get(getApplicationContext()));

		Equalizer eq = new Equalizer(0, (new MediaPlayer()).getAudioSessionId());
		final int equalizer_min = eq.getBandLevelRange()[0];
		final int equalizer_max = eq.getBandLevelRange()[1];
		for(short i=0; i<eq.getNumberOfBands(); i++){
			ViewGroup table = (ViewGroup)getLayoutInflater().inflate(R.layout.equalizer_preference_row, (ViewGroup)findViewById(R.id.equalizer_container));
			ViewGroup newview = (ViewGroup)table.getChildAt(table.getChildCount()-1);
			((TextView)newview.findViewById(R.id.equalizer_freq)).setText(eq.getCenterFreq(i)/1000 + "Hz");

			SeekBar seekBar = (SeekBar)newview.findViewById(R.id.equalizer_bar);
			seekBar.setMax(equalizer_max - equalizer_min);
			seekBar.setProgress(Preference.IntArray.EQUALIZER_LEVEL.get(getApplicationContext(), i) - equalizer_min);
			seekBar.setOnSeekBarChangeListener((new SeekBar.OnSeekBarChangeListener(){
				private int id;

				@Override
				public void onProgressChanged(@Nullable SeekBar seekBar, int progress, boolean fromUser){
					Preference.IntArray.EQUALIZER_LEVEL.set(getApplicationContext(), id, progress + equalizer_min);
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

		setEqualizerEnabled(Preference.Bool.EQUALIZER_ENABLED.get(getApplicationContext()));
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
				Preference.Bool.BASSBOOST_ENABLED.set(getApplicationContext(), isChecked);
				break;
			case R.id.reverb_switch:
				findViewById(R.id.reverb_spinner).setEnabled(isChecked);
				Preference.Bool.REVERB_ENABLED.set(getApplicationContext(), isChecked);
				break;
			case R.id.loudness_switch:
				findViewById(R.id.loudness_level).setEnabled(isChecked);
				Preference.Bool.LOUDNESS_ENABLED.set(getApplicationContext(), isChecked);
				break;
			case R.id.equalizer_switch:
				setEqualizerEnabled(isChecked);
				Preference.Bool.EQUALIZER_ENABLED.set(getApplicationContext(), isChecked);
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
}

