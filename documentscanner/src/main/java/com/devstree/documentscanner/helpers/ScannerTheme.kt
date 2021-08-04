package com.devstree.documentscanner.helpers

import androidx.annotation.StyleRes

object ScannerTheme {
    @StyleRes var theme: Int=0

    var nextButtonText = "Next"
    var cancelButtonText = "Cancel"
    var cancelButtonBgColor = "#6666ff"
    var nextButtonBgColor = "#ff0000"
    var progressColor = "#331199"

    var imageError = "No image selected, please try again."
    var cropError = "You have not selected a valid field. Please correct until the lines turn blue."
}