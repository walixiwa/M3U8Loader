package ru.yourok.m3u8loader.activitys

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ru.yourok.m3u8loader.R
import ru.yourok.m3u8loader.fragments.loaderList.LoaderListFragment
import ru.yourok.m3u8loader.navigationBar.NavigationBar
import ru.yourok.m3u8loader.theme.Theme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.set(this)
        setContentView(R.layout.activity_main)
        NavigationBar.setup(this)
        if (supportFragmentManager.backStackEntryCount == 0)
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentContainer, LoaderListFragment.newInstance())
                    .commit()
    }
}