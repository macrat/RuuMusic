package jp.blanktar.ruumusic.wear

import android.app.Fragment
import android.os.Bundle
import android.support.wearable.view.CurvedChildLayoutManager
import android.support.wearable.view.WearableRecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.fragment_playlist.*


class PlaylistFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.layoutManager = CurvedChildLayoutManager(getContext())

        list.adapter = RecyclerAdapter(arrayOf<String>("/this/is/test/",
                "/path/to/song/",
                "/this/is/test/",
                "/path/to/song/",
                "this is song",
                "song file name",
                "this is song",
                "song file name"))
    }
}
