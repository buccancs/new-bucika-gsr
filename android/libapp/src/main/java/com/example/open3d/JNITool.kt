package com.example.open3d

/**
 * JNI Tool for Open3D native library integration
 * Provides interface to Open3D point cloud processing capabilities
 */
object JNITool {
    
    init {
        try {
            System.loadLibrary("open3d")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
            // Fallback to stub implementation
        }
    }
    
    /**
     * Initialize Open3D native library
     */
    external fun initOpen3D(): Boolean
    
    /**
     * Create point cloud from depth data
     */
    external fun createPointCloud(depthData: ByteArray, width: Int, height: Int): Long
    
    /**
     * Process point cloud with given parameters
     */
    external fun processPointCloud(pointCloudPtr: Long, parameters: String): Boolean
    
    /**
     * Get processed point cloud data
     */
    external fun getPointCloudData(pointCloudPtr: Long): ByteArray?
    
    /**
     * Release point cloud resources
     */
    external fun releasePointCloud(pointCloudPtr: Long)
    
    /**
     * Apply voxel filtering to point cloud
     */
    external fun voxelFilter(pointCloudPtr: Long, voxelSize: Float): Boolean
    
    /**
     * Apply statistical outlier removal
     */
    external fun statisticalOutlierRemoval(pointCloudPtr: Long, neighbors: Int, stdRatio: Double): Boolean
    
    /**
     * Estimate normals for point cloud
     */
    external fun estimateNormals(pointCloudPtr: Long): Boolean
    
    /**
     * Perform ICP registration between two point clouds
     */
    external fun icpRegistration(sourcePtr: Long, targetPtr: Long): FloatArray?
    
    /**
     * Check if native library is available
     */
    fun isNativeLibraryAvailable(): Boolean {
        return try {
            initOpen3D()
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }
    
    /**
     * Get version information
     */
    external fun getVersion(): String
    
    /**
     * Release all native resources
     */
    external fun cleanup()
}