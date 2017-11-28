package com.github.laurenttreguier.deck.model

import com.orm.SugarRecord

class CardFolder(var card: Card? = null, var folder: Folder? = null) : SugarRecord() {
}