package jp.blanktar.ruumusic.client.preference


import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.client.main.MainActivity
import jp.blanktar.ruumusic.util.Preference
import kotlinx.android.synthetic.main.activity_preference.*


fun bindPreferenceOnOff(switch: android.support.v7.widget.SwitchCompat, pref: Preference.BooleanPreferenceHandler, receiver: (Boolean) -> Unit) {
    receiver(pref.get())
    switch.isChecked = pref.get()

    switch.setOnCheckedChangeListener { _, checked -> pref.set(checked) }
    pref.setOnChangeListener { receiver(pref.get()) }
}


fun bindSeekBarPreference(bar: android.widget.SeekBar, pref: Preference.IntPreferenceHandler, callback: ((Int) -> Unit)? = null) {
    bar.progress = pref.get()

    callback?.invoke(pref.get())

    bar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(b: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
            pref.set(progress)

            callback?.invoke(progress)
        }

        override fun onStartTrackingTouch(p0: android.widget.SeekBar?) {}
        override fun onStopTrackingTouch(p0: android.widget.SeekBar?) {}
    })

    pref.setOnChangeListener {
        bar.progress = pref.get()
    }
}


fun bindSeekBarPreference(bar: android.widget.SeekBar, pref: Preference.ShortPreferenceHandler, callback: ((Short) -> Unit)? = null) {
    bar.progress = pref.get().toInt()

    callback?.invoke(pref.get())

    bar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(b: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
            pref.set(progress.toShort())

            callback?.invoke(progress.toShort())
        }

        override fun onStartTrackingTouch(p0: android.widget.SeekBar?) {}
        override fun onStopTrackingTouch(p0: android.widget.SeekBar?) {}
    })

    pref.setOnChangeListener {
        bar.progress = pref.get().toInt()
    }
}


fun <T> bindSpinnerPreference(spinner: android.widget.Spinner, pref: Preference.PreferenceHandler<T>, values: List<T>) {
    spinner.setSelection(values.indexOf(pref.get()))

    spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long){
            pref.set(values[position])
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
    }

    pref.setOnChangeListener {
        spinner.setSelection(values.indexOf(pref.get()))
    }
}


class PreferenceActivity : AppCompatActivity() {
    private var rootDirItem: Item? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        setSupportActionBar(toolbar)
        if (Build.VERSION.SDK_INT < 24) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(!isInMultiWindowMode)
        }

        val preference = Preference(applicationContext)

        val adapter = Adapter(this)
        adapter.add(Item(getString(R.string.preference_list_sound_name), null))
        adapter.add(Item(getString(R.string.preference_list_player_name), getString(R.string.preference_list_player_description)))
        adapter.add(Item(getString(R.string.preference_list_widget_name), getString(R.string.preference_list_widget_description)))
        rootDirItem = Item(getString(R.string.preference_list_rootdir_name), preference.RootDirectory.get())
        adapter.add(rootDirItem!!)
        list.adapter = adapter

        list.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> startActivity(Intent(applicationContext, SoundPreferenceActivity::class.java))
                1 -> startActivity(Intent(applicationContext, PlayerPreferenceActivity::class.java))
                2 -> startActivity(Intent(applicationContext, WidgetPreferenceActivity::class.java))
                3 -> startActivityForResult(Intent(applicationContext, DirectorySelectActivity::class.java), 0)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == RESULT_OK) {
            Preference(applicationContext).RootDirectory.set(data.getStringExtra("directory"))
            rootDirItem?.description = data.getStringExtra("directory")
            (list.adapter as Adapter).notifyDataSetChanged()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            startActivity(Intent(applicationContext, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        supportActionBar?.setDisplayHomeAsUpEnabled(!isInMultiWindowMode)
    }


    class Item(val name: String, var description: String?)

    class Adapter(context: Context) : ArrayAdapter<Item>(context, R.layout.preference_item) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.preference_item, parent, false)

            val item = getItem(position)

            view.findViewById<TextView>(R.id.name)?.text = item.name
            if (item.description == null) {
                view.findViewById<TextView>(R.id.description)?.visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.description)?.text = item.description
                view.findViewById<TextView>(R.id.description)?.visibility = View.VISIBLE
            }

            return view
        }
    }
}
