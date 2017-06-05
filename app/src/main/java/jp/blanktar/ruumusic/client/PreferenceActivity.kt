package jp.blanktar.ruumusic.client

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar

import jp.blanktar.ruumusic.R


class PreferenceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val viewPager = findViewById(R.id.viewpager) as ViewPager
        viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment? {
                return when (position) {
                    0 -> SoundPreferenceFragment()
                    1 -> PlayerPreferenceFragment()
                    2 -> WidgetPreferenceFragment()
                    else -> null
                }
            }

            override fun getCount(): Int {
                return 3
            }

            override fun getPageTitle(position: Int): CharSequence {
                return getString(listOf(R.string.title_sound_preference,
                                        R.string.title_player_preference,
                                        R.string.title_widget_preference)[position])
            }
        }
        (findViewById(R.id.tablayout) as TabLayout).setupWithViewPager(viewPager)
    }
}

