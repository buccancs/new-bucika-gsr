package com.topdon.thermal.dialog

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.tools.NumberTools
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.ui.widget.MyItemDecoration
import com.topdon.thermal.R
import com.topdon.thermal.adapter.ConfigEmAdapter
import com.topdon.thermal.bean.DataBean
import com.topdon.thermal.databinding.DialogConfigGuideBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfigGuideDialog(context: Context, val isTC007: Boolean, val dataBean: DataBean) : Dialog(context, R.style.TransparentDialog) {

    private lateinit var binding: DialogConfigGuideBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        
        binding = DialogConfigGuideBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.tvDefaultTempTitle.text = "${context.getString(R.string.thermal_config_environment)} ${UnitTools.showConfigC(-10, if (isTC007) 50 else 55)}"
        binding.tvDefaultDisTitle.text = "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)"
        binding.tvSpaceEmTitle.text = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"

        binding.tvDefaultEmTitle.text = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"
        binding.tvDefaultEmValue.text = NumberTools.to02(dataBean.radiation)

        val itemDecoration = MyItemDecoration(context)
        itemDecoration.wholeBottom = 20f

        binding.recyclerView.addItemDecoration(itemDecoration)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = ConfigEmAdapter(context)

        binding.clStep1.isVisible = SharedManager.configGuideStep == 1
        binding.clStep2Top.isVisible = SharedManager.configGuideStep == 2
        binding.clStep2Bottom.isVisible = SharedManager.configGuideStep == 2

        binding.tvNext.setOnClickListener {
            binding.clStep1.isVisible = false
            binding.clStep2Top.isVisible = true
            binding.clStep2Bottom.isVisible = true
            SharedManager.configGuideStep = 2
        }
        binding.tvIKnow.setOnClickListener {
            dismiss()
            SharedManager.configGuideStep = 0
        }
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
