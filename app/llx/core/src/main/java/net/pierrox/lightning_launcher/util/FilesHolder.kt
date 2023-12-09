package net.pierrox.lightning_launcher.util

import android.content.Context
import java.io.File

/**
 * An object which used to hold and manage common app files and directories
 *
 * @param context application context
 **/
class FilesHolder(private val context: Context) {

    private fun getTempDir() = File(context.filesDir, "tmp").apply { mkdirs() }

    /**
     * Gets a temporary picked image file
     * @return image file
     **/
    fun getTempImageFile() = File(getTempDir(), "image")

    /**
     * File authority name which used by AndroidX FileProvider
     **/
    val fileProviderName = "${context.packageName}.provider"

}