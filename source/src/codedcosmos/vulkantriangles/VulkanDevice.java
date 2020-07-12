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

import codedcosmos.vulkantriangles.graphics.GameWindow;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.KHRDedicatedAllocation.VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRGetMemoryRequirements2.VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanDevice {
	
	// Device
	private VkDevice device;
	
	// Queues
	private VkQueue graphicsQueue;
	private VkQueue computeQueue;
	private int graphicsQueueFamily;
	private int computeQueueFamily;
	
	private boolean queuesAreSplit;
	
	// Memory
	private VkPhysicalDeviceProperties properties;
	private VkPhysicalDeviceMemoryProperties memoryProperties;
	
	public VulkanDevice(VulkanInstance vulkanInstance, GameWindow window) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			//----------------------------
			// Extract
			VkInstance instance = vulkanInstance.get();
			long surface = window.getSurface();
			
			//----------------------------
			// Parse Physical Vulkan Devices
			
			// Obtain number of avaliable devices
			IntBuffer pPhysicalDeviceCount = stack.mallocInt(1);
			
			ret = vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, null);
			VkUtils.check(ret, "Failed to obtain number of physical devices");
			
			int physicalDeviceCount = pPhysicalDeviceCount.get(0);
			if (physicalDeviceCount == 0) {
				throw new VulkanException("No vulkan devices avaliable");
			}
			
			// Create list of devices
			PointerBuffer pPhysicalDevices = stack.mallocPointer(physicalDeviceCount);
			ret = vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, pPhysicalDevices);
			VkUtils.check(ret, "Failed to get physical devices");
			
			VkPhysicalDevice[] parsedDevices = new VkPhysicalDevice[physicalDeviceCount];
			for (int i = 0; i < physicalDeviceCount; i++) {
				VkPhysicalDevice device = new VkPhysicalDevice(pPhysicalDevices.get(i), instance);
				parsedDevices[i] = device;
			}
			
			Log.print("Found " + parsedDevices.length + " vulkan device" + (parsedDevices.length == 1 ? "":"s"));
			
			//----------------------------
			// Pick Physical Device
			
			VkPhysicalDevice validPhysicalDevice = null;
			graphicsQueueFamily = -1;
			computeQueueFamily = -1;
			
			// Iterate over all devices and find the first valid one
			for (int i = 0; i <= parsedDevices.length; i++) {
				if (i == parsedDevices.length) {
					throw new VulkanException("Could not find valid physical device");
				}
				
				// Get device
				VkPhysicalDevice physicalDevice = parsedDevices[i];
				
				// Verify extensions
				IntBuffer pPropertyCount = stack.mallocInt(1);
				ret = vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer)null, pPropertyCount, null);
				VkUtils.check(ret, "Failed to enumerate device extensions count");
				
				VkExtensionProperties.Buffer pProperties = VkExtensionProperties.mallocStack(pPropertyCount.get(0), stack);
				ret = vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer)null, pPropertyCount, pProperties);
				VkUtils.check(ret, "Failed to enumerate device extensions");
				
				if (!pProperties.stream().anyMatch(p -> p.extensionNameString().equals(VK_KHR_SWAPCHAIN_EXTENSION_NAME))) {
					throw new VulkanException("Missing required extension " + VK_KHR_SWAPCHAIN_EXTENSION_NAME);
				}
				if (!pProperties.stream().anyMatch(p -> p.extensionNameString().equals(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME))) {
					throw new VulkanException("Missing required extension " + VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME);
				}
				
				// Optimal queue family
				boolean optimalComputeFamily = false;
				
				// List queue families
				IntBuffer pQueueFamilyPropertyCount = stack.mallocInt(1);
				vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, null);
				
				int queueFamilyPropertyCount = pQueueFamilyPropertyCount.get(0);
				if (queueFamilyPropertyCount == 0) {
					// No queue family properties
					throw new VulkanException("No queue family properties");
				}
				
				VkQueueFamilyProperties.Buffer familyProperties = VkQueueFamilyProperties.callocStack(queueFamilyPropertyCount, stack);
				vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, familyProperties);
				int queueFamilyIndex = 0;
				
				// Iterate over
				for (VkQueueFamilyProperties queueFamilyProperties : familyProperties) {
					IntBuffer pSupported = stack.mallocInt(1);
					if (queueFamilyProperties.queueCount() < 1) {
						continue;
					}
					
					// Check for presentation support
					vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, queueFamilyIndex, surface, pSupported);
					boolean supportsPresentation = (pSupported.get(0) != 0);
					
					// Check for graphics support
					boolean supportsGraphics = (queueFamilyProperties.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0;
					
					// Check for compute support
					boolean supportsCompute = (queueFamilyProperties.queueFlags() & VK_QUEUE_COMPUTE_BIT) != 0;
					
					// Set queue families
					if (graphicsQueueFamily == -1) {
						if (supportsPresentation && supportsGraphics) {
							graphicsQueueFamily = queueFamilyIndex;
						}
					}
					
					if (!optimalComputeFamily) {
						// See if queueFamily supports compute at all
						if (supportsCompute) {
							if (!supportsGraphics && !supportsPresentation) {
								computeQueueFamily = queueFamilyIndex;
								optimalComputeFamily = true;
							} else {
								computeQueueFamily = queueFamilyIndex;
							}
						}
					}
					
					// Increment
					queueFamilyIndex++;
				}
				
				// Skip device if there are queue families are invalid
				if (computeQueueFamily == -1 || graphicsQueueFamily == -1) {
					Log.printErr("Could not find queue families for device " + i);
					continue;
				}
				
				// Device is valid
				validPhysicalDevice = physicalDevice;
				break;
			}
			
			Log.print("Picked vulkan device");
			
			// --------------------------------------
			// Create Queues
			VkDeviceQueueCreateInfo.Buffer pQueueCreateInfos;
			if (graphicsQueueFamily == computeQueueFamily) {
				// If they are the same, create a single queue
				pQueueCreateInfos = VkDeviceQueueCreateInfo.callocStack(1, stack)
						.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
						.queueFamilyIndex(graphicsQueueFamily)
						.pQueuePriorities(stack.floats(1.0f));
			} else {
				// If they are different, create two queues
				pQueueCreateInfos = VkDeviceQueueCreateInfo.callocStack(2, stack);
				pQueueCreateInfos.get(0)
						.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
						.queueFamilyIndex(graphicsQueueFamily)
						.pQueuePriorities(stack.floats(0.9f));
				pQueueCreateInfos.get(1)
						.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
						.queueFamilyIndex(computeQueueFamily)
						.pQueuePriorities(stack.floats(1.0f));
			}
			
			// --------------------------------------
			// Enable extensions
			
			// Get properties
			IntBuffer pPropertyCount = stack.mallocInt(1);
			ret = vkEnumerateDeviceExtensionProperties(validPhysicalDevice, (ByteBuffer)null, pPropertyCount, null);
			VkUtils.check(ret, "Failed to enumerate device extensions count");
			
			VkExtensionProperties.Buffer pProperties = VkExtensionProperties.mallocStack(pPropertyCount.get(0), stack);
			ret = vkEnumerateDeviceExtensionProperties(validPhysicalDevice, (ByteBuffer)null, pPropertyCount, pProperties);
			VkUtils.check(ret, "Failed to enumerate device extensions");
			
			// Add extensions
			PointerBuffer extensions = stack.mallocPointer(2 + 1);
			extensions.put(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME))
					.put(stack.UTF8(VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME));
			
			if (pProperties.stream().anyMatch(p -> p.extensionNameString().equals(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME))) {
				extensions.put(stack.UTF8(VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME));
			}
			
			PointerBuffer ppEnabledLayerNames = null;
			if (VulkanTriangles.USE_DEBUG) {
				ppEnabledLayerNames = stack.pointers(stack.UTF8(vulkanInstance.getDebugExtension()));
			}
			
			// --------------------------------------
			// Create Device
			VkDeviceCreateInfo pCreateInfo = VkDeviceCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
					.pQueueCreateInfos(pQueueCreateInfos)
					.ppEnabledExtensionNames(extensions.flip())
					.ppEnabledLayerNames(ppEnabledLayerNames);
			
			PointerBuffer pDevice = stack.mallocPointer(1);
			ret = vkCreateDevice(validPhysicalDevice, pCreateInfo, null, pDevice);
			VkUtils.check(ret, "Failed to create vulkan logical device");
			
			device = new VkDevice(pDevice.get(0), validPhysicalDevice, pCreateInfo);
			
			// --------------------------------------
			// Create queues
			if (graphicsQueueFamily == computeQueueFamily) {
				// Create queue
				PointerBuffer pQueue = stack.mallocPointer(1);
				vkGetDeviceQueue(device, graphicsQueueFamily, 0, pQueue);
				graphicsQueue = new VkQueue(pQueue.get(0), device);
				computeQueue = graphicsQueue;
				
				queuesAreSplit = false;
				Log.print("Created a suboptimal single queue");
			} else {
				// Create queues
				PointerBuffer pGraphicsQueue = stack.mallocPointer(1);
				vkGetDeviceQueue(device, graphicsQueueFamily, 0, pGraphicsQueue);
				graphicsQueue = new VkQueue(pGraphicsQueue.get(0), device);
				
				PointerBuffer pComputeQueue = stack.mallocPointer(1);
				vkGetDeviceQueue(device, computeQueueFamily, 0, pComputeQueue);
				computeQueue = new VkQueue(pComputeQueue.get(0), device);
				
				queuesAreSplit = true;
				Log.print("Created the optimal separated graphics and compute queues");
			}
			
			// --------------------------------------
			// Set memory properties and limits
			memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
			vkGetPhysicalDeviceMemoryProperties(validPhysicalDevice, memoryProperties);
			
			properties = VkPhysicalDeviceProperties.calloc();
			vkGetPhysicalDeviceProperties(validPhysicalDevice, properties);
		}
	}
	
	public boolean getMemoryType(int typeBits, int properties, IntBuffer typeIndex) {
		int bits = typeBits;
		for (int i = 0; i < 32; i++) {
			if ((bits & 1) == 1) {
				if ((memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
					typeIndex.put(0, i);
					return true;
				}
			}
			bits >>= 1;
		}
		return false;
	}
	
	public void waitIdle() {
		vkDeviceWaitIdle(device);
	}
	
	public VkDevice get() {
		return device;
	}
	
	public int getGraphicsQueueFamily() {
		return graphicsQueueFamily;
	}
	
	public int getComputeQueueFamily() {
		return computeQueueFamily;
	}
	
	public VkQueue getGraphicsQueue() {
		return graphicsQueue;
	}
	
	public VkQueue getComputeQueue() {
		return computeQueue;
	}
	
	public void free() {
		memoryProperties.free();
		properties.free();
		
		vkDestroyDevice(device, null);
	}
}
