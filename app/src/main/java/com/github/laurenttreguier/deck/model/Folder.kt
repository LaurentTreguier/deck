package com.github.laurenttreguier.deck.model

import java.util.*

class Folder(name: String) : NamedRecord(name), Comparable<Folder> {
    constructor() : this("")

    init {
        id = Calendar.getInstance().time.time
    }

    override fun compareTo(other: Folder): Int {
        val a = if (reversed) other else this
        val b = if (reversed) this else other
        return a.name.compareTo(b.name)
    }
}
