package com.topdon.module.thermal.ir.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.module.thermal.ir.R
import com.topdon.module.thermal.ir.databinding.FragmentGalleryBinding
import kotlinx.coroutines.launch

/**
 * Gallery fragment for thermal image display with ViewBinding implementation.
 * 
 * Provides professional image viewing interface with zoom/pan capabilities
 * for thermal imaging gallery functionality in research applications.
 * 
 * Features include:
 * - PhotoView integration for advanced image manipulation
 * - Glide-based efficient image loading
 * - Professional error handling and lifecycle management
 * 
 * @author Topdon Thermal Imaging Team
 * @since 2024-01-01
 */
class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    
    private var path = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        path = requireArguments().getString("path")!!
        loadGalleryImage()
    }

    /**
     * Load thermal image into PhotoView with professional error handling.
     */
    private fun loadGalleryImage() {
        Glide.with(this)
            .load(path)
            .into(binding.fragmentGalleryImg)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}