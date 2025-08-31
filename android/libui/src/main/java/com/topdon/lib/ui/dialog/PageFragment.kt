package com.topdon.lib.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.topdon.lib.ui.databinding.FragmentPageBinding

class PageFragment : Fragment() {
    
    private var _binding: FragmentPageBinding? = null
    private val binding get() = _binding!!
    
    private var imageRes: Int = 0
    
    companion object {
        private const val ARG_IMAGE_RES = "image_res"
        
        fun newInstance(imageRes: Int): PageFragment {
            val fragment = PageFragment()
            val args = Bundle()
            args.putInt(ARG_IMAGE_RES, imageRes)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageRes = it.getInt(ARG_IMAGE_RES, 0)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPageBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (imageRes != 0) {
            binding.img.setImageResource(imageRes)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}