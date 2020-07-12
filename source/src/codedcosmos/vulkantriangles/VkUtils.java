/*
 *     VulkanTriangles by codedcosmos
 *
 *     VulkanTriangles is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License 3 as published by
 *     the Free Software Foundation.
 *     VulkanTriangles is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License 3 for more details.
 *     You should have received a copy of the GNU General Public License 3
 *     along with VulkanTriangles.  If not, see <https://www.gnu.org/licenses/>.
 */

package codedcosmos.vulkantriangles;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.EXTBufferDeviceAddress.VK_ERROR_INVALID_DEVICE_ADDRESS_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.EXTDescriptorIndexing.VK_ERROR_FRAGMENTATION_EXT;
import static org.lwjgl.vulkan.EXTFullScreenExclusive.VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT;
import static org.lwjgl.vulkan.EXTGlobalPriority.VK_ERROR_NOT_PERMITTED_EXT;
import static org.lwjgl.vulkan.EXTImageDrmFormatModifier.VK_ERROR_INVALID_DRM_FORMAT_MODIFIER_PLANE_LAYOUT_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.NVGLSLShader.VK_ERROR_INVALID_SHADER_NV;
import static org.lwjgl.vulkan.NVRayTracing.*;
import static org.lwjgl.vulkan.NVRayTracing.VK_SHADER_STAGE_ANY_HIT_BIT_NV;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_ERROR_INVALID_EXTERNAL_HANDLE;
import static org.lwjgl.vulkan.VK11.VK_ERROR_OUT_OF_POOL_MEMORY;

public class VkUtils {
	public static void check(int ret, String message) throws VulkanException {
		if (ret != VK_SUCCESS) {
			throw new VulkanException(formatRet(ret) + " : " + message);
		}
	}
	
	public static String formatRet(int ret) {
		switch (ret) {
			// Success codes
			case VK_SUCCESS: return "VK_SUCCESS";
			case VK_NOT_READY: return "VK_NOT_READY";
			case VK_TIMEOUT: return "VK_TIMEOUT";
			case VK_EVENT_SET: return "VK_EVENT_SET";
			case VK_EVENT_RESET: return "VK_EVENT_RESET";
			case VK_INCOMPLETE: return "VK_INCOMPLETE";
			case VK_SUBOPTIMAL_KHR: return "VK_SUBOPTIMAL_KHR";
			
			// Error codes
			case VK_ERROR_OUT_OF_HOST_MEMORY: return "VK_ERROR_OUT_OF_HOST_MEMORY";
			case VK_ERROR_OUT_OF_DEVICE_MEMORY: return "VK_ERROR_OUT_OF_DEVICE_MEMORY";
			case VK_ERROR_INITIALIZATION_FAILED: return "VK_ERROR_INITIALIZATION_FAILED";
			case VK_ERROR_DEVICE_LOST: return "VK_ERROR_DEVICE_LOST";
			case VK_ERROR_MEMORY_MAP_FAILED: return "VK_ERROR_MEMORY_MAP_FAILED";
			case VK_ERROR_LAYER_NOT_PRESENT: return "VK_ERROR_LAYER_NOT_PRESENT";
			case VK_ERROR_EXTENSION_NOT_PRESENT: return "VK_ERROR_EXTENSION_NOT_PRESENT";
			case VK_ERROR_FEATURE_NOT_PRESENT: return "VK_ERROR_FEATURE_NOT_PRESENT";
			case VK_ERROR_INCOMPATIBLE_DRIVER: return "VK_ERROR_INCOMPATIBLE_DRIVER";
			case VK_ERROR_TOO_MANY_OBJECTS: return "VK_ERROR_TOO_MANY_OBJECTS";
			case VK_ERROR_FORMAT_NOT_SUPPORTED: return "VK_ERROR_FORMAT_NOT_SUPPORTED";
			case VK_ERROR_FRAGMENTED_POOL: return "VK_ERROR_FRAGMENTED_POOL";
			case VK_ERROR_OUT_OF_POOL_MEMORY: return "VK_ERROR_OUT_OF_POOL_MEMORY";
			case VK_ERROR_INVALID_EXTERNAL_HANDLE: return "VK_ERROR_INVALID_EXTERNAL_HANDLE";
			case VK_ERROR_SURFACE_LOST_KHR: return "VK_ERROR_SURFACE_LOST_KHR";
			case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR: return "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR";
			case VK_ERROR_OUT_OF_DATE_KHR: return "VK_ERROR_OUT_OF_DATE_KHR";
			case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR: return "VK_ERROR_INCOMPATIBLE_DISPLAY_KHR";
			case VK_ERROR_VALIDATION_FAILED_EXT: return "VK_ERROR_VALIDATION_FAILED_EXT";
			case VK_ERROR_INVALID_SHADER_NV: return "VK_ERROR_INVALID_SHADER_NV";
			case VK_ERROR_INVALID_DRM_FORMAT_MODIFIER_PLANE_LAYOUT_EXT: return "VK_ERROR_INVALID_DRM_FORMAT_MODIFIER_PLANE_LAYOUT_EXT";
			case VK_ERROR_NOT_PERMITTED_EXT: return "VK_ERROR_NOT_PERMITTED_EXT";
			case VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT: return "VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT";
			case VK_ERROR_FRAGMENTATION_EXT: return "VK_ERROR_FRAGMENTATION_EXT";
			case VK_ERROR_INVALID_DEVICE_ADDRESS_EXT: return "VK_ERROR_INVALID_DEVICE_ADDRESS_EXT";
			
			// Default
			default: return "UNKNOWN:"+ret;
		}
	}
	
	public static String getVendorByID(int id) {
		switch (id) {
			case 0x1002: return "AMD";
			case 0x1010: return "ImgTec";
			case 0x10DE: return "NVIDIA";
			case 0x13B5: return "ARM";
			case 0x5143: return "Qualcomm";
			case 0x8086: return "INTEL";
		}
		return "UNKNOWN";
	}
	
	public static int vulkanShaderStageToShadercKind(int stage) throws VulkanException {
		switch (stage) {
			case VK_SHADER_STAGE_VERTEX_BIT:
				return shaderc_vertex_shader;
			case VK_SHADER_STAGE_FRAGMENT_BIT:
				return shaderc_fragment_shader;
			case VK_SHADER_STAGE_RAYGEN_BIT_NV:
				return shaderc_raygen_shader;
			case VK_SHADER_STAGE_CLOSEST_HIT_BIT_NV:
				return shaderc_closesthit_shader;
			case VK_SHADER_STAGE_MISS_BIT_NV:
				return shaderc_miss_shader;
			case VK_SHADER_STAGE_ANY_HIT_BIT_NV:
				return shaderc_anyhit_shader;
			case VK_SHADER_STAGE_COMPUTE_BIT:
				return shaderc_compute_shader;
			default:
				throw new VulkanException("Could not find shader stage ("+stage+")");
		}
	}
}
