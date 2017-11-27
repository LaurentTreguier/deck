package com.github.laurenttreguier.deck

import android.content.Intent
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.laurenttreguier.deck.model.Card
import java.io.File
import java.util.*

class CardAdapter(private val cards: MutableList<Card>) :
        RecyclerView.Adapter<CardAdapter.ViewHolder>() {
    val cardsBackup = ArrayList<Card>()
    val selected = ArrayList<Card>()
    val selectedCount: Int
        get() = selected.size
    var onSelectionListener: OnSelectionListener? = null

    companion object {
        private val UNSELECTED_SCALE = 1f
        private val SELECTED_SCALE = 0.9f
    }

    init {
        Collections.sort(cards)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? {
        val inflater = LayoutInflater.from(parent?.context)
        val cardView = inflater.inflate(R.layout.card, parent, false)

        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val card = cards[position]

        fun updateSelect(animate: Boolean) {
            holder?.run {
                val visibility = if (selected.contains(card)) View.VISIBLE else View.GONE
                val scale = if (selected.contains(card)) SELECTED_SCALE else UNSELECTED_SCALE

                select?.visibility = visibility

                if (animate) {
                    ViewCompat.animate(itemView)
                            ?.setInterpolator(FastOutSlowInInterpolator())
                            ?.setDuration(itemView.context?.resources
                                    ?.getInteger(android.R.integer.config_shortAnimTime)!!.toLong())
                            ?.scaleX(scale)
                            ?.scaleY(scale)
                } else {
                    itemView?.scaleX = scale
                    itemView?.scaleY = scale
                }
            }
        }

        fun toggle() {
            if (selected.size == 0) {
                onSelectionListener?.onBegin()
            }

            if (selected.contains(card)) {
                selected.remove(card)
            } else {
                selected.add(card)
            }

            updateSelect(true)

            if (selected.size == 0) {
                onSelectionListener?.onEnd()
            }
        }

        holder?.itemView?.run {
            holder.title?.text = card.name

            val request = Glide.with(holder.preview?.context).load(File(card.previewPath))
            val params = holder.preview?.layoutParams

            if (params?.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                request.listener(object : RequestListener<File, GlideDrawable> {
                    override fun onException(e: Exception?, model: File?,
                                             target: Target<GlideDrawable>?,
                                             isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: GlideDrawable?, model: File?,
                                                 target: Target<GlideDrawable>?,
                                                 isFromMemoryCache: Boolean,
                                                 isFirstResource: Boolean): Boolean {
                        holder.preview?.let {
                            it.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                                override fun onLayoutChange(v: View?, left: Int, top: Int,
                                                            right: Int, bottom: Int,
                                                            oldLeft: Int, oldTop: Int,
                                                            oldRight: Int, oldBottom: Int) {
                                    it.removeOnLayoutChangeListener(this)

                                    if (it.height > 0) {
                                        params.width = it.width
                                        params.height = it.height
                                        holder.preview?.layoutParams = params
                                    }
                                }
                            })
                        }

                        return false
                    }
                })
            }

            request.into(holder.preview)

            setOnClickListener {
                if (selected.size > 0) {
                    toggle()
                } else {
                    val url = Constants.POST_URL + card.id
                    holder.preview?.context?.startActivity(Intent.parseUri(url, 0)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }

            setOnLongClickListener {
                toggle()
                return@setOnLongClickListener true
            }

            updateSelect(false)
        }
    }

    override fun getItemCount() = cards.size

    fun backup() {
        cards.removeAll(selected)
        cardsBackup.addAll(selected)
        selected.clear()
        notifyDataSetChanged()
    }

    fun restore() {
        cards.addAll(cardsBackup)
        cardsBackup.clear()
        Collections.sort(cards)
        notifyDataSetChanged()
    }

    fun flush() = cardsBackup.clear()

    fun delete() {
        cardsBackup.forEach { it.delete() }
        flush()
    }

    fun clear() {
        while (selected.size > 0) {
            notifyItemChanged(cards.indexOf(selected.removeAt(0)))
        }

        onSelectionListener?.onEnd()
    }

    class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null
        var preview: ImageView? = null
        var select: ImageView? = null

        init {
            title = itemView?.findViewById(R.id.card_title)
            preview = itemView?.findViewById(R.id.card_preview)
            select = itemView?.findViewById(R.id.card_select)
        }
    }

    interface OnSelectionListener {
        fun onBegin()
        fun onEnd()
    }
}

val RecyclerView.cardAdapter: CardAdapter?
    get() = adapter?.run { this as CardAdapter }
