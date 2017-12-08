package com.github.laurenttreguier.deck.model

import java.io.File
import java.util.*

class Card(name: String = "", var postId: String = "", var previewPath: String = "") : NamedRecord(name), Comparable<Card> {
    companion object {
        val COMPARATOR_ALPHABETICAL: Comparator<Card> = Comparator { o1, o2 ->
            val a = if (reversed) o2 else o1
            val b = if (reversed) o1 else o2
            return@Comparator a.name.compareTo(b.name)
        }

        val COMPARATOR_TIMESTAMP: Comparator<Card> = Comparator { o1, o2 ->
            val a = if (reversed) o1 else o2
            val b = if (reversed) o2 else o1
            return@Comparator a.timestamp.compareTo(b.timestamp)
        }

        var comparator: Comparator<Card> = COMPARATOR_TIMESTAMP
    }

    var timestamp: Date = Calendar.getInstance().time

    override fun compareTo(other: Card): Int {
        return comparator.compare(this, other)
    }

    override fun delete(): Boolean {
        File(previewPath).delete()
        return super.delete()
    }
}
