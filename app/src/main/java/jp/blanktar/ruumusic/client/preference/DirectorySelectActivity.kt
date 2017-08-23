package jp.blanktar.ruumusic.client.preference

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import jp.blanktar.ruumusic.R
import jp.blanktar.ruumusic.util.Preference
import jp.blanktar.ruumusic.util.RuuDirectory
import jp.blanktar.ruumusic.util.RuuFileBase
import kotlinx.android.synthetic.main.activity_directory_select.*


class DirectorySelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directory_select)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setSupportActionBar(toolbar)

        val preference = Preference(getApplicationContext());

        var directory = RuuDirectory.getInstance(applicationContext, "/")
        try {
            if (preference.RootDirectory.areSet()) {
                directory = RuuDirectory.rootDirectory(applicationContext)
            } else {
                directory = RuuDirectory.rootCandidate(applicationContext)
            }
        } catch (err: RuuFileBase.NotFound) {
        }

        filer.onClickListener = { file ->
            if (file.isDirectory) {
                directory = file as RuuDirectory
                filer.changeFiles(directory.children, if (directory.fullPath != "/") directory.parent else null)
            }
        }

        filer.changeFiles(directory.children, if (directory.fullPath != "/") directory.parent else null)

        select.setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra("directory", directory.fullPath))
            finish()
        }

        setResult(RESULT_CANCELED, Intent())
    }
}
