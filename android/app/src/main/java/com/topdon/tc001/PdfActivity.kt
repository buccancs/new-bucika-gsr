package com.topdon.tc001

import android.view.WindowManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.tc001.databinding.ActivityPdfBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * PDF Document Viewer Activity
 * 
 * This activity provides PDF document viewing functionality for the BucikaGSR application,
 * specifically designed to display device manuals and documentation for TC001 and TS004 devices.
 * 
 * Key Features:
 * - PDF document rendering with zoom and scroll capabilities
 * - Device-specific manual selection (TC001/TS004)
 * - Asset file management with local storage caching
 * - Screen-on mode during PDF viewing for better user experience
 * - Horizontal and vertical scrolling support
 * - Double-tap zoom functionality
 * - Anti-aliasing for improved rendering quality
 * 
 * ViewBinding Integration:
 * - Modern type-safe view access using ActivityPdfBinding
 * - Eliminates synthetic view imports for better compile-time safety
 * - Follows Android development best practices
 * 
 * Technical Implementation:
 * - Uses com.github.barteksc.pdfviewer.PDFView for PDF rendering
 * - Automatically copies PDF files from assets to external storage
 * - Maintains screen awake mode during document viewing
 * - Supports both TC001 and TS004 device manuals
 * 
 * @author BucikaGSR Development Team
 * @since 1.0.0
 * @see BaseActivity for common activity functionality
 */
@Route(path = RouterConfig.PDF)
class PdfActivity : BaseActivity() {

    /**
     * ViewBinding instance for type-safe view access
     * Provides compile-time verified access to all views in activity_pdf.xml
     */
    private lateinit var binding: ActivityPdfBinding

    /**
     * Initializes the content view using ViewBinding
     * 
     * @return The layout resource ID for the activity
     */
    override fun initContentView() = R.layout.activity_pdf

    /**
     * Initializes view components and configures the PDF viewer
     * 
     * Sets up the PDF viewer with:
     * - Device-specific manual selection (TC001 or TS004)
     * - Swipe navigation enabled
     * - Horizontal scrolling disabled (vertical only)
     * - Double-tap zoom functionality
     * - Anti-aliasing for better rendering
     * - Default page set to first page
     */
    override fun initView() {
        binding = ActivityPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        //本地说明书
        binding.pdfView.fromAsset(if (intent.getBooleanExtra("isTS001", false)) "TC001.pdf" else "TS004.pdf")
            .enableSwipe(true) // allows to block changing pages using swipe
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .defaultPage(0)
            .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
            .password(null)
            .scrollHandle(null)
            .enableAntialiasing(true) // improve rendering a little bit on low-res screens
            // spacing between pages in dp. To define spacing color, set view background
            .spacing(0)
            .load()
    }

    /**
     * Initializes data and manages PDF file copying from assets
     * 
     * Ensures that required PDF files are available in external storage:
     * - TC001.pdf - Manual for TC001 device
     * - TS004.pdf - Manual for TS004 device
     * 
     * Files are copied from assets to external storage if they don't exist,
     * providing faster access and reducing memory usage during viewing.
     */
    override fun initData() {
        val tc001File = File(getExternalFilesDir("pdf")!!, "TC001.pdf")
        if (!tc001File.exists()) {
            copyBigDataToSD("TC001.pdf", tc001File)
        }

        val tc004File = File(getExternalFilesDir("pdf")!!, "TS004.pdf")
        if (!tc004File.exists()) {
            copyBigDataToSD("TS004.pdf", tc004File)
        }
    }

    /**
     * Called when the activity resumes
     * Keeps screen on during PDF viewing for better user experience
     */
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Called when the activity pauses
     * Clears the keep screen on flag to preserve battery
     */
    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Copies large PDF files from assets to external storage
     * 
     * This method efficiently copies PDF files from the application assets
     * to external storage for faster access and reduced memory usage.
     * 
     * @param assetsName The name of the asset file to copy
     * @param targetFile The target file location in external storage
     * @throws IOException If an error occurs during file copying
     */
    @Throws(IOException::class)
    private fun copyBigDataToSD(assetsName: String, targetFile: File) {
        val myOutput: OutputStream = FileOutputStream(targetFile)
        val myInput = assets.open(assetsName)
        val buffer = ByteArray(1024)
        var length: Int = myInput.read(buffer)
        while (length > 0) {
            myOutput.write(buffer, 0, length)
            length = myInput.read(buffer)
        }
        myOutput.flush()
        myInput.close()
        myOutput.close()
    }

