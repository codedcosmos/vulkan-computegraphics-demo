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
import codedcosmos.vulkantriangles.VulkanException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderPass {
	
	private long renderPass;
	
	private int colorFormat;
	private int colorSpace;
	private int depthFormat;
	
	public VulkanRenderPass(VkDevice device, long surface) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// --------------------------------------
			// Determine color and depth from surface
			// Determine color formats
			IntBuffer colorCount = stack.mallocInt(1);
			ret = vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), surface, colorCount, null);
			VkUtils.check(ret, "Failed to get device surface formats count");
			
			VkSurfaceFormatKHR.Buffer pSurfaceFormats = VkSurfaceFormatKHR.callocStack(colorCount.get(0), stack);
			ret = vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), surface, colorCount, pSurfaceFormats);
			VkUtils.check(ret, "Failed to get device surface formats");
			
			if (pSurfaceFormats.remaining() == 1 && pSurfaceFormats.get(0).format() == VK_FORMAT_UNDEFINED) {
				colorFormat = VK_FORMAT_R8G8B8A8_UNORM;
			} else {
				colorFormat = pSurfaceFormats.get(0).format();
			}
			colorSpace = pSurfaceFormats.get(0).colorSpace();
			
			// Determine depth format
			int[] depthFormats = new int[] {
					VK_FORMAT_D32_SFLOAT_S8_UINT,
					VK_FORMAT_D32_SFLOAT,
					VK_FORMAT_D24_UNORM_S8_UINT,
					VK_FORMAT_D16_UNORM_S8_UINT,
					VK_FORMAT_D16_UNORM
			};
			
			depthFormat = 0;
			
			VkFormatProperties formatProps = VkFormatProperties.callocStack(stack);
			for (int format : depthFormats) {
				vkGetPhysicalDeviceFormatProperties(device.getPhysicalDevice(), format, formatProps);
				// Format must support depth stencil attachment for optimal tiling
				if ((formatProps.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
					depthFormat = format;
					break;
				}
			}
			
			if (depthFormat == 0) {
				throw new VulkanException("Failed to find suitable depth format for surface");
			}
			
			// --------------------------------------
			// Create attachments
			VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(2, stack);
			
			attachments.get(0) // <- Color Attachment
					.format(colorFormat)
					.samples(VK_SAMPLE_COUNT_1_BIT)
					.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
					.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
					.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
					.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
					.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
					.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
			
			attachments.get(1) // <- depth-stencil attachment
					.format(depthFormat)
					.samples(VK_SAMPLE_COUNT_1_BIT)
					.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
					.storeOp(VK_ATTACHMENT_STORE_OP_STORE)
					.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
					.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
					.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
					.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
			
			VkAttachmentReference.Buffer colorReference = VkAttachmentReference.callocStack(1, stack)
					.attachment(0)
					.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
			
			VkAttachmentReference depthReference = VkAttachmentReference.callocStack(stack)
					.attachment(1)
					.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
			
			VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack)
					.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
					.colorAttachmentCount(colorReference.remaining())
					.pColorAttachments(colorReference)
					.pDepthStencilAttachment(depthReference);
			
			// --------------------------------------
			// Create render pass
			VkRenderPassCreateInfo renderPassCreateInfo = VkRenderPassCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
					.pAttachments(attachments)
					.pSubpasses(subpass);
			
			LongBuffer pRenderPass = stack.mallocLong(1);
			ret = vkCreateRenderPass(device, renderPassCreateInfo, null, pRenderPass);
			VkUtils.check(ret, "Failed to create render pass");
			renderPass = pRenderPass.get(0);
			
			Log.print("Created raster renderPass");
		}
	}
	
	public long get() {
		return renderPass;
	}
	
	public void free(VkDevice device) {
		vkDestroyRenderPass(device, renderPass, null);
	}
	
	public int getColorFormat() {
		return colorFormat;
	}
	
	public int getDepthFormat() {
		return depthFormat;
	}
	
	public int getColorSpace() {
		return colorSpace;
	}
}