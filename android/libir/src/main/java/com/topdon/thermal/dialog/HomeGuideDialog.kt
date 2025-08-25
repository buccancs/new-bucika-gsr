package com.topdon.thermal.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.topdon.thermal.R
import com.topdon.thermal.databinding.DialogHomeGuideBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeGuideDialog(context: Context, private val currentStep: Int) : Dialog(context, R.style.TransparentDialog) {

    private lateinit var binding: DialogHomeGuideBinding

    var onNextClickListener: ((step: Int) -> Unit)? = null

    var onSkinClickListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        
        binding = DialogHomeGuideBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        when (currentStep) {
            1 -> {
                binding.clGuide1.isVisible = true
                binding.clGuide2.isVisible = false
                binding.clGuide3.isVisible = false
            }
            2 -> {
                binding.clGuide1.isVisible = false
                binding.clGuide2.isVisible = true
                binding.clGuide3.isVisible = false
            }
            3 -> {
                binding.clGuide1.isVisible = false
                binding.clGuide2.isVisible = false
                binding.clGuide3.isVisible = true
            }
        }

        binding.tvNext1.setOnClickListener {
            onNextClickListener?.invoke(1)
            binding.clGuide1.isVisible = false
            binding.clGuide2.isVisible = true
        }
        binding.tvNext2.setOnClickListener {
            onNextClickListener?.invoke(2)
            binding.clGuide2.isVisible = false
            binding.clGuide3.isVisible = true
        }
        binding.tvIKnow.setOnClickListener {
            onNextClickListener?.invoke(3)
            dismiss()
        }

        binding.tvSkin1.setOnClickListener {
            onSkinClickListener?.invoke()
            dismiss()
        }
        binding.tvSkin2.setOnClickListener {
            onSkinClickListener?.invoke()
            dismiss()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onSkinClickListener?.invoke()
    }

    fun blurBg(rootView: View) {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                val sourceBitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                val outputBitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(sourceBitmap)
                rootView.draw(canvas)

                val renderScript = RenderScript.create(context)
                val inputAllocation = Allocation.createFromBitmap(renderScript, sourceBitmap)
                val outputAllocation = Allocation.createTyped(renderScript, inputAllocation.type)

                val blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
                blurScript.setRadius(20f)
                blurScript.setInput(inputAllocation)
                blurScript.forEach(outputAllocation)
                outputAllocation.copyTo(outputBitmap)
                renderScript.destroy()

                launch(Dispatchers.Main) {
                    binding.ivBlurBg.isVisible = true
                    binding.ivBlurBg.setImageBitmap(outputBitmap)
                }
            } catch (_: Exception) {

            }
        }
    }
