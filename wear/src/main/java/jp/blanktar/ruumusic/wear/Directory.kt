package jp.blanktar.ruumusic.wear


class Directory(val path: String,
                val directories: Array<String>,
                val musics: Array<String>) {

    val all
        get() = directories.map { x -> x + "/" } .plus(musics)

    val parentPath
        get() = path.dropLast(path.length - path.dropLast(1).lastIndexOf('/') - 1)
}
