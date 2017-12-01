package ru.yourok.dwl.list

import ru.yourok.dwl.settings.Settings

/**
 * Created by yourok on 07.11.17.
 */
class List {
    var items: MutableList<Item> = mutableListOf()
    var url: String = ""
    var filePath: String = ""
    var info: Info = Info()
    var isConvert: Boolean = Settings.convertVideo
    var subsUrl: String = ""
}

class Info {
    var bandwidth: Int = 0
    var title: String = ""
}

class Item {
    var index: Int = -1
    var url: String = ""
    var size: Long = 0
    var duration: Float = 0F
    var isLoad: Boolean = true
    var isComplete = false
    var encData: EncKey? = null
}
