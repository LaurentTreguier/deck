package com.github.laurenttreguier.deck

import android.app.SearchManager
import android.content.Intent
import android.graphics.Rect
import android.os.AsyncTask
import android.os.Bundle
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
import android.widget.Toast
import com.github.laurenttreguier.deck.model.Card
import com.github.laurenttreguier.deck.model.CardFolder
import com.github.laurenttreguier.deck.model.Folder
import com.orm.SugarRecord
import com.orm.query.Condition
import com.orm.query.Select

class MainActivity : AppCompatActivity() {
    private var drawer: DrawerLayout? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private var toolbar: Toolbar? = null
    private var navigation: NavigationView? = null
    private var refresh: SwipeRefreshLayout? = null
    private var recycler: RecyclerView? = null
    private var loader: ContentLoadingProgressBar? = null
    private var selecting = false
    private var folder: Folder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawer = findViewById(R.id.drawer) as DrawerLayout?
        toolbar = findViewById(R.id.activity_main_toolbar) as Toolbar?
        navigation = findViewById(R.id.navigation) as NavigationView?
        refresh = findViewById(R.id.activity_main_refresh) as SwipeRefreshLayout?
        recycler = findViewById(R.id.activity_main_content) as RecyclerView?
        loader = findViewById(R.id.activity_main_loader) as ContentLoadingProgressBar?

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
        menuInflater.inflate(if (selecting) R.menu.main_delete else R.menu.main, menu)

        if (!selecting) {
            val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
            val searchItem = menu?.findItem(R.id.main_search)
            val searchView = searchItem?.actionView as SearchView

            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
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
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.main_new_folder -> newFolder()

            R.id.main_delete_folder -> deleteFolder()

            R.id.main_cancel -> (recycler?.adapter as CardAdapter).clear()

            R.id.main_delete -> {
                val adapter = recycler?.adapter as CardAdapter
                val count = adapter.getSelectedCount()

                adapter.backup()
                Snackbar.make(recycler as RecyclerView,
                        resources.getQuantityString(R.plurals.activity_main_delete, count, count),
                        Snackbar.LENGTH_LONG)
                        .setAction(android.R.string.cancel, { adapter.restore() })
                        .setCallback(object : Snackbar.Callback() {
                            override fun onDismissed(snackbar: Snackbar?, event: Int) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    adapter.delete()
                                }
                            }
                        })
                        .show()

                selecting = false
                invalidateOptionsMenu()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawer!!.isDrawerOpen(GravityCompat.START)) {
            drawer?.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupNavigation() {
        navigation?.menu?.clear()
        navigation?.menu
                ?.add(0, 1, 0, R.string.activity_main_all_cards)
                ?.setIcon(R.drawable.ic_folder_special_dark)

        SugarRecord.listAll(Folder::class.java)
                .sorted()
                .forEach { navigation?.menu?.add(it.name)?.setIcon(R.drawable.ic_folder_dark) }

        navigation?.menu
                ?.add(0, 2, 0, R.string.activity_main_new_folder)
                ?.setIcon(R.drawable.ic_add_dark)
        navigation?.setNavigationItemSelectedListener {
            when (it.itemId) {
                1 -> {
                    folder = null
                    setupContent(intent)
                }

                2 -> newFolder()

                else -> {
                    folder = Select.from(Folder::class.java)
                            .where(Condition.prop(Folder::name.name)
                                    .eq(it.title))
                            .list()[0]

                    setupContent(intent)
                }
            }

            onBackPressed()
            return@setNavigationItemSelectedListener true
        }
    }

    private fun setupContent(intent: Intent?) {
        object : AsyncTask<Void, Void, CardAdapter>() {
            override fun onPreExecute() {
                loader?.show()
                title = if (folder != null) {
                    folder!!.name
                } else {
                    getString(R.string.activity_main_all_cards)
                }
            }

            override fun doInBackground(vararg params: Void?): CardAdapter {
                val cards = if (folder != null) {
                    Select.from(CardFolder::class.java)
                            .where(Condition.prop(CardFolder::folder.name)
                                    .eq(folder!!.id))
                            .list()
                            .mapNotNull { it.card }
                            .toMutableList()
                } else if (intent?.action == Intent.ACTION_SEARCH) {
                    Select.from(Card::class.java)
                            .where(Condition.prop(Card::name.name)
                                    .like("%" + intent?.getStringExtra(SearchManager.QUERY) + "%"))
                            .list()
                } else {
                    SugarRecord.listAll(Card::class.java)
                }

                val adapter = CardAdapter(cards)
                adapter.setOnSelectionListener(object : CardAdapter.OnSelectionListener {
                    override fun onBegin() {
                        selecting = true
                        invalidateOptionsMenu()
                    }

                    override fun onEnd() {
                        selecting = false
                        invalidateOptionsMenu()
                    }
                })

                return adapter
            }

            override fun onPostExecute(result: CardAdapter?) {
                recycler?.adapter = result
                loader?.hide()
                refresh?.isRefreshing = false
            }
        }.execute()
    }

    private fun newFolder() {
        val dialogContent = layoutInflater.inflate(R.layout.dialog, null)
        val nameEditText = dialogContent.findViewById(R.id.dialog_name) as TextView

        AlertDialog.Builder(this, R.style.AppTheme_AlertDialog)
                .setTitle(R.string.activity_share_dialog_title)
                .setView(dialogContent)
                .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                    val name = nameEditText.text

                    if (name.isNotEmpty()) {
                        Folder(name.toString()).save()
                        setupNavigation()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun deleteFolder() {
        if (folder != null) {
            folder!!.let {
                Select.from(CardFolder::class.java)
                        .where(Condition.prop(CardFolder::folder.name)
                                .eq(it.id))
                        .list().forEach { it.delete() }
                it.delete()
            }

            folder = null
            setupNavigation()
            setupContent(intent)
        } else {
            Toast.makeText(this, R.string.activity_main_cannot_delete, Toast.LENGTH_SHORT).show()
        }
    }
}
