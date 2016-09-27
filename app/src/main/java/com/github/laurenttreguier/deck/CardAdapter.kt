package com.github.laurenttreguier.deck

import android.content.Intent
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
    private val cardsBackup = ArrayList<Card>()
    private val selected = ArrayList<Card>()
    private var onSelectionListener: OnSelectionListener? = null

    companion object {
        private val SELECTED_SCALE = 0.9f
        private val UNSELECTED_SCALE = 1f
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
            holder?.let {
                val visibility = if (selected.contains(card)) View.VISIBLE else View.GONE
                val scale = if (selected.contains(card)) SELECTED_SCALE else UNSELECTED_SCALE

                holder.select?.visibility = visibility

                if (animate) {
                    holder.root?.animate()
                            ?.setInterpolator(FastOutSlowInInterpolator())
                            ?.setDuration(holder.root?.context?.resources
                                    ?.getInteger(android.R.integer.config_shortAnimTime)!!.toLong())
                            ?.scaleX(scale)
                            ?.scaleY(scale)
                } else {
                    holder.root?.scaleX = scale
                    holder.root?.scaleY = scale
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

        holder?.root?.let {
            holder.title?.text = card.name

            val request = Glide.with(holder.preview?.context).load(File(card.previewPath))
            val params = holder.preview?.layoutParams

            if (params?.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                request.listener(object : RequestListener<File, GlideDrawable> {
                    override fun onException(e: Exception?, model: File?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: GlideDrawable?, model: File?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                        holder.preview?.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                            override fun onLayoutChange(p0: View?, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int) {
                                holder.preview?.removeOnLayoutChangeListener(this)
                                params?.width = holder.preview?.width
                                params?.height = holder.preview?.height
                                holder.preview?.layoutParams = params
                            }
                        })

                        return false
                    }
                })
            }

            request.into(holder.preview)

            it.setOnClickListener {
                if (selected.size > 0) {
                    toggle()
                } else {
                    val url = holder.title?.context?.getString(R.string.post_url) + card.id
                    holder.preview?.context?.startActivity(Intent.parseUri(url, 0)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }

            it.setOnLongClickListener {
                toggle()
                return@setOnLongClickListener true
            }

            updateSelect(false)
        }
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    fun getSelectedCount(): Int {
        return selected.size
    }

    fun setOnSelectionListener(onSelectionListener: OnSelectionListener) {
        this.onSelectionListener = onSelectionListener
    }

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

    fun delete() {
        cardsBackup.forEach { it.delete() }
        cardsBackup.clear()
    }

    fun clear() {
        selected.clear()
        notifyDataSetChanged()
        onSelectionListener?.onEnd()
    }

    class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
        var root: ViewGroup? = null
        var title: TextView? = null
        var preview: ImageView? = null
        var select: ImageView? = null

        init {
            root = itemView as ViewGroup?
            title = itemView?.findViewById(R.id.card_title) as TextView?
            preview = itemView?.findViewById(R.id.card_preview) as ImageView?
            select = itemView?.findViewById(R.id.card_select) as ImageView?
        }
    }

    interface OnSelectionListener {
        fun onBegin()
        fun onEnd()
    }
}
