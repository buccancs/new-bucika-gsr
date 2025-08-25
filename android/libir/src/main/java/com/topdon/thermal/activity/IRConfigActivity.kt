package com.topdon.thermal.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.config.RouterConfig
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.tools.NumberTools
import com.topdon.lib.core.tools.UnitTools
import com.topdon.lib.core.dialog.TipDialog
import com.topdon.lib.core.repository.TC007Repository
import com.topdon.lib.core.socket.WebSocketProxy
import com.topdon.lib.ui.widget.MyItemDecoration
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.thermal.R
import com.topdon.thermal.adapter.ConfigEmAdapter
import com.topdon.thermal.bean.DataBean
import com.topdon.thermal.bean.ModelBean
import com.topdon.thermal.databinding.ActivityIrConfigBinding
import com.topdon.thermal.databinding.ItemIrConfigConfigBinding
import com.topdon.thermal.databinding.ItemIrConfigFootBinding
import com.topdon.thermal.dialog.ConfigGuideDialog
import com.topdon.thermal.dialog.IRConfigInputDialog
import com.topdon.thermal.repository.ConfigRepository
import com.topdon.thermal.viewmodel.IRConfigViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Route(path = RouterConfig.IR_SETTING)
class IRConfigActivity : BaseActivity(), View.OnClickListener {

    private var isTC007 = false

    private val viewModel: IRConfigViewModel by viewModels()

    private lateinit var binding: ActivityIrConfigBinding

    private lateinit var adapter: ConfigAdapter

    override fun initContentView(): Int = R.layout.activity_ir_config

    @SuppressLint("SetTextI18n")
    override fun initView() {
        binding = ActivityIrConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        isTC007 = intent.getBooleanExtra(ExtraKeyConfig.IS_TC007, false)

        binding.tvDefaultTempTitle.text = "${getString(R.string.thermal_config_environment)} ${UnitTools.showConfigC(-10, if (isTC007) 50 else 55)}"
        binding.tvDefaultDisTitle.text = "${getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)"
        binding.tvDefaultEmTitle.text = "${getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"
        binding.tvDefaultTempUnit.text = UnitTools.showUnit()

        binding.ivDefaultSelector.setOnClickListener(this)
        binding.viewDefaultTempBg.setOnClickListener(this)
        binding.viewDefaultDisBg.setOnClickListener(this)
        binding.tvDefaultEmValue.setOnClickListener(this)

        adapter = ConfigAdapter(this, isTC007)
        adapter.onSelectListener = {
            viewModel.checkConfig(isTC007, it)
        }
        adapter.onDeleteListener = {
            TipDialog.Builder(this)
                .setMessage(getString(R.string.tip_config_delete, "${getString(R.string.thermal_custom_mode)}${it.name}"))
                .setPositiveListener(R.string.app_confirm) {
                    viewModel.deleteConfig(isTC007, it.id)
                }
                .setCancelListener(R.string.app_cancel)
                .create().show()
        }
        adapter.onUpdateListener = {
            viewModel.updateCustom(isTC007, it)
        }
        adapter.onAddListener = View.OnClickListener {
            TipDialog.Builder(this)
                .setMessage(R.string.tip_myself_model)
                .setPositiveListener(R.string.app_confirm) {
                    viewModel.addConfig(isTC007)
                }
                .setCancelListener(R.string.app_cancel)
                .create().show()
        }

        val itemDecoration = MyItemDecoration(this)
        itemDecoration.wholeBottom = 20f

        binding.recyclerView.addItemDecoration(itemDecoration)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = ConcatAdapter(adapter, ConfigEmAdapter(this))

        viewModel.configLiveData.observe(this) {

            binding.tvDefaultTempValue.text = NumberTools.to02(UnitTools.showUnitValue(it.defaultModel.environment))
            binding.tvDefaultDisValue.text = NumberTools.to02(it.defaultModel.distance)
            binding.tvDefaultEmValue.text = NumberTools.to02(it.defaultModel.radiation)
            binding.ivDefaultSelector.isSelected = true

            showGuideDialog(it)

            if (isTC007 && WebSocketProxy.getInstance().isTC007Connect()) {
                lifecycleScope.launch {
                    val config = ConfigRepository.readConfig(true)
                    TC007Repository.setIRConfig(config.environment, config.distance, config.radiation)
                }
            }
        }
        viewModel.getConfig(isTC007)
    }

    override fun initData() {
    }

