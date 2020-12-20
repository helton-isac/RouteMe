package com.hitg.routeme.presentation.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

object DialogUtils {
    fun showSimpleDialog(context: Context, title: String, message: String) {
        val alertDialog: AlertDialog = AlertDialog.Builder(context).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL,
            "OK",
            DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() })
        alertDialog.show()
    }
}