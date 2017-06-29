package jp.blanktar.ruumusic.wear

import android.app.Fragment
import android.os.Bundle
import android.support.wearable.view.CurvedChildLayoutManager
import android.support.wearable.view.WearableRecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class PlaylistFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_playlist, container, false)
        
        val recycler = view.findViewById(R.id.list) as WearableRecyclerView
        recycler.layoutManager = CurvedChildLayoutManager(getContext())

        recycler.adapter = RecyclerAdapter(arrayOf<String>("/this/is/test/",
                                                           "/path/to/song/",
                                                           "/this/is/test/",
                                                           "/path/to/song/",
                                                           "this is song",
                                                           "song file name",
                                                           "this is song",
                                                           "song file name"))

        return view
    }
}
