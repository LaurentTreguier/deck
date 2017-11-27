package com.github.laurenttreguier.deck

import android.app.SearchManager
import android.content.Intent
import android.graphics.Rect
import android.os.AsyncTask
import android.os.Bundle
import android.support.annotation.PluralsRes
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.GravityCompat
import android.support.v4.view.MenuItemCompat
import android.support.v4.widget.ContentLoadingProgressBar
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.github.laurenttreguier.deck.model.Card
import com.github.laurenttreguier.deck.model.CardFolder
import com.github.laurenttreguier.deck.model.Folder
import com.orm.SugarRecord
import com.orm.query.Condition
import com.orm.query.Select
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private var drawer: DrawerLayout? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private var toolbar: Toolbar? = null
    private var navigation: NavigationView? = null
    private var loader: ContentLoadingProgressBar? = null
    private var snackbar: Snackbar? = null
    private var folder: Folder? = null
    internal var refresh: SwipeRefreshLayout? = null
    internal var recycler: RecyclerView? = null
    internal var selecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawer = findViewById(R.id.drawer)
        toolbar = findViewById(R.id.activity_main_toolbar)
        navigation = findViewById(R.id.navigation)
        refresh = findViewById(R.id.activity_main_refresh)
        recycler = findViewById(R.id.activity_main_content)
        loader = findViewById(R.id.activity_main_loader)

        setSupportActionBar(toolbar)

        drawerToggle = ActionBarDrawerToggle(this, drawer, toolbar,
                android.R.string.ok, android.R.string.cancel)
        drawer?.addDrawerListener(drawerToggle!!)
        drawerToggle?.syncState()

        setupNavigation()

        refresh?.setColorSchemeColors(ResourcesCompat
                .getColor(resources, R.color.colorAccent, theme))
        refresh?.setOnRefreshListener { setupContent(intent) }

        recycler?.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?,
                                        state: RecyclerView.State?) {
                outRect?.top = resources.getDimension(R.dimen.activity_vertical_margin).toInt()
                outRect?.left = resources.getDimension(R.dimen.activity_horizontal_margin).toInt()
            }
        })

        setupContent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setupContent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(if (selecting) R.menu.main_selection else R.menu.main, menu)

        if (!selecting) {
            val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
            val searchItem = menu?.findItem(R.id.main_search)
            val searchView = searchItem?.actionView as SearchView

            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            @Suppress("DEPRECATION")
            MenuItemCompat.setOnActionExpandListener(searchItem,
                    object : MenuItemCompat.OnActionExpandListener {
                        override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                            return true
                        }

                        override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                            intent.removeExtra(SearchManager.QUERY)
                            startActivity(intent)
                            return true
                        }
                    })

            if (folder == null) {
                menu.removeItem(R.id.main_delete_folder)
            }
        } else if (folder == null) {
            menu?.removeItem(R.id.main_selection_remove)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.main_new_folder -> newFolder()

            R.id.main_delete_folder -> deleteFolder()

            R.id.main_selection_cancel -> cancelSelection()

            R.id.main_selection_add -> addSelection()

            R.id.main_selection_remove -> removeSelection()

            R.id.main_selection_delete -> deleteSelection()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawer!!.isDrawerOpen(GravityCompat.START)) {
            drawer!!.closeDrawer(GravityCompat.START)
        } else if (selecting) {
            cancelSelection()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupNavigation() {
        navigation?.menu?.clear()
        navigation?.menu
                ?.add(1, 1, Menu.NONE, R.string.activity_main_all_cards)
                ?.setIcon(R.drawable.ic_folder_special_dark)
                ?.setCheckable(true)
                ?.run {
                    if (folder == null) {
                        isChecked = true
                    }
                }

        SugarRecord.listAll(Folder::class.java)
                .sorted()
                .forEach {
                    navigation?.menu
                            ?.add(it.name)
                            ?.setIcon(R.drawable.ic_folder_dark)
                            ?.setCheckable(true)
                            ?.run {
                                if (folder != null && folder!!.name == it.name) {
                                    isChecked = true
                                }
                            }
                }

        navigation?.menu
                ?.add(2, 2, Menu.NONE, R.string.activity_main_new_folder)
                ?.setIcon(R.drawable.ic_create_new_folder_dark)
        navigation?.setNavigationItemSelectedListener {
            when (it.itemId) {
                1 -> {
                    folder = null
                    setupContent(intent)
                }

                2 -> newFolder()

                else -> {
                    folder = Select.from(Folder::class.java)
                            .where(Condition.prop(Folder::name.name).eq(it.title))
                            .first()

                    setupContent(intent)
                }
            }

            onBackPressed()
            return@setNavigationItemSelectedListener true
        }
    }

    private fun setupContent(intent: Intent?) {
        loader?.show()
        title = if (folder != null) {
            folder!!.name
        } else {
            getString(R.string.activity_main_all_cards)
        }

        snackbar?.dismiss()
        removeCards()
        cancelSelection()
        invalidateOptionsMenu()

        SetupTask(WeakReference(this), WeakReference(intent), WeakReference(loader), WeakReference(folder)).execute()
    }

    private fun newFolder() {
        val dialogContent = layoutInflater.inflate(R.layout.dialog, null)
        val nameEditText = dialogContent.findViewById<TextView>(R.id.dialog_name)

        AlertDialog.Builder(this, R.style.AppTheme_AlertDialog)
                .setTitle(R.string.activity_main_new_folder)
                .setView(dialogContent)
                .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                    val name = nameEditText.text
                    val num = Select.from(Folder::class.java)
                            .where(Condition.prop(Folder::name.name).eq(name))
                            .count()

                    if (name.isNotEmpty() && num == 0L) {
                        folder = Folder(name.toString()).apply { save() }
                        setupNavigation()
                        setupContent(intent)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun deleteFolder() {
        AlertDialog.Builder(this, R.style.AppTheme_AlertDialog)
                .setTitle(R.string.menu_main_delete_folder)
                .setMessage(R.string.activity_main_delete_folder_confirm)
                .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                    folder?.run {
                        Select.from(CardFolder::class.java)
                                .where(Condition.prop(CardFolder::folder.name).eq(id))
                                .list()
                                .forEach { it.delete() }
                        delete()
                    }

                    folder = null
                    setupNavigation()
                    setupContent(intent)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun removeCards() {
        val adapter = recycler!!.cardAdapter

        adapter?.cardsBackup?.forEach {
            Select.from(CardFolder::class.java)
                    .where(Condition.prop(CardFolder::folder.name)
                            .eq(folder?.id))
                    .where(Condition.prop(CardFolder::card.name)
                            .eq(it.id))
                    .list()
                    .forEach { it.delete() }
        }

        adapter?.flush()
    }

    private fun cancelSelection() = recycler?.cardAdapter?.run { clear() }

    private fun addSelection() {
        val folders = SugarRecord.listAll(Folder::class.java).sorted()
        AlertDialog.Builder(this, R.style.AppTheme_AlertDialog)
                .setTitle(R.string.menu_main_selection_add)
                .setItems(folders.map { it.name }.toTypedArray()) { dialogInterface, i ->
                    recycler?.cardAdapter?.selected?.forEach {
                        val cardFolders = Select.from(CardFolder::class.java)
                                .where(Condition.prop(CardFolder::folder.name)
                                        .eq(folders[i].id))
                                .where(Condition.prop(CardFolder::card.name)
                                        .eq(it.id))

                        if (cardFolders.count() == 0L) {
                            CardFolder(it, folders[i]).save()
                        }
                    }

                    cancelSelection()
                }
                .show()
    }

    private fun removeSelection() = removeOrDeleteSelection(R.plurals.activity_main_remove) {
        removeCards()
    }

    private fun deleteSelection() = removeOrDeleteSelection(R.plurals.activity_main_delete) {
        recycler?.cardAdapter?.delete()
    }

    private fun removeOrDeleteSelection(@PluralsRes string: Int, endAction: () -> Unit) {
        val adapter = recycler!!.cardAdapter
        val count = adapter!!.selectedCount

        adapter.backup()
        snackbar = Snackbar.make(recycler as RecyclerView,
                resources.getQuantityString(string, count, count),
                Snackbar.LENGTH_LONG)
                .setAction(android.R.string.cancel) { adapter.restore() }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar?, event: Int) {
                        if (event != DISMISS_EVENT_ACTION) {
                            endAction()
                        }
                    }
                })

        snackbar?.show()
        cancelSelection()
    }
}

