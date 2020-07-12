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

package codedcosmos.vulkantriangles.graphics;

import codedcosmos.vulkantriangles.Log;
import codedcosmos.vulkantriangles.VkUtils;
import codedcosmos.vulkantriangles.VulkanDevice;
import codedcosmos.vulkantriangles.VulkanException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSwapchain {
	
	// Swapchain
	private long swapchain;
	
	// Images and views
	private int imageCount;
	private long[] images;
	private long[] imageViews;
	
	// Depth stencil
	private long depthStencilView;
	private long depthStencilImage;
	private long depthStencilMemory;
	
	// Frame buffers
	private long[] framebuffers;
	
	// Size
	private int width;
	private int height;
	
	// Image index
	private IntBuffer pImageIndex;
	
	// Rendering
	private PointerBuffer pCommandbuffer;
	private LongBuffer pImageAcquireSemaphore;
	private IntBuffer pWaitDstStageMask;
	private LongBuffer pRenderCompleteSemaphore;
	private LongBuffer pSwapchain;
	
	private VkSubmitInfo submitInfo;
	private VkPresentInfoKHR presentInfo;
	
	public VulkanSwapchain(VulkanDevice device, VulkanRenderPass renderPass, GameWindow window) throws VulkanException {
		// Allocate buffers
		pImageIndex = MemoryUtil.memAllocInt(1);
		
		pCommandbuffer = MemoryUtil.memAllocPointer(1);
		pImageAcquireSemaphore = MemoryUtil.memAllocLong(1);
		pWaitDstStageMask = MemoryUtil.memAllocInt(1);
		pRenderCompleteSemaphore = MemoryUtil.memAllocLong(1);
		pSwapchain = MemoryUtil.memAllocLong(1);
		
		// Allocate structs
		submitInfo = VkSubmitInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
		
		presentInfo = VkPresentInfoKHR.calloc()
				.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
		
		// Put in dstStageMask since it never changes
		pWaitDstStageMask.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
		
		recreateSwapchain(device, renderPass, window, false);
	}
	
	public void recreateSwapchain(VulkanDevice device, VulkanRenderPass renderPass, GameWindow window) throws VulkanException {
		recreateSwapchain(device, renderPass, window, true);
	}
	
	private void recreateSwapchain(VulkanDevice vulkanDevice, VulkanRenderPass vulkanRenderPass, GameWindow window, boolean built) throws VulkanException {
		int ret;
		
		// Extract
		VkDevice device = vulkanDevice.get();
		long surface = window.getSurface();
		
		long renderPass = vulkanRenderPass.get();
		
		int colorFormat = vulkanRenderPass.getColorFormat();
		int colorSpace = vulkanRenderPass.getColorSpace();
		int depthFormat = vulkanRenderPass.getDepthFormat();
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// --------------------------------------
			// Get width, height and image count
			
			// Capabilities
			VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.callocStack(stack);
			ret = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getPhysicalDevice(), surface, surfaceCapabilities);
			VkUtils.check(ret, "Failed to get physical device surface capabilities");
			
			// Width, height
			width = getDimension(window.getWidth(), surfaceCapabilities.minImageExtent().width(), surfaceCapabilities.maxImageExtent().width());
			height = getDimension(window.getHeight(), surfaceCapabilities.minImageExtent().height(), surfaceCapabilities.maxImageExtent().height());
			
			// Image count
			if (surfaceCapabilities.maxImageCount() == 0) {
				imageCount = surfaceCapabilities.minImageCount()+1;
			} else {
				imageCount = Math.min(surfaceCapabilities.minImageCount() + 1, surfaceCapabilities.maxImageCount());
			}
			
			// --------------------------------------
			// Create swapchain
			VkSwapchainCreateInfoKHR pCreateInfo = VkSwapchainCreateInfoKHR.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
					.surface(surface)
					.minImageCount(imageCount)
					.imageExtent(e -> e.set(width, height))
					.imageFormat(colorFormat)
					.imageColorSpace(colorSpace)
					.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
					.preTransform(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)
					.imageArrayLayers(1)
					.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
					.presentMode(VK_PRESENT_MODE_IMMEDIATE_KHR)
					.oldSwapchain(built ? swapchain : VK_NULL_HANDLE)
					.clipped(true)
					.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
			
			LongBuffer pSwapchain = stack.mallocLong(1);
			ret = vkCreateSwapchainKHR(device, pCreateInfo, null, pSwapchain);
			VkUtils.check(ret, built ? "Failed to recreate swapchain" : "Failed to create swapchain");
			if (built) {
				free(device);
			}
			swapchain = pSwapchain.get(0);
			this.pSwapchain.put(0, swapchain);
			
			// --------------------------------------
			// Create Depth Stencil Image View
			VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
					.imageType(VK_IMAGE_TYPE_2D)
					.format(depthFormat)
					.mipLevels(1)
					.arrayLayers(1)
					.samples(VK_SAMPLE_COUNT_1_BIT)
					.tiling(VK_IMAGE_TILING_OPTIMAL)
					.usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
			
			imageCreateInfo.extent().width(width).height(height).depth(1);
			
			VkMemoryAllocateInfo depthImageAllocateInfo = VkMemoryAllocateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
			
			VkImageViewCreateInfo depthStencilViewCreateInfo = VkImageViewCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
					.viewType(VK_IMAGE_VIEW_TYPE_2D)
					.format(depthFormat);
			depthStencilViewCreateInfo.subresourceRange()
					.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT)
					.levelCount(1)
					.layerCount(1);
			
			VkMemoryRequirements memoryRequirements = VkMemoryRequirements.callocStack(stack);
			
			LongBuffer pDepthStencilImage = stack.mallocLong(1);
			ret = vkCreateImage(device, imageCreateInfo, null, pDepthStencilImage);
			VkUtils.check(ret, "Failed to create depth-stencil image in swapchain");
			depthStencilImage = pDepthStencilImage.get(0);
			
			vkGetImageMemoryRequirements(device, depthStencilImage, memoryRequirements);
			depthImageAllocateInfo.allocationSize(memoryRequirements.size());
			IntBuffer pMemoryTypeIndex = stack.mallocInt(1);
			vulkanDevice.getMemoryType(memoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, pMemoryTypeIndex);
			depthImageAllocateInfo.memoryTypeIndex(pMemoryTypeIndex.get(0));
			
			LongBuffer pDepthStencilMemory = stack.mallocLong(1);
			ret = vkAllocateMemory(device, depthImageAllocateInfo, null, pDepthStencilMemory);
			VkUtils.check(ret, "Failed to create depth-stencil memory");
			depthStencilMemory = pDepthStencilMemory.get(0);
			
			ret = vkBindImageMemory(device, depthStencilImage, depthStencilMemory, 0);
			VkUtils.check(ret, "Failed to bind depth-stencil image to memory");
			
			depthStencilViewCreateInfo.image(depthStencilImage);
			LongBuffer pDepthStencilView = stack.mallocLong(1);
			ret = vkCreateImageView(device, depthStencilViewCreateInfo, null, pDepthStencilView);
			VkUtils.check(ret, "Failed to create depth stencil image view");
			depthStencilView = pDepthStencilView.get(0);
			
			// --------------------------------------
			// Create image and views
			IntBuffer pImageCount = stack.mallocInt(1);
			ret = vkGetSwapchainImagesKHR(device, swapchain, pImageCount, null);
			VkUtils.check(ret, "Failed to get number of swapchain images");
			imageCount = pImageCount.get(0);
			
			LongBuffer pSwapchainImages = stack.mallocLong(imageCount);
			ret = vkGetSwapchainImagesKHR(device, swapchain, pImageCount, pSwapchainImages);
			VkUtils.check(ret, "Failed to get swapchain images");
			
			images = new long[imageCount];
			imageViews = new long[imageCount];
			
			LongBuffer pBufferView = stack.mallocLong(1);
			VkImageViewCreateInfo colorAttachmentView = VkImageViewCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
					.format(colorFormat)
					.viewType(VK_IMAGE_VIEW_TYPE_2D);
			
			colorAttachmentView.subresourceRange()
					.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
					.levelCount(1)
					.layerCount(1);
			
			for (int i = 0; i < imageCount; i++) {
				images[i] = pSwapchainImages.get(i);
				colorAttachmentView.image(images[i]);
				ret = vkCreateImageView(device, colorAttachmentView, null, pBufferView);
				VkUtils.check(ret, "Failed to create image view");
				imageViews[i] = pBufferView.get(0);
			}
			
			// --------------------------------------
			// Create frame buffers
			LongBuffer pAttachments = stack.mallocLong(2);
			pAttachments.put(1, depthStencilView);
			
			VkFramebufferCreateInfo frameBufferCreateInfo = VkFramebufferCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
					.renderPass(renderPass)
					.pAttachments(pAttachments)
					.width(width)
					.height(height)
					.layers(1);
			
			framebuffers = new long[imageCount];
			LongBuffer pFramebuffer = stack.mallocLong(1);
			for (int i = 0; i < imageCount; i++) {
				pAttachments.put(0, imageViews[i]);
				ret = vkCreateFramebuffer(device, frameBufferCreateInfo, null, pFramebuffer);
				VkUtils.check(ret, "Failed to create frame buffer " + i);
				framebuffers[i] = pFramebuffer.get(0);
			}
			
			// --------------------------------------
			// Final message
			Log.print(built ? "Recreated swapchain with size of " + width + " " + height : "Created Swapchain with inital size of " + width + " " + height);
		}
	}
	
	public boolean submitAndPresent(VkDevice device, VkQueue queue, long renderFence, long imageAcquireSemaphore, long renderCompleteSemaphore, VkCommandBuffer commandBuffer) throws VulkanException {
		int ret;
		
		// Acquire image
		ret = vkAcquireNextImageKHR(device, swapchain, -1L,
				imageAcquireSemaphore, VK_NULL_HANDLE, pImageIndex);
		VkUtils.check(ret, "Failed to acquire image");
		
		// Put
		pCommandbuffer.put(0, commandBuffer);
		pImageAcquireSemaphore.put(0, imageAcquireSemaphore);
		pRenderCompleteSemaphore.put(0, renderCompleteSemaphore);
		
		// Submit
		submitInfo
				.pCommandBuffers(pCommandbuffer)
				.pWaitSemaphores(pImageAcquireSemaphore)
				.waitSemaphoreCount(1)
				.pWaitDstStageMask(pWaitDstStageMask)
				.pSignalSemaphores(pRenderCompleteSemaphore);
		
		ret = vkQueueSubmit(queue, submitInfo, renderFence);
		VkUtils.check(ret, "Failed to submit command");
		
		vkQueueWaitIdle(queue);
		
		// Present
		presentInfo
				.pWaitSemaphores(pRenderCompleteSemaphore)
				.swapchainCount(1)
				.pSwapchains(pSwapchain)
				.pImageIndices(pImageIndex)
				.pResults(null);
		
		ret = vkQueuePresentKHR(queue, presentInfo);
		
		// If the window resizes, this will occur,
		// return true to inform swapchain needs to be rebuilt
		if (ret == VK_ERROR_OUT_OF_DATE_KHR) return true;
		
		// Check
		VkUtils.check(ret, "Failed to present image");
		
		// Return false
		return false;
	}
	
	public long getImage(int idx) {
		return images[idx];
	}
	
	public long getImageView(int idx) {
		return imageViews[idx];
	}
	
	public long getFramebuffer(int idx) {
		return framebuffers[idx];
	}
	
	public long get() {
		return swapchain;
	}
	
	private int getDimension(int val, int min, int max) {
		if (min > val) {
			return min;
		}
		if (max < val) {
			return max;
		}
		return val;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getImageCount() {
		return imageCount;
	}
	
	public void free(VkDevice device) {
		MemoryUtil.memFree(pImageIndex);
		
		MemoryUtil.memFree(pCommandbuffer);
		MemoryUtil.memFree(pImageAcquireSemaphore);
		MemoryUtil.memFree(pWaitDstStageMask);
		MemoryUtil.memFree(pRenderCompleteSemaphore);
		MemoryUtil.memFree(pSwapchain);
		
		submitInfo.free();
		presentInfo.free();
		
		vkDestroySwapchainKHR(device, swapchain, null);
		for (long imageView : imageViews) {
			vkDestroyImageView(device, imageView, null);
		}
		for (long framebuffer : framebuffers) {
			vkDestroyFramebuffer(device, framebuffer, null);
		}
		
		vkFreeMemory(device, depthStencilMemory, null);
		vkDestroyImageView(device, depthStencilView, null);
		vkDestroyImage(device, depthStencilImage, null);
	}
}
