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

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;

public class VulkanInstance {
	// Instance
	private VkInstance instance;
	
	// Debug handles for instance
	private long debugID;
	private VkDebugReportCallbackEXT debugCallback;
	
	private String debugExtension;
	
	public VulkanInstance() throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			//----------------------------
			// Instance
			
			// Create application info
			VkApplicationInfo applicationInfo = VkApplicationInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
					.pApplicationName(stack.UTF8Safe("VulkanCubes"))
					.applicationVersion(VK_MAKE_VERSION(1, 0, 0))
					.pEngineName(stack.UTF8Safe("codedcosmosengine"))
					.engineVersion(VK_MAKE_VERSION(1, 0, 0))
					.apiVersion(VK_API_VERSION_1_0);
			
			// Get required extensions
			PointerBuffer pGlfwRequiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
			
			ArrayList<ByteBuffer> extensionsToEnable = new ArrayList<ByteBuffer>();
			extensionsToEnable.add(stack.UTF8(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME));
			
			// Optionally add debug extensions
			if (VulkanTriangles.USE_DEBUG) {
				extensionsToEnable.add(stack.UTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME));
			}
			
			// Allocate and fill pointerbuffer of extensions
			PointerBuffer pEnabledExtensions = stack.mallocPointer(pGlfwRequiredExtensions.remaining() + extensionsToEnable.size());
			
			pEnabledExtensions.put(pGlfwRequiredExtensions);
			for (ByteBuffer extension : extensionsToEnable) {
				pEnabledExtensions.put(extension);
			}
			pEnabledExtensions.flip();
			
			// Create required layers
			PointerBuffer pEnabledLayers = null;
			
			if (VulkanTriangles.USE_DEBUG) {
				String[] validationLayers = new String[]{"VK_LAYER_LUNARG_standard_validation", "VK_LAYER_KHRONOS_validation"};
				
				// Get number supported layers
				IntBuffer layerCount = stack.ints(0);
				vkEnumerateInstanceLayerProperties(layerCount, null);
				
				// Get supported layers
				VkLayerProperties.Buffer availableLayers = VkLayerProperties.mallocStack(layerCount.get(0), stack);
				vkEnumerateInstanceLayerProperties(layerCount, availableLayers);
				
				Set<String> avaliableLayerNames = availableLayers.stream()
						.map(VkLayerProperties::layerNameString)
						.collect(Collectors.toSet());
				
				for (int i = 0; i <= validationLayers.length; i++) {
					if (i == validationLayers.length) {
						Log.printErr("Could not find any supported validation layers. Have you installed the Vulkan SDK?");
						Log.printErr("Disabling vulkan validation layers for current instance");
						break;
					} else if (avaliableLayerNames.contains(validationLayers[i])) {
						debugExtension = validationLayers[i];
						Log.print("Picked debug extension ["+i+"]: "+ debugExtension);
						
						pEnabledLayers = stack.pointers(stack.UTF8(validationLayers[i]));
						break;
					}
				}
			}
			
			// Create instance createinfo
			VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
					.pApplicationInfo(applicationInfo)
					.ppEnabledExtensionNames(pEnabledExtensions)
					.ppEnabledLayerNames(pEnabledLayers);
			
			// Create Instance
			PointerBuffer pInstance = stack.mallocPointer(1);
			ret = vkCreateInstance(pCreateInfo, null, pInstance);
			VkUtils.check(ret, "Failed to create Vulkan Instance");
			instance = new VkInstance(pInstance.get(0), pCreateInfo);
			Log.print("Successfully created vulkan instance");
			
			//----------------------------
			// Instance Debug
			
			if (VulkanTriangles.USE_DEBUG) {
				// Vulkan Debug is enabled so setup debugger
				debugCallback = new VkDebugReportCallbackEXT() {
					public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix,
									  long pMessage, long pUserData) {
						String type = "";
						if ((flags & VK_DEBUG_REPORT_WARNING_BIT_EXT) != 0) {
							type += "WARN  ";
						}
						if ((flags & VK_DEBUG_REPORT_ERROR_BIT_EXT) != 0) {
							type += "ERROR ";
						}
						if ((flags & VK_DEBUG_REPORT_DEBUG_BIT_EXT) != 0) {
							type += "DEBUG ";
						}
						
						Log.printErr("VULKAN", type, VkDebugReportCallbackEXT.getString(pMessage));
						return 0;
					}
				};
				
				// Create debug report callback
				VkDebugReportCallbackCreateInfoEXT debugReportCallback = VkDebugReportCallbackCreateInfoEXT.callocStack(stack)
						.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT);
				
				// Create Debug Report Callback
				LongBuffer pCallback = stack.mallocLong(1);
				int logging_bits = VK_DEBUG_REPORT_ERROR_BIT_EXT;
				
				ret = vkCreateDebugReportCallbackEXT(instance, debugReportCallback
						.pfnCallback(debugCallback)
						.flags(logging_bits), null, pCallback);
				VkUtils.check(ret, "Failed to create debugging callback for vulkan instance!");
				
				debugID = pCallback.get(0);
				Log.print("Successfully setup debugging for vulkan instance");
			}
		}
	}
	
	public VkInstance get() {
		return instance;
	}
	
	public String getDebugExtension() {
		return debugExtension;
	}
	
	public void free() {
		if (VulkanTriangles.USE_DEBUG) {
			Log.print("Freeing debug instance resources");
			vkDestroyDebugReportCallbackEXT(instance, debugID, null);
			debugCallback.free();
		}
		vkDestroyInstance(instance, null);
	}
}
