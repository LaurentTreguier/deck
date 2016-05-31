package com.github.laurenttreguier.deck.model

import com.orm.SugarRecord
import java.io.File
import java.util.*

class Card(postId: Long, var name: String, var previewPath: String) : SugarRecord(), Comparable<Card> {
    var timestamp: Date

    constructor() : this(0, "", "")

    init {
        id = postId
        timestamp = Calendar.getInstance().time
    }

    override fun compareTo(other: Card): Int {
        return other.timestamp.compareTo(timestamp)
    }

    override fun delete(): Boolean {
        File(previewPath).delete()
        return super.delete()
    }
}
