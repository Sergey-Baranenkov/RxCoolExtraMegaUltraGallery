package com.example.rxgallery

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.rxgallery.databinding.ActivityMainBinding
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val imagePaths: MutableList<String> = mutableListOf()
    private val disposableBag = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.startLoading.setOnClickListener(::startLoading)
        binding.cancelLoading.setOnClickListener(::cancelLoading)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_IMAGES
            )
        } else {
            readImages()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableBag.clear()
    }

    private fun startLoading(view: View) {
        val disposable = Flowable.fromIterable(imagePaths)
            .subscribeOn(Schedulers.io())
            .doOnNext{ try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }}
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::load, Throwable::printStackTrace)

        disposableBag.add(disposable)
    }

    private fun cancelLoading(view: View) {
        disposableBag.clear()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_IMAGES -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readImages()
                }
                return
            }
        }
    }

    private fun readImages() {
        val imageCursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.DATA),
            null,
            null,
            null
        ) ?: return

        repeat(imageCursor.count) {
            imageCursor.moveToNext()
            imagePaths.add(imageCursor.getString(0))
        }

        imageCursor.close()
    }

    private fun load(imagePath: String) {
        try {
            binding.imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_READ_IMAGES = 0
    }
}