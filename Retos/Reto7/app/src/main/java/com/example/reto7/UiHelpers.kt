package com.example.reto7

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.view.View
import android.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * There's no title bar, so keep the content clear of the status and
 * navigation bars (and any display cutout in landscape).
 */
fun applySystemBarPadding(root: View) {
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}

/** Shows the given menu anchored to the corner menu button. */
fun showMenuPopup(anchor: View, menuRes: Int, onItemClick: (Int) -> Boolean) {
    val popup = PopupMenu(anchor.context, anchor)
    popup.menuInflater.inflate(menuRes, popup.menu)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        popup.setForceShowIcon(true)
    }
    popup.setOnMenuItemClickListener { onItemClick(it.itemId) }
    popup.show()
}

/** Shows the About dialog (about_dialog.xml). */
fun showAboutDialog(activity: Activity) {
    AlertDialog.Builder(activity)
        .setView(activity.layoutInflater.inflate(R.layout.about_dialog, null))
        .setPositiveButton(R.string.about_ok, null)
        .show()
}
