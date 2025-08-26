package com.topdon.thermal.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.topdon.lib.core.tools.GlideLoader
import com.topdon.thermal.R
import com.topdon.thermal.databinding.FragmentGalleryBinding
import kotlinx.coroutines.launch

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

    private fun loadGalleryImage() {
        Glide.with(this)
            .load(path)
            .into(binding.fragmentGalleryImg)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
