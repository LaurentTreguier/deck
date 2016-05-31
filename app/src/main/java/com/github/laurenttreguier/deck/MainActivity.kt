package com.github.laurenttreguier.deck

import android.app.SearchManager
import android.content.Intent
import android.graphics.Rect
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.MenuItemCompat
import android.support.v4.widget.ContentLoadingProgressBar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.github.laurenttreguier.deck.model.Card
import com.orm.SugarRecord
import com.orm.query.Condition
import com.orm.query.Select

class MainActivity : AppCompatActivity() {
    private var toolbar: Toolbar? = null
    private var refresh: SwipeRefreshLayout? = null
    private var recycler: RecyclerView? = null
    private var loader: ContentLoadingProgressBar? = null
    private var selecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.activity_main_toolbar) as Toolbar?
        refresh = findViewById(R.id.activity_main_refresh) as SwipeRefreshLayout?
        recycler = findViewById(R.id.activity_main_content) as RecyclerView?
        loader = findViewById(R.id.activity_main_loader) as ContentLoadingProgressBar?

        refresh?.setColorSchemeColors(ResourcesCompat
                .getColor(resources, R.color.colorAccent, theme))

        refresh?.setOnRefreshListener {
            setupContent(intent)
        }

        recycler?.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?,
                                        state: RecyclerView.State?) {
                outRect?.top = resources.getDimension(R.dimen.activity_vertical_margin).toInt()
                outRect?.left = resources.getDimension(R.dimen.activity_horizontal_margin).toInt()
            }
        })

        setSupportActionBar(toolbar)
        setupContent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setupContent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(if (selecting) R.menu.main_delete else R.menu.main, menu)
        supportActionBar?.setDisplayHomeAsUpEnabled(selecting)

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
        } else {
            toolbar?.setNavigationOnClickListener {
                (recycler?.adapter as CardAdapter).clear()
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.main_delete -> {
                val adapter = recycler?.adapter as CardAdapter
                val count = adapter.getSelectedCount()

                adapter.backup()
                Snackbar.make(recycler as RecyclerView,
                        resources.getQuantityString(R.plurals.activity_main_delete, count, count),
                        Snackbar.LENGTH_LONG)
                        .setAction(android.R.string.cancel, {
                            adapter.restore()
                        })
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

    private fun setupContent(intent: Intent?) {
        loader?.show()
        AsyncTask.execute {
            val cards = if (intent?.action == Intent.ACTION_SEARCH) {
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

            runOnUiThread {
                recycler?.adapter = adapter
                loader?.hide()
                refresh?.isRefreshing = false
            }
        }
    }
}
