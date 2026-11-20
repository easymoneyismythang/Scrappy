package com.scrappy.app

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Preserve each screen's content padding when Android draws edge-to-edge.
fun applyScreenInsets(view: View) {
    val left=view.paddingLeft;val top=view.paddingTop
    val right=view.paddingRight;val bottom=view.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(view){v,insets->
        val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
        v.setPadding(left+bars.left,top+bars.top,right+bars.right,bottom+bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(view)
}
