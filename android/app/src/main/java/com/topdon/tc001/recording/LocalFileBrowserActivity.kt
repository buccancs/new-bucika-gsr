package com.topdon.tc001.recording

import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.config.FileConfig
import com.topdon.tc001.R
import com.topdon.tc001.databinding.ActivityLocalFileBrowserBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LocalFileBrowserActivity : BaseActivity() {

    private lateinit var binding: ActivityLocalFileBrowserBinding
    private lateinit var fileAdapter: FileAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        private const val SUPPORTED_VIDEO_EXTENSIONS = listOf("mp4", "avi", "mov")
        private const val SUPPORTED_DATA_EXTENSIONS = listOf("csv", "txt") 
        private const val SUPPORTED_JSON_EXTENSIONS = listOf("json")
    }
    
    override fun initContentView(): Int = R.layout.activity_local_file_browser

    override fun initView() {
        binding = ActivityLocalFileBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.titleView.setTitle("Local File Browser")
        binding.titleView.setLeftClickListener { finish() }
        
        setupRecyclerView()
        loadFiles()
    }

    override fun initData() {

    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter { file ->
            openFile(file)
        }
        
        binding.recyclerFiles.apply {
            layoutManager = LinearLayoutManager(this@LocalFileBrowserActivity)
            adapter = fileAdapter
        }
    }

    private fun loadFiles() {
        val recordingDirs = listOf(
            File(FileConfig.lineGalleryDir),
            File(FileConfig.lineGalleryDir, "enhanced_recordings"),
            File(FileConfig.lineGalleryDir, "gsr_data")
        )
        
        val allFiles = mutableListOf<FileItem>()
        val supportedExtensions = SUPPORTED_VIDEO_EXTENSIONS + SUPPORTED_DATA_EXTENSIONS + SUPPORTED_JSON_EXTENSIONS
        
        recordingDirs.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.extension.lowercase() in supportedExtensions)) {
                        allFiles.add(FileItem(file, getFileType(file)))
                    }
                }
            }
        }
        
        allFiles.sortByDescending { it.file.lastModified() }
        
        fileAdapter.submitList(allFiles)
        
        if (allFiles.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.recyclerFiles.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.recyclerFiles.visibility = View.VISIBLE
        }
        
        binding.tvFileCount.text = "Found ${allFiles.size} files"
    }

    private fun getFileType(file: File): FileType {
        return when (file.extension.lowercase()) {
            in SUPPORTED_VIDEO_EXTENSIONS -> FileType.VIDEO
            in SUPPORTED_DATA_EXTENSIONS -> FileType.DATA
            in SUPPORTED_JSON_EXTENSIONS -> FileType.JSON
            else -> FileType.OTHER
        }
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                ?: "*
    enum class FileType {
        
        VIDEO, 
        
        DATA, 
        
        JSON, 
        
        OTHER
    }

    data class FileItem(
        val file: File,
        val type: FileType
    )

    private inner class FileAdapter(
        private val onFileClick: (File) -> Unit
    ) : RecyclerView.Adapter<FileViewHolder>() {
        
        private var files = listOf<FileItem>()
        
        fun submitList(newFiles: List<FileItem>) {
            files = newFiles
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FileViewHolder {
            val view = layoutInflater.inflate(R.layout.item_file, parent, false)
            return FileViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            holder.bind(files[position])
        }
        
        override fun getItemCount(): Int = files.size
    }

    private inner class FileViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        
        private val tvFileName = itemView.findViewById<android.widget.TextView>(R.id.tv_file_name)
        private val tvFileSize = itemView.findViewById<android.widget.TextView>(R.id.tv_file_size)
        private val tvFileDate = itemView.findViewById<android.widget.TextView>(R.id.tv_file_date)
        private val ivFileType = itemView.findViewById<android.widget.ImageView>(R.id.iv_file_type)
        
        fun bind(fileItem: FileItem) {
            val file = fileItem.file
            
            tvFileName.text = file.name
            tvFileSize.text = formatFileSize(file.length())
            tvFileDate.text = dateFormat.format(Date(file.lastModified()))
            
            val iconRes = when (fileItem.type) {
                FileType.VIDEO -> R.drawable.ic_video_white_svg
                FileType.DATA -> android.R.drawable.ic_menu_agenda
                FileType.JSON -> android.R.drawable.ic_menu_info_details
                FileType.OTHER -> android.R.drawable.ic_menu_gallery
            }
            ivFileType.setImageResource(iconRes)
            
            itemView.setOnClickListener {
                onFileClick(file)
            }
            
            itemView.setOnLongClickListener {
                showFileOptions(file)
                true
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.2f MB".format(mb)
            kb >= 1 -> "%.2f KB".format(kb)
            else -> "$bytes B"
        }
    }

    private fun showFileOptions(file: File) {
        val options = arrayOf("Open", "Share", "Delete", "Properties")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFile(file)
                    1 -> shareFile(file)
                    2 -> deleteFile(file)
                    3 -> showFileProperties(file)
                }
            }
            .show()
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this,
                "Error sharing file: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteFile(file: File) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                if (file.delete()) {
                    android.widget.Toast.makeText(this, "File deleted", android.widget.Toast.LENGTH_SHORT).show()
                    loadFiles()
                } else {
                    android.widget.Toast.makeText(this, "Failed to delete file", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFileProperties(file: File) {
        val properties = """
            Name: ${file.name}
            Size: ${formatFileSize(file.length())}
            Created: ${dateFormat.format(Date(file.lastModified()))}
            Path: ${file.absolutePath}
            Type: ${file.extension.uppercase()}
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("File Properties")
            .setMessage(properties)
            .setPositiveButton("OK", null)
            .show()
    }
