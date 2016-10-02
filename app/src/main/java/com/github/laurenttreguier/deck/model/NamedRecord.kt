package com.github.laurenttreguier.deck.model

import com.orm.SugarRecord

abstract class NamedRecord(var name: String) : SugarRecord() {
    companion object {
        var reversed: Boolean = false
    }
}