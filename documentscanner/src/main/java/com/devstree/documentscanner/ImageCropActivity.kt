package com.devstree.documentscanner

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.devstree.documentscanner.base.CropperErrorType
import com.devstree.documentscanner.base.DocumentScanActivity
import com.devstree.documentscanner.databinding.ActivityImageCropBinding
import com.devstree.documentscanner.helpers.ScannerTheme
import com.devstree.documentscanner.helpers.ScannerTheme.cancelButtonBgColor
import com.devstree.documentscanner.helpers.ScannerTheme.cancelButtonText
import com.devstree.documentscanner.helpers.ScannerTheme.imageError
import com.devstree.documentscanner.helpers.ScannerTheme.nextButtonBgColor
import com.devstree.documentscanner.helpers.ScannerTheme.nextButtonText
import com.devstree.documentscanner.libraries.PolygonView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ImageCropActivity : DocumentScanActivity() {
    private lateinit var binding: ActivityImageCropBinding
    private var isInverted = false
    private var sourceImage: Bitmap? = null
    private var cropImage: Bitmap? = null
    private var sourceImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageCropBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (ScannerTheme.theme != 0) setTheme(ScannerTheme.theme)
        sourceImageUrl = intent.getStringExtra(EXTRA_IMAGE_PATH)
        val bitmap = BitmapFactory.decodeFile(sourceImageUrl)
        sourceImage = bitmap
        cropImage = sourceImage
        isInverted = false
        if (sourceImage != null) {
            initView()
        } else {
            Toast.makeText(this, imageError, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun getHolderImageCrop(): FrameLayout {
        return binding.holderImageCrop
    }

    override fun getImageView(): ImageView {
        return binding.imageView
    }

    override fun getPolygonView(): PolygonView {
        return binding.polygonView
    }

    override fun showProgressBar() {
        setViewInteract(binding.rlContainer, false)
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun hideProgressBar() {
        setViewInteract(binding.rlContainer, true)
        binding.progressBar.visibility = View.GONE
    }

    override fun showError(errorType: CropperErrorType) {
        if (errorType == CropperErrorType.CROP_ERROR) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, ScannerTheme.cropError, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun getBitmapImage(): Bitmap {
        return cropImage!!
    }

    private fun setViewInteract(view: View, canDo: Boolean) {
        view.isEnabled = canDo
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setViewInteract(view.getChildAt(i), canDo)
            }
        }
    }

    private fun initView() {
        binding.btnClose.text = cancelButtonText
        binding.btnImageCrop.text = nextButtonText
        binding.btnClose.setBackgroundColor(Color.parseColor(cancelButtonBgColor))
        binding.btnImageCrop.setBackgroundColor(Color.parseColor(nextButtonBgColor))
        binding.progressBar.indeterminateTintList =
            ColorStateList.valueOf(Color.parseColor(nextButtonBgColor))
        startCropping()
    }

    fun onClick(view: View) {
        when (view) {
            binding.btnClose -> {
                finish()
            }
            binding.btnImageCrop -> {
                saveScanImage()
            }
            binding.btnRotate -> {
                showProgressBar()
                disposable.add(
                    Observable.fromCallable {
                        if (isInverted) invertColor()
                        cropImage = rotateBitmap(cropImage, 90f)
                        false
                    }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { result: Boolean? ->
                            hideProgressBar()
                            startCropping()
                        }
                )
            }
            binding.btnRebase -> {
                cropImage = sourceImage?.copy(sourceImage?.config, true)
                isInverted = false
                startCropping()
            }
            binding.btnInvert -> {
                showProgressBar()
                disposable.add(
                    Observable.fromCallable {
                        invertColor()
                        false
                    }.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { result: Boolean? ->
                            hideProgressBar()
                            val scaledBitmap =
                                scaledBitmap(cropImage,
                                    holderImageCrop.width,
                                    holderImageCrop.height)
                            imageView.setImageBitmap(scaledBitmap)
                        }
                )
            }
        }
    }

    private fun invertColor() {
        if (!isInverted) {
            val bmpMonochrome = Bitmap.createBitmap(
                cropImage!!.width, cropImage!!.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmpMonochrome)
            val ma = ColorMatrix()
            ma.setSaturation(0f)
            val paint = Paint()
            paint.colorFilter = ColorMatrixColorFilter(ma)
            canvas.drawBitmap(cropImage!!, 0f, 0f, paint)
            cropImage = bmpMonochrome.copy(bmpMonochrome.config, true)
        } else {
            cropImage = sourceImage!!.copy(sourceImage!!.config, true)
        }
        isInverted = !isInverted
    }

    @AfterPermissionGranted(REQUEST_PERMISSION)
    private fun saveScanImage() {
        if (!requestPermission()) return
        showProgressBar()
        var outputImage = ""
        disposable.add(
            Observable.fromCallable {
                cropImage = croppedImage
                if (cropImage == null) return@fromCallable false
                outputImage = saveToInternalStorage(cropImage!!)
                false
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { result: Boolean? ->
                    hideProgressBar()
                    if (cropImage != null) {
                        sourceImage = cropImage
                        cropImage?.recycle()
                        sourceImage?.recycle()
                        val intent = Intent()
                        intent.putExtra(EXTRA_OUTPUT_IMAGE, outputImage)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }
        )
    }

    private fun saveToInternalStorage(bitmapImage: Bitmap): String {
        if (sourceImageUrl == null) return ""
        val sourceFile = File(sourceImageUrl!!)
        val outputFile = File(sourceFile.parent,
            "${sourceFile.nameWithoutExtension}_scan.${sourceFile.extension}")

        if (outputFile.exists()) outputFile.delete()
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(outputFile)
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return outputFile.absolutePath
    }

    private fun requestPermission(): Boolean {
        if (EasyPermissions.hasPermissions(this, *permissions)) return true
        EasyPermissions.requestPermissions(this,
            getString(R.string.permission_storage_rationale),
            REQUEST_PERMISSION,
            *permissions)
        return false
    }

    companion object {
        var EXTRA_IMAGE_PATH = "scanner_source_image"
        var EXTRA_OUTPUT_IMAGE = "scanner_output_image"
        const val REQUEST_PERMISSION = 101

        private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}