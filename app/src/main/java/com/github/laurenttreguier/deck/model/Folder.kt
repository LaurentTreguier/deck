package com.github.laurenttreguier.deck.model

import java.util.*

class Folder(name: String) : NamedRecord(name), Comparable<Folder> {
    constructor() : this("")

    init {
        id = Calendar.getInstance().time.time
    }

    override fun compareTo(other: Folder): Int {
        val a = if (reversed) this else other
        val b = if (reversed) other else this
        return a.name.compareTo(b.name)
    }
}