private class SetupTask(val activity: WeakReference<MainActivity>,
                        val intent: WeakReference<Intent?>,
                        val loader: WeakReference<ContentLoadingProgressBar?>,
                        val folder: WeakReference<Folder?>) : AsyncTask<Void, Void, CardAdapter>() {
    override fun doInBackground(vararg params: Void?): CardAdapter {
        val act: MainActivity = activity.get()!!
        val cards = when {
            folder.get() != null -> Select.from(CardFolder::class.java)
                    .where(Condition.prop(CardFolder::folder.name).eq(folder.get()!!.id))
                    .list()
                    .mapNotNull { it.card }
                    .toMutableList()
            intent.get()?.action == Intent.ACTION_SEARCH -> Select.from(Card::class.java)
                    .where(Condition.prop(Card::name.name)
                            .like("%" + intent.get()?.getStringExtra(SearchManager.QUERY) + "%"))
                    .list()
            else -> SugarRecord.listAll(Card::class.java)
        }

        val adapter = CardAdapter(cards)
        adapter.onSelectionListener = object : CardAdapter.OnSelectionListener {
            override fun onBegin() {
                act.selecting = true
                act.invalidateOptionsMenu()
            }

            override fun onEnd() {
                act.selecting = false
                act.invalidateOptionsMenu()
            }
        }

        return adapter
    }

    override fun onPostExecute(result: CardAdapter?) {
        val act: MainActivity = activity.get()!!

        if (act.recycler?.adapter == null) {
            act.recycler!!.adapter = result
        } else {
            act.recycler?.swapAdapter(result, true)
        }

        loader.get()?.hide()
        act.refresh?.isRefreshing = false
    }
}
