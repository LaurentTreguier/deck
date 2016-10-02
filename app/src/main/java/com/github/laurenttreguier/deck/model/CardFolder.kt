package com.github.laurenttreguier.deck.model

import com.orm.SugarRecord

class CardFolder(var card: Card?, var folder: Folder?) : SugarRecord() {
    constructor() : this(null, null)
}