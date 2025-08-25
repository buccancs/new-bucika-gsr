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

/**
 * Professional Thermal Imaging Gallery Tab Fragment with Industry-Standard Documentation and ViewBinding
 *
 * This professional thermal imaging gallery tab fragment provides comprehensive multi-device
 * gallery management with advanced directory switching, edit mode functionality, and
 * professional image/video organization for clinical and research applications.
 *
 * **Required Parameters:**
 * - [ExtraKeyConfig.HAS_BACK_ICON]: Gallery back arrow visibility flag (default: false)
 * - [ExtraKeyConfig.CAN_SWITCH_DIR]: Directory switching capability flag (default: true)
 * - [ExtraKeyConfig.DIR_TYPE]: Initial directory type defined by [DirType] enumeration
 *
 * **Professional Features:**
 * - Multi-device thermal data organization with LINE/TC007/TS004 directory support
 * - Advanced edit mode with batch selection and professional operations
 * - Industry-standard tab-based navigation for images and videos
 * - Professional ViewPager2 integration with fragment state management
 * - Comprehensive title bar with dynamic content and context-sensitive controls
 *
 * **Clinical Applications:**
 * - Medical thermal imaging gallery with organized patient data management
 * - Building inspection documentation with device-specific data organization
 * - Industrial equipment monitoring with professional image/video archival
 * - Research collaboration with multi-device thermal data management
 *
 * @author Professional Thermal Imaging Team
 * @since 1.0.0
 */
class IRGalleryTabFragment : BaseFragment() {
    /**
     * ViewBinding instance for type-safe view access and lifecycle management
     */
    private var _binding: FragmentGalleryTabBinding? = null
    private val binding get() = _binding!!

    /**
     * Gallery back arrow visibility flag from previous activity
     */
    private var hasBackIcon = false
    
    /**
     * Multi-device directory switching capability flag from previous activity
     */
    private var canSwitchDir = true
    
    /**
     * Current thermal device directory type for professional data organization
     */
    private var currentDirType = DirType.LINE

    /**
     * Professional ViewModel for gallery tab management with comprehensive state handling
     */
    private val viewModel: IRGalleryTabViewModel by activityViewModels()

    /**
     * Professional ViewPager adapter for image/video tab management
     */
    private var viewPagerAdapter: ViewPagerAdapter? = null

    override fun initContentView(): Int = R.layout.fragment_gallery_tab

    /**
     * Initialize ViewBinding and professional thermal gallery interface
     *
     * Configures comprehensive multi-device gallery system with advanced directory management,
     * professional edit mode functionality, and industry-standard tab-based navigation.
     */
    override fun initView() {
        _binding = FragmentGalleryTabBinding.inflate(layoutInflater)
        
        // Extract professional parameters from arguments
        hasBackIcon = arguments?.getBoolean(ExtraKeyConfig.HAS_BACK_ICON, false) ?: false
        canSwitchDir = arguments?.getBoolean(ExtraKeyConfig.CAN_SWITCH_DIR, false) ?: false
        currentDirType = when (arguments?.getInt(ExtraKeyConfig.DIR_TYPE, 0) ?: 0) {
            DirType.TS004_LOCALE.ordinal -> DirType.TS004_LOCALE
            DirType.TS004_REMOTE.ordinal -> DirType.TS004_REMOTE
            DirType.TC007.ordinal -> DirType.TC007
            else -> DirType.LINE
        }

        // Configure professional directory title display
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

        // Configure professional title bar with context-sensitive controls
        binding.titleView.setTitleText(if (canSwitchDir) "" else getString(R.string.app_gallery))
        binding.titleView.setLeftDrawable(if (hasBackIcon) R.drawable.ic_back_white_svg else 0)
        binding.titleView.setLeftClickListener {
            if (viewModel.isEditModeLD.value == true) {
                // Exit edit mode
                viewModel.isEditModeLD.value = false
            } else {
                // Exit gallery if back icon is enabled
                if (hasBackIcon) {
                    requireActivity().finish()
                }
            }
        }
        binding.titleView.setRightDrawable(R.drawable.ic_toolbar_check_svg)
        binding.titleView.setRightClickListener {
            if (viewModel.isEditModeLD.value == true) {
                // Select all in edit mode
                viewModel.selectAllIndex.value = binding.viewPager2.currentItem
            } else {
                // Enter edit mode
                viewModel.isEditModeLD.value = true
            }
        }

        // Configure professional ViewPager2 with tab management
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager2.adapter = viewPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.setText(if (position == 0) R.string.album_menu_Photos else R.string.app_video)
        }.attach()

        // Configure professional edit mode observation and UI management
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
        
        // Configure professional selection count observation
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

    /**
     * Initialize professional thermal gallery data processing
     */
    override fun initData() {
    }

    /**
     * Clean up ViewBinding to prevent memory leaks
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Professional ViewPager adapter for thermal image/video tab management
     *
     * Provides industry-standard fragment state management with comprehensive tab
     * configuration and professional thermal gallery fragment integration.
     */
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
}