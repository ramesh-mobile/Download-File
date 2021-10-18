package com.sr.savefileproject.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.avatar.inpsection.data.network.RetrofitInterface
import com.sr.dummyapidemo.data.di.component.DaggerRetroComponent
import com.sr.savefileproject.AppApplication
import com.sr.savefileproject.R
import okhttp3.ResponseBody
import java.io.*
import java.util.*
import javax.inject.Inject

/**
 * Created by ramesh on 16-10-2021
 */
class DownloadWorker(val context: Context,val workerParameters: WorkerParameters) : Worker(context,workerParameters){

    private val TAG = "DownloadWorker"

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager
    private var totalFileSize = 0
    private val notId = 100

    var APP_FOLDER = "Gigs Project"

    var APP_COMPLETE_FOLDER = Environment.getExternalStorageDirectory().toString() +
            File.separator + APP_FOLDER

    var outputStream: OutputStream? = null

    @Inject
    lateinit var retrofitInterface: RetrofitInterface

    init {
        DaggerRetroComponent.create().injectRetro(this)
    }

    override fun doWork(): Result {

        val path = "http://www.africau.edu/images/default/sample.pdf";
        //val path = "https://www.tutorialspoint.com/android/android_tutorial.pdf"
        //val path = "http://projanco.com/Library/Android%20App%20Development%20in%20Android%20Studio%20-%20Java%20plus%20Android%20edition%20for%20beginners.pdf"
        val originalDocName = "sample.pdf"

        try {
            setupNotification(context.getString(R.string.downloading_initializing))
            initDownload(path,originalDocName)

            return Result.success()
        }
        catch (e : Exception){
            globalMessage = context.getString(R.string.error_downloading)
            notificationNotify(globalMessage)
            Log.e(TAG, "doWork: got an error ${e.message}" )
            //e.printStackTrace()
        }
        return Result.failure()
    }

    private fun setupNotification(msg: String) {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = NotificationCompat.Builder(
            applicationContext, AppApplication.CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(msg)
            .setSmallIcon(R.drawable.ic_launcher_background)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppApplication.CHANNEL_ID,
                AppApplication.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
            NotificationCompat.Builder(context, AppApplication.CHANNEL_ID)
        }
        notificationManager.notify(notId, notificationBuilder.build());
    }

    private fun initDownload(path: String, originalDocName: String) {
        val request = retrofitInterface.downloadFile(path)
        val body = request?.execute()?.body()
        if (body != null)
            downloadFile(body, originalDocName)
        else
            notificationNotify(context.getString(R.string.file_not_availble_error))
    }

    private fun downloadFile(body: ResponseBody, originalDocName: String) {

        //create file outputstream with scoped storage
        setupScopedStorage(originalDocName)

        val data = ByteArray(1024 * 4)
        val bis: InputStream = BufferedInputStream(body.byteStream(), 1024 * 8)

        var count: Int
        var timeCount = 1
        var total: Long = 0
        val fileSize = body.contentLength()
        val startTime = System.currentTimeMillis()

        while (bis.read(data).also { count = it } != -1) {
            outputStream?.write(data, 0, count)

            //for notification updated
            total += count.toLong()
            totalFileSize = Math.round(fileSize / Math.pow(1024.0, 2.0)).toInt()
            val current = Math.round(total / Math.pow(1024.0, 2.0)).toDouble()
            val progress = (total * 100 / fileSize).toInt()
            val currentTime = System.currentTimeMillis() - startTime

            if (currentTime > 1000 * timeCount) {
                sendNotification(progress,current.toInt(),totalFileSize)
                timeCount++
            }

        }
        onDownloadComplete()
        outputStream?.flush()
        outputStream?.close()
        bis.close()
    }

    private fun setupScopedStorage(originalDocName: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, originalDocName)
                put(MediaStore.MediaColumns.MIME_TYPE, "pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + File.separator + APP_FOLDER)
            }
            val contentResolver = context.contentResolver
            var mUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream =
                contentResolver.openOutputStream(Objects.requireNonNull(mUri)!!) as FileOutputStream?
        } else {
            if (!File(APP_COMPLETE_FOLDER).exists()) {
                File(APP_COMPLETE_FOLDER).mkdirs()
            }
            var outputFile = File(APP_COMPLETE_FOLDER, originalDocName)
            outputStream = FileOutputStream(outputFile)
        }
    }

    private fun sendNotification(progress : Int, currentFileSize: Int,totalFileSize: Int) {
        var contentMsg = "Downloading.." + progress+ "%" +
                "(" +currentFileSize + "MB/" + totalFileSize + "MB)"
        Log.d(TAG,contentMsg)
        notificationBuilder.setProgress(100,progress,false);
        notificationNotify(contentMsg)
    }

    private fun onDownloadComplete() {
        notificationBuilder.setProgress(0,0,false)
        notificationNotify(context.getString(R.string.file_downloaded))
    }

    private fun notificationNotify(msg : String) {
        notificationBuilder.setContentText(msg);
        globalMessage = msg
        notificationManager.notify(notId, notificationBuilder.build());
    }

    companion object{
        var globalMessage : String= ""
    }
}