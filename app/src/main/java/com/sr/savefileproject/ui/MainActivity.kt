package com.sr.savefileproject.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.*
import com.google.android.material.snackbar.Snackbar
import com.sr.savefileproject.R
import com.sr.savefileproject.data.worker.DownloadWorker
import com.sr.savefileproject.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    lateinit var binding : ActivityMainBinding

    var PERMISSION_REQUEST_CODE = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDownload.setOnClickListener {
            if (!checkUserPermission()) {
                requestPermission()
                showSnack(getString(R.string.permission_not_available))
            }
            else{
                startWorkManager()
            }
        }

    }

    fun startWorkManager(){
        var constraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        onWork(true)

        //cancel all work
        WorkManager.getInstance(this).cancelAllWork()

        var oneTimeDownloadWorker = OneTimeWorkRequest.Builder(DownloadWorker::class.java)
            .setConstraints(constraint)
            .build()

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(oneTimeDownloadWorker.id)
            .observe(this, Observer { workInfo->
                if(workInfo!=null){
                    if (workInfo.state == WorkInfo.State.RUNNING) {
                        showMessage(getString(R.string.file_is_downloading))
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        showMessage(DownloadWorker.globalMessage)
                        onWork(false)
                    } else if (workInfo.state == WorkInfo.State.SUCCEEDED ) {
                        showMessage(getString(R.string.file_downloaded))
                        onWork(false)
                    }
                }
            })
        WorkManager.getInstance(this).enqueue(oneTimeDownloadWorker)
    }

    fun onWork(isWorkStarted: Boolean){
        if(isWorkStarted){
            binding.progressBar.visibility = View.VISIBLE
            binding.btnDownload.isEnabled = false
        }
        else {
            binding.btnDownload.isEnabled = true
            binding.progressBar.visibility = View.GONE
        }
    }

    fun showSnack(msg: String) {
        Snackbar.make(binding.btnDownload,"$msg",Snackbar.LENGTH_LONG).show()
    }

    fun showMessage(msg: String) {
        binding.lblMessage.text = msg
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<String?>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var isAllGranted  = true
            Log.d(TAG, "onRequestPermissionsResult: "+isAllGranted)
            grantResults.forEach {
                if(it != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false
                    return@forEach
                }
            }
            if(isAllGranted==true)
                startWorkManager()
            else
                showMessage(getString(R.string.permission_denied))
        }
    }

    private fun checkUserPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
    }

}