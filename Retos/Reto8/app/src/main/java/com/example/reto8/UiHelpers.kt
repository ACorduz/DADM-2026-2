package com.example.reto8

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * The app draws edge to edge: the app bar grows under the status bar and
 * the rest of the screen stays clear of the navigation bar and cutouts.
 */
fun applySystemBarInsets(root: View, appBar: View) {
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        appBar.setPadding(0, bars.top, 0, 0)
        v.setPadding(bars.left, 0, bars.right, bars.bottom)
        insets
    }
}

/** e.g. "Consultoría, Fábrica de software", in declaration order. */
fun Context.classificationLabels(classifications: Set<Classification>): String =
    classifications.sortedBy { it.ordinal }.joinToString(", ") { getString(it.label) }

/** Asks before deleting [company]; calls [onDeleted] once it's gone. */
fun confirmDelete(
    activity: Activity,
    db: CompanyDbHelper,
    company: Company,
    onDeleted: () -> Unit,
) {
    AlertDialog.Builder(activity)
        .setTitle(R.string.delete_title)
        .setMessage(activity.getString(R.string.delete_message, company.name))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.action_delete) { _, _ ->
            if (db.delete(company.id)) {
                Toast.makeText(
                    activity, activity.getString(R.string.deleted, company.name), Toast.LENGTH_SHORT
                ).show()
            }
            // Also when it was already gone, so the screen stops showing it
            onDeleted()
        }
        .show()
}

/** Opens [intent] in another app, or tells the user there's none for it. */
fun Activity.startExternal(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show()
    }
}
