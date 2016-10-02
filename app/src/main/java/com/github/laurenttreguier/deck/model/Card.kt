package com.github.laurenttreguier.deck.model

import java.io.File
import java.util.*

class Card(postId: Long, name: String, var previewPath: String) : NamedRecord(name), Comparable<Card> {
    companion object {
        val COMPARATOR_ALPHABETICAL: Comparator<Card> = Comparator { o1, o2 ->
            val a = if (reversed) o2 else o1
            val b = if (reversed) o1 else o2
            return@Comparator a.name.compareTo(b.name)
        }

        val COMPARATOR_TIMESTAMP: Comparator<Card> = Comparator { o1, o2 ->
            val a = if (reversed) o2 else o1
            val b = if (reversed) o1 else o2
            return@Comparator a.timestamp.compareTo(b.timestamp)
        }

        var comparator: Comparator<Card> = COMPARATOR_TIMESTAMP
    }

    var timestamp: Date = Calendar.getInstance().time

    constructor() : this(0, "", "")

    init {
        id = postId
    }

    override fun compareTo(other: Card): Int {
        return comparator.compare(this, other)
    }

    override fun delete(): Boolean {
        File(previewPath).delete()
        return super.delete()
    }
}
