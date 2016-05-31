package com.github.laurenttreguier.deck

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.support.v7.app.AlertDialog
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.github.laurenttreguier.deck.model.Card
import java.io.File

class ShareActivity : Activity() {
    private var saving = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = ShareCompat.IntentReader.from(this).text.toString()
        val imageUri = ShareCompat.IntentReader.from(this).stream
        val dialogContent = layoutInflater.inflate(R.layout.dialog, null)
        val nameEditText = dialogContent.findViewById(R.id.dialog_name) as TextView?

        fun terminate() {
            val imageFile = File(imageUri.path)

            if (!saving && imageFile.exists()) {
                imageFile.delete()
            }

            val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(nameEditText?.windowToken, 0)
            finish()
        }

        AlertDialog.Builder(this, R.style.AppTheme_AlertDialog)
                .setTitle(R.string.activity_share_dialog_title)
                .setView(dialogContent)
                .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                    val id = url.replace(getString(R.string.post_url), "").replace("/", "").toLong()
                    val card = Card(id, nameEditText?.text.toString(), imageUri.path)

                    card.save()
                    saving = true
                    terminate()
                }
                .setNegativeButton(android.R.string.cancel) { dialogInterface, i -> terminate() }
                .setOnCancelListener { terminate() }
                .setOnDismissListener { terminate() }
                .show()
    }
}
