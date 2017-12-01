package ru.yourok.dwl.manager

import android.app.Activity
import android.support.v7.app.AlertDialog
import ru.yourok.dwl.downloader.Downloader
import ru.yourok.dwl.downloader.State
import ru.yourok.dwl.list.List
import ru.yourok.dwl.settings.Settings
import ru.yourok.dwl.storage.Document
import ru.yourok.dwl.utils.Utils
import ru.yourok.m3u8loader.R
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread


object Manager {
    @Volatile private var loaderList: MutableList<Downloader> = mutableListOf()
    @Volatile private var queueList: MutableList<Int> = mutableListOf()

    init {
        val list = Utils.loadLists()
        if (list != null)
            list.forEach {
                loaderList.add(Downloader(it))
            }
    }

    fun getLoader(index: Int): Downloader? {
        if (index in 0 until loaderList.size)
            return loaderList[index]
        return null
    }

    fun findLoader(list: List): Downloader? {
        loaderList.forEach {
            if (it.list.url + it.list.info.title == list.url + list.info.title)
                return it
        }
        return null
    }

    fun getLoadersSize(): Int {
        return loaderList.size
    }

    fun getLoaderStat(i: Int): State? {
        return getLoader(i)?.getState()
    }

    fun addList(list: MutableList<List>) {
        synchronized(loaderList) {
            list.forEach {
                var isFindUrl = false
                loaderList.forEach { item ->
                    if (it.url == item.list.url)
                        throw IOException(Settings.context!!.getString(R.string.error_same_url))
                }
                if (!isFindUrl) {
                    //find equal url
                    val flist = loaderList.find { downloader ->
                        downloader.getState().name == it.info.title
                    }
                    //replace urls if find eq
                    if (flist != null) {
                        flist.list.url = it.url
                        if (flist.list.items.size != it.items.size) {
                            it.items = flist.list.items
                        } else
                            flist.list.items.forEachIndexed { index, item ->
                                if (index < it.items.size)
                                    item.url = it.items[index].url
                            }

                    } else
                        loaderList.add(Downloader(it))
                }
            }
            list.forEach { Utils.saveList(it) }
        }
    }

    fun removes(indexes: Set<Int>, activity: Activity) {
        synchronized(loaderList) {
            var isFile = false
            indexes.forEach {
                if (File(loaderList[it].list.filePath).exists()) {
                    isFile = true
                    return@forEach
                }
            }
            if (isFile) {
                with(activity) {
                    AlertDialog.Builder(activity)
                            .setTitle(this@with.getString(R.string.delete_all_items) + "?")
                            .setPositiveButton(R.string.delete_with_files) { _, _ ->
                                val delLoader: MutableList<Downloader> = mutableListOf()
                                indexes.forEach {
                                    delLoader.add(loaderList[it])
                                    loaderList[it].stop()
                                }
                                delLoader.forEach {
                                    it.waitEnd()
                                    Utils.removeList(it.list)
                                    loaderList.remove(it)
                                    val f = Document.openFile(it.list.filePath)
                                    if (f != null) f.delete()
                                    if (it.list.subsUrl.isNotEmpty())
                                        Document.openFile(File(File(it.list.filePath).parent, it.list.info.title + ".srt").canonicalPath)?.delete()
                                }
                            }
                            .setNegativeButton(R.string.remove_from_list) { _, _ ->
                                val delLoader: MutableList<Downloader> = mutableListOf()
                                indexes.forEach {
                                    delLoader.add(loaderList[it])
                                    loaderList[it].stop()
                                }
                                delLoader.forEach {
                                    it.waitEnd()
                                    Utils.removeList(it.list)
                                    loaderList.remove(it)
                                }
                            }
                            .setNeutralButton(" ", null)
                            .show()
                }
            } else {
                val delLoader: MutableList<Downloader> = mutableListOf()
                indexes.forEach {
                    delLoader.add(loaderList[it])
                    loaderList[it].stop()
                }
                delLoader.forEach {
                    it.waitEnd()
                    Utils.removeList(it.list)
                    loaderList.remove(it)
                }
            }
        }
    }

    fun removeAll(activity: Activity) {
        synchronized(loaderList) {
            var isFile = false
            loaderList.forEach {
                if (File(it.list.filePath).exists()) {
                    isFile = true
                    return@forEach
                }
            }
            if (isFile) {
                with(activity) {
                    AlertDialog.Builder(activity)
                            .setTitle(this@with.getString(R.string.delete_all_items) + "?")
                            .setPositiveButton(R.string.delete_with_files) { _, _ ->
                                stopAll()
                                loaderList.forEach {
                                    it.waitEnd()
                                    Utils.removeList(it.list)
                                    val f = Document.openFile(it.list.filePath)
                                    if (f != null) f.delete()
                                    if (it.list.subsUrl.isNotEmpty())
                                        Document.openFile(File(File(it.list.filePath).parent, it.list.info.title + ".srt").canonicalPath)?.delete()
                                }
                                loaderList.clear()
                            }
                            .setNegativeButton(R.string.remove_from_list) { _, _ ->
                                stopAll()
                                loaderList.forEach {
                                    it.waitEnd()
                                    Utils.removeList(it.list)
                                }
                                loaderList.clear()
                            }
                            .setNeutralButton(" ", null)
                            .show()
                }
            } else {
                stopAll()
                loaderList.forEach {
                    it.waitEnd()
                    Utils.removeList(it.list)
                }
                loaderList.clear()
            }
        }
    }

    /////////Queue funcs
    private val lockQueue: Any = Any()
    private var loading: Boolean = false
    private var currentLoader = -1

    fun isLoading() = loading

    fun load(index: Int) {
        if (index in 0 until loaderList.size) {
            if (loaderList[index].isComplete())
                return
            if (!inQueue(index))
                queueList.add(index)
            synchronized(lockQueue) {
                thread { startLoading() }
            }
        }
    }

    fun loadAll() {
        loaderList.forEachIndexed { index, downloader ->
            if (!downloader.isComplete() && !inQueue(index))
                queueList.add(index)
        }
        synchronized(lockQueue) {
            thread { startLoading() }
        }
    }

    fun stop(index: Int) {
        loaderList[index].stop()
    }

    fun stopAll() {
        synchronized(lockQueue) {
            queueList.clear()
            loaderList.forEach { it.stop() }
            currentLoader = -1
        }
    }

    private fun startLoading() {
        synchronized(lockQueue) {
            if (queueList.isEmpty())
                return
            if (loading)
                return
            loading = true
        }
        thread {
            LoaderService.start()
            currentLoader = -1
            while (queueList.size > 0) {
                if (!loading)
                    break
                synchronized(lockQueue) {
                    currentLoader = queueList[0]
                    thread { loaderList[currentLoader].load() }
                    queueList.removeAt(0)
                }
                Thread.sleep(100)
                loaderList[currentLoader].waitEnd()
                if (currentLoader != -1 && !loaderList[currentLoader].isComplete()) {
                    loading = false
                    queueList.clear()
                }
            }
            loading = false
            currentLoader = -1
            LoaderService.stop()
        }
    }

    fun inQueue(index: Int): Boolean {
        synchronized(lockQueue) {
            if (index == currentLoader)
                return true
            if (index in 0 until loaderList.size)
                queueList.forEach { if (it == index) return true }
            return false
        }
    }

    fun getCurrentLoader(): Int {
        return currentLoader
    }
}