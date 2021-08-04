package com.devstree.documentscanner

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.devstree.documentscanner.databinding.ActivityMainBinding
import com.devstree.documentscanner.helpers.ScannerTheme
import com.devstree.mediafilepicker.bottomsheet.BottomSheetFilePicker
import com.devstree.mediafilepicker.listener.MediaPickerCallback
import com.devstree.mediafilepicker.model.Media
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ScannerTheme.nextButtonText = "Next"
        ScannerTheme.cancelButtonText = "Cancel"
        ScannerTheme.nextButtonBgColor = "#FF6200EE"
        ScannerTheme.cancelButtonBgColor = "#FF000000"
    }

    fun onClick(view: View) {
        when (view) {
            binding.btnPickImage -> {
                val bottomSheetFilePicker = BottomSheetFilePicker(BuildConfig.APPLICATION_ID)
                bottomSheetFilePicker.setMediaListenerCallback(BottomSheetFilePicker.IMAGE,
                    object : MediaPickerCallback {
                        override fun onPickedSuccess(media: Media?) {
                            if (media == null) return
                            openScanner(media.url)
                        }

                        override fun onPickedError(error: String?) {
                        }

                        override fun showProgressBar(enable: Boolean) {
                        }
                    })
                bottomSheetFilePicker.show(supportFragmentManager, "image")
            }
        }
    }

    private fun openScanner(imgPath: String?) {
        if (imgPath == null) return
        val intent = Intent(this, ImageCropActivity::class.java)
        intent.putExtra(ImageCropActivity.EXTRA_IMAGE_PATH, imgPath)
        scanResult.launch(intent)
    }

    private var scanResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val image = result.data!!.getStringExtra(ImageCropActivity.EXTRA_OUTPUT_IMAGE)
                if (!image.isNullOrEmpty()) {
                    binding.imgScanResult.setImageURI(Uri.fromFile(File(image)))
                } else Toast.makeText(this, "Document parsing failed", Toast.LENGTH_LONG).show()
            }
        }
}