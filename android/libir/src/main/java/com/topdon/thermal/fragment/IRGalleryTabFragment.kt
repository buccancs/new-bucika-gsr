package com.topdon.thermal.fragment

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.elvishew.xlog.XLog
import com.google.android.material.tabs.TabLayoutMediator
import com.topdon.lib.core.config.ExtraKeyConfig
import com.topdon.lib.core.ktbase.BaseFragment
import com.topdon.lib.core.repository.GalleryRepository.DirType
import com.topdon.thermal.R
import com.topdon.thermal.databinding.FragmentGalleryTabBinding
import com.topdon.thermal.event.GalleryDirChangeEvent
import com.topdon.thermal.popup.GalleryChangePopup
import com.topdon.thermal.popup.OptionPickPopup
import com.topdon.thermal.viewmodel.IRGalleryTabViewModel
import org.greenrobot.eventbus.EventBus

class IRGalleryTabFragment : BaseFragment() {
    
    private var _binding: FragmentGalleryTabBinding? = null
    private val binding get() = _binding!!

    private var hasBackIcon = false
    
    private var canSwitchDir = true
    
    private var currentDirType = DirType.LINE

    private val viewModel: IRGalleryTabViewModel by activityViewModels()

    private var viewPagerAdapter: ViewPagerAdapter? = null

    override fun initContentView(): Int = R.layout.fragment_gallery_tab

    override fun initView() {
        _binding = FragmentGalleryTabBinding.inflate(layoutInflater)
        
        hasBackIcon = arguments?.getBoolean(ExtraKeyConfig.HAS_BACK_ICON, false) ?: false
        canSwitchDir = arguments?.getBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, false) ?: false
        currentDirType = when (arguments?.getInt(ExtraKeyConfig.DIR_TYPE, 0) ?: 0) {
            DirType.TS004_LOCALE.ordinal -> DirType.TS004_LOCALE
            DirType.TS004_REMOTE.ordinal -> DirType.TS004_REMOTE
            DirType.TC007.ordinal -> DirType.TC007
            else -> DirType.LINE
        }

        binding.tvTitleDir.text = when (currentDirType) {
            DirType.LINE -> getString(R.string.tc_has_line_device)
            DirType.TC007 -> "TC007"
            else -> "TS004"
        }
        binding.tvTitleDir.isVisible = canSwitchDir
        binding.tvTitleDir.setOnClickListener {
            val popup = GalleryChangePopup(requireContext())
            popup.onPickListener = { position, str ->
                currentDirType = when (position) {
                    0 -> DirType.LINE
                    1 -> DirType.TS004_LOCALE
                    else -> DirType.TC007
                }
                binding.tvTitleDir.text = str
                EventBus.getDefault().post(GalleryDirChangeEvent(currentDirType))
            }
            popup.show(binding.tvTitleDir)
        }

        binding.titleView.setTitleText(if (canSwitchDir) "" else getString(R.string.app_gallery))
        binding.titleView.setLeftDrawable(if (hasBackIcon) R.drawable.ic_back_white_svg else 0)
        binding.titleView.setLeftClickListener {
            if (viewModel.isEditModeLD.value == true) {

                viewModel.isEditModeLD.value = false
            } else {

                if (hasBackIcon) {
                    requireActivity().finish()
                }
            }
        }
        binding.titleView.setRightDrawable(R.drawable.ic_toolbar_check_svg)
        binding.titleView.setRightClickListener {
            if (viewModel.isEditModeLD.value == true) {

                viewModel.selectAllIndex.value = binding.viewPager2.currentItem
            } else {

                viewModel.isEditModeLD.value = true
            }
        }

        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager2.adapter = viewPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.setText(if (position == 0) R.string.album_menu_Photos else R.string.app_video)
        }.attach()

        viewModel.isEditModeLD.observe(viewLifecycleOwner) { isEditMode ->
            if (isEditMode) {
                binding.titleView.setLeftDrawable(R.drawable.svg_x_cc)
            } else {
                binding.titleView.setLeftDrawable(if (hasBackIcon) R.drawable.ic_back_white_svg else 0)
            }
            binding.titleView.setRightDrawable(if (isEditMode) 0 else R.drawable.ic_toolbar_check_svg)
            binding.titleView.setRightText(if (isEditMode) getString(R.string.report_select_all) else "")
            binding.tabLayout.isVisible = !isEditMode
            binding.viewPager2.isUserInputEnabled = !isEditMode
            if (isEditMode) {
                binding.titleView.setTitleText(getString(R.string.chosen_item, viewModel.selectSizeLD.value))
                binding.tvTitleDir.isVisible = false
            } else {
                binding.titleView.setTitleText(if (canSwitchDir) "" else getString(R.string.app_gallery))
                binding.tvTitleDir.isVisible = canSwitchDir
            }
        }
        
        viewModel.selectSizeLD.observe(viewLifecycleOwner) {
            if (viewModel.isEditModeLD.value == true) {
                binding.titleView.setTitleText(getString(R.string.chosen_item, it))
                binding.tvTitleDir.isVisible = false
            } else {
                binding.titleView.setTitleText(if (canSwitchDir) "" else getString(R.string.app_gallery))
                binding.tvTitleDir.isVisible = canSwitchDir
            }
        }
    }

    override fun initData() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            val bundle = Bundle()
            bundle.putBoolean(ExtraKeyConfig.IS_VIDEO, position == 1)
            bundle.putInt(ExtraKeyConfig.DIR_TYPE, currentDirType.ordinal)
            val fragment = IRGalleryFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
