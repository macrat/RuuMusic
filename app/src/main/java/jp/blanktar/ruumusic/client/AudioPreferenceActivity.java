package jp.blanktar.ruumusic.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

import jp.blanktar.ruumusic.R;
import jp.blanktar.ruumusic.util.Preference;
import jp.blanktar.ruumusic.service.RuuService;


public class AudioPreferenceActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, SharedPreferences.OnSharedPreferenceChangeListener{
	private int equalizer_min;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_preference);
		if(getSupportActionBar() != null){
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		registerReceiver(broadcastReceiver, new IntentFilter(RuuService.ACTION_EFFECT_INFO));
		startService((new Intent(getApplicationContext(), RuuService.class)).setAction(RuuService.ACTION_REQUEST_EFFECT_INFO));

		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);

		initBassBoost();
		initReverb();
		initLoudnessEnhancer();
		initEqualizer();
	}

	@Override
	protected void onDestroy(){
		unregisterReceiver(broadcastReceiver);
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);

		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged(@NonNull SharedPreferences preference, @NonNull String key){
		if(key.startsWith(Preference.AUDIO_PREFIX)){
			updateBassBoost();
			updateReverb();
			updateLoudnessEnhancer();
			updateEqualizer();
		}
	}

	private void initBassBoost(){
		((SwitchCompat)findViewById(R.id.bass_boost_switch)).setOnCheckedChangeListener(this);

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

		updateBassBoost();
	}

	private void updateBassBoost(){
		((SwitchCompat)findViewById(R.id.bass_boost_switch)).setChecked(Preference.Bool.BASSBOOST_ENABLED.get(getApplicationContext()));
		findViewById(R.id.bass_boost_level).setEnabled(Preference.Bool.BASSBOOST_ENABLED.get(getApplicationContext()));
		((SeekBar)findViewById(R.id.bass_boost_level)).setProgress(Preference.Int.BASSBOOST_LEVEL.get(getApplicationContext()));
	}

	private void initReverb(){
		ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.reverb_options, R.layout.spinner_item);
		adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

		((SwitchCompat)findViewById(R.id.reverb_switch)).setOnCheckedChangeListener(this);

		Spinner spinner = (Spinner)findViewById(R.id.reverb_spinner);
		spinner.setAdapter(adapter);
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
			public void onNothingSelected(@Nullable AdapterView<?> parent){
			}
		});

		updateReverb();
	}

	private void updateReverb(){
		((SwitchCompat)findViewById(R.id.reverb_switch)).setChecked(Preference.Bool.REVERB_ENABLED.get(getApplicationContext()));
		findViewById(R.id.reverb_spinner).setEnabled(Preference.Bool.REVERB_ENABLED.get(getApplicationContext()));

		Spinner spinner = (Spinner)findViewById(R.id.reverb_spinner);
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
	}

	private void initLoudnessEnhancer(){
		if(Build.VERSION.SDK_INT < 19){
			findViewById(R.id.loudness_wrapper).setVisibility(View.GONE);
			return;
		}

		((SwitchCompat)findViewById(R.id.loudness_switch)).setOnCheckedChangeListener(this);

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

		updateLoudnessEnhancer();
	}

	private void updateLoudnessEnhancer(){
		if(Build.VERSION.SDK_INT < 19){
			return;
		}

		((SwitchCompat)findViewById(R.id.loudness_switch)).setChecked(Preference.Bool.LOUDNESS_ENABLED.get(getApplicationContext()));
		findViewById(R.id.loudness_level).setEnabled(Preference.Bool.LOUDNESS_ENABLED.get(getApplicationContext()));
		((SeekBar)findViewById(R.id.loudness_level)).setProgress(Preference.Int.LOUDNESS_LEVEL.get(getApplicationContext()));
	}

	private void initEqualizer(){
		findViewById(R.id.equalizer_switch).setEnabled(false);

		((SwitchCompat)findViewById(R.id.equalizer_switch)).setOnCheckedChangeListener(this);

		updateEqualizer();
	}

	private void updateEqualizer(){
		boolean enabled = Preference.Bool.EQUALIZER_ENABLED.get(getApplicationContext());

		((SwitchCompat)findViewById(R.id.equalizer_switch)).setChecked(enabled);

		((Spinner)findViewById(R.id.equalizer_spinner)).setSelection(Preference.Int.EQUALIZER_PRESET.get(getApplicationContext()) + 1);
		findViewById(R.id.equalizer_spinner).setEnabled(enabled);

		ViewGroup container = (ViewGroup)findViewById(R.id.equalizer_container);
		for(int i=0; i<container.getChildCount(); i++){
			ViewGroup row = (ViewGroup)container.getChildAt(i);
			((SeekBar)row.findViewById(R.id.equalizer_bar)).setProgress(Preference.IntArray.EQUALIZER_LEVEL.get(getApplicationContext(), i) - equalizer_min);
			for(int j=0; j<row.getChildCount(); j++){
				row.getChildAt(j).setEnabled(enabled);
			}
			row.setEnabled(enabled);
		}
		container.setEnabled(enabled);
	}

	private void setEqualizerInfo(@NonNull Intent intent){
		findViewById(R.id.equalizer_switch).setEnabled(true);

		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item);
		adapter.add("Custom");
		for(String name: intent.getStringArrayExtra("equalizer_presets")){
			adapter.add(name);
		}
		Spinner spinner = (Spinner)findViewById(R.id.equalizer_spinner);
		spinner.setAdapter(adapter);
		adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(@Nullable AdapterView<?> parent, @Nullable View view, int position, long id){
				Preference.Int.EQUALIZER_PRESET.set(getApplicationContext(), position - 1);
			}

			@Override
			public void onNothingSelected(@Nullable AdapterView<?> parent){
			}
		});

		equalizer_min = intent.getShortExtra("equalizer_min", (short)0);
		final int equalizer_max = intent.getShortExtra("equalizer_max", (short)0);
		final int[] freqs = intent.getIntArrayExtra("equalizer_freqs");
		for(int i=0; i<freqs.length; i++){
			ViewGroup table = (ViewGroup)getLayoutInflater().inflate(R.layout.equalizer_preference_row, (ViewGroup)findViewById(R.id.equalizer_container));
			ViewGroup newview = (ViewGroup)table.getChildAt(table.getChildCount()-1);
			((TextView)newview.findViewById(R.id.equalizer_freq)).setText(freqs[i]/1000 + "Hz");

			SeekBar seekBar = (SeekBar)newview.findViewById(R.id.equalizer_bar);
			seekBar.setMax(equalizer_max - equalizer_min);
			seekBar.setOnSeekBarChangeListener((new SeekBar.OnSeekBarChangeListener(){
				private int id;

				@Override
				public void onProgressChanged(@Nullable SeekBar seekBar, int progress, boolean fromUser){
					if(fromUser){
						Preference.Int.EQUALIZER_PRESET.set(getApplicationContext(), -1);
					}
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

		updateEqualizer();
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
				Preference.Bool.BASSBOOST_ENABLED.set(getApplicationContext(), isChecked);
				break;
			case R.id.reverb_switch:
				Preference.Bool.REVERB_ENABLED.set(getApplicationContext(), isChecked);
				break;
			case R.id.loudness_switch:
				Preference.Bool.LOUDNESS_ENABLED.set(getApplicationContext(), isChecked);
				break;
			case R.id.equalizer_switch:
				Preference.Bool.EQUALIZER_ENABLED.set(getApplicationContext(), isChecked);
				break;
		}
	}

	final private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(@Nullable Context context, @NonNull Intent intent){
			if(RuuService.ACTION_EFFECT_INFO.equals(intent.getAction())){
				setEqualizerInfo(intent);
			}
		}
	};
}

