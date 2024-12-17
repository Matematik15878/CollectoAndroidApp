package com.collecto.collectoandroidapp.view

import android.view.View
import android.app.Activity
import android.app.AlertDialog
import android.widget.ProgressBar
import com.collecto.collectoandroidapp.R

class LoadingOverlay(activity: Activity) {

    private val progressBar: ProgressBar = activity.findViewById(R.id.progressBar)
    private val darkenView: View = activity.findViewById(R.id.darken_view)
    private var loadingDialog: AlertDialog? = null

    fun show() {
        progressBar.visibility = View.VISIBLE
        darkenView.visibility = View.VISIBLE
        darkenView.isClickable = true
        loadingDialog?.show()
    }

    fun hide() {
        progressBar.visibility = View.GONE
        darkenView.visibility = View.GONE
        darkenView.isClickable = false
        loadingDialog?.dismiss()
    }

}