    private fun showGuideDialog(modelBean: ModelBean) {
        if (SharedManager.configGuideStep == 0) {
            binding.ivDefaultSelector.isSelected = modelBean.defaultModel.use
            adapter.refresh(modelBean.myselfModel)
            return
        }
        val guideDialog = ConfigGuideDialog(this, isTC007, modelBean.defaultModel)
        guideDialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= 31) {
                window?.decorView?.setRenderEffect(null)
            }
            binding.ivDefaultSelector.isSelected = modelBean.defaultModel.use
            adapter.refresh(modelBean.myselfModel)
        }
        guideDialog.show()

        if (Build.VERSION.SDK_INT >= 31) {
            window?.decorView?.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR))
        } else {
            lifecycleScope.launch {

                delay(100)
                guideDialog.blurBg(binding.llRoot)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.ivDefaultSelector -> {
                viewModel.checkConfig(isTC007, 0)
            }
            binding.viewDefaultTempBg -> {
                IRConfigInputDialog(this, IRConfigInputDialog.Type.TEMP, isTC007)
                    .setInput(UnitTools.showUnitValue(viewModel.configLiveData.value?.defaultModel?.environment!!))
                    .setConfirmListener {
                        viewModel.updateDefaultEnvironment(isTC007, UnitTools.showToCValue(it))
                    }
                    .show()
            }
            binding.viewDefaultDisBg -> {
                IRConfigInputDialog(this, IRConfigInputDialog.Type.DIS, isTC007)
                    .setInput(viewModel.configLiveData.value?.defaultModel?.distance)
                    .setConfirmListener {
                        viewModel.updateDefaultDistance(isTC007, it)
                    }
                    .show()
            }
            binding.tvDefaultEmValue -> {
                IRConfigInputDialog(this, IRConfigInputDialog.Type.EM, isTC007)
                    .setInput(viewModel.configLiveData.value?.defaultModel?.radiation)
                    .setConfirmListener {
                        viewModel.updateDefaultRadiation(isTC007, it)
                    }
                    .show()
            }
        }
    }

    private class ConfigAdapter(val context: Context, val isTC007: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val dataList: ArrayList<DataBean> = ArrayList()

        var onSelectListener: ((id: Int) -> Unit)? = null
        
        var onDeleteListener: ((bean: DataBean) -> Unit)? = null
        
        var onUpdateListener: ((bean: DataBean) -> Unit)? = null

        var onAddListener: View.OnClickListener? = null

        @SuppressLint("NotifyDataSetChanged")
        fun refresh(newList: List<DataBean>) {
            dataList.clear()
            dataList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < dataList.size) 0 else 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                ItemViewHolder(ItemIrConfigConfigBinding.inflate(LayoutInflater.from(context), parent, false))
            } else {
                FootViewHolder(ItemIrConfigFootBinding.inflate(LayoutInflater.from(context), parent, false))
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ItemViewHolder) {
                val dataBean = dataList[position]
                holder.binding.tvName.text = "${context.getString(R.string.thermal_custom_mode)}${dataBean.name}"
                holder.binding.ivSelector.isSelected = dataBean.use

                holder.binding.tvTempTitle.text = "${context.getString(R.string.thermal_config_environment)} ${UnitTools.showConfigC(-10, if (isTC007) 50 else 55)}"
                holder.binding.tvDisTitle.text = "${context.getString(R.string.thermal_config_distance)} (0.2~${if (isTC007) 4 else 5}m)"
                holder.binding.tvEmTitle.text = "${context.getString(R.string.thermal_config_radiation)} (${if (isTC007) "0.1" else "0.01"}~1.00)"
                holder.binding.tvTempUnit.text = UnitTools.showUnit()

                holder.binding.tvTempValue.text = NumberTools.to02(UnitTools.showUnitValue(dataBean.environment))
                holder.binding.tvDisValue.text = NumberTools.to02(dataBean.distance)
                holder.binding.tvEmValue.text = NumberTools.to02(dataBean.radiation)
            } else if (holder is FootViewHolder) {
                holder.binding.tvAdd.setTextColor(if (dataList.size >= 10) 0x80ffffff.toInt() else 0xccffffff.toInt())
            }
        }

        override fun getItemCount(): Int = dataList.size + 1

        inner class ItemViewHolder(val binding: ItemIrConfigConfigBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.ivSelector.setOnClickListener {
                    val position: Int = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onSelectListener?.invoke(dataList[position].id)
                    }
                }
                binding.ivDel.setOnClickListener {
                    val position: Int = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onDeleteListener?.invoke(dataList[position])
                    }
                }
                binding.viewTempBg.setOnClickListener {
                    val position: Int = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        IRConfigInputDialog(context, IRConfigInputDialog.Type.TEMP, isTC007)
                            .setInput(UnitTools.showUnitValue(dataList[position].environment))
                            .setConfirmListener {
                                binding.tvTempValue.text = NumberTools.to02(UnitTools.showToCValue(it))
                                dataList[position].environment = UnitTools.showToCValue(it)
                                onUpdateListener?.invoke(dataList[position])
                            }
                            .show()
                    }
                }
                binding.viewDisBg.setOnClickListener {
                    val position: Int = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        IRConfigInputDialog(context, IRConfigInputDialog.Type.DIS, isTC007)
                            .setInput(dataList[position].distance)
                            .setConfirmListener {
                                binding.tvDisValue.text = it.toString()
                                dataList[position].distance = it
                                onUpdateListener?.invoke(dataList[position])
                            }
                            .show()
                    }
                }
                binding.tvEmValue.setOnClickListener {
                    val position: Int = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        IRConfigInputDialog(context, IRConfigInputDialog.Type.EM, isTC007)
                            .setInput(dataList[position].radiation)
                            .setConfirmListener {
                                binding.tvEmValue.text = it.toString()
                                dataList[position].radiation = it
                                onUpdateListener?.invoke(dataList[position])
                            }
                            .show()
                    }
                }
            }
        }

        inner class FootViewHolder(val binding: ItemIrConfigFootBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.viewAdd.setOnClickListener {
                    if (dataList.size < 10) {
                        val position: Int = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            onAddListener?.onClick(it)
                        }
                    } else {
                        TToast.shortToast(context, R.string.config_add_tip)
                    }
                }
                binding.tvAllEmissivity.setOnClickListener {
                    context.startActivity(Intent(context, IREmissivityActivity::class.java))
                }
            }
        }
    }
