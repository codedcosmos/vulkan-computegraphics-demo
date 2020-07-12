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

import codedcosmos.vulkantriangles.VkUtils;
import codedcosmos.vulkantriangles.VulkanException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanFrameDirector {
	// Sync
	private long[] renderFences;
	
	private long[] imageAcquireSemaphores;
	private long[] renderCompleteSemaphores;
	
	public VulkanFrameDirector(VkDevice device, int count) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// --------------------------------------
			// Create render fences
			LongBuffer pFence = stack.mallocLong(1);
			
			VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
					.flags(VK_FENCE_CREATE_SIGNALED_BIT);
			
			renderFences = new long[count];
			
			for (int i = 0; i < count; i++) {
				ret = vkCreateFence(device, fenceCreateInfo, null, pFence);
				VkUtils.check(ret, "Failed to create fence " +i + "/" + count);
				renderFences[i] = pFence.get(0);
			}
			
			// --------------------------------------
			// Semaphore generic
			LongBuffer pSemaphore = stack.mallocLong(1);
			
			VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
			
			// --------------------------------------
			// Create image acquire semaphores
			
			imageAcquireSemaphores = new long[count];
			
			for (int i = 0; i < count; i++) {
				ret = vkCreateSemaphore(device, semaphoreCreateInfo, null, pSemaphore);
				VkUtils.check(ret, "Failed to create semaphore " +i + "/" + count);
				imageAcquireSemaphores[i] = pSemaphore.get(0);
			}
			
			// --------------------------------------
			// Create render complete semaphores
			
			renderCompleteSemaphores = new long[count];
			
			for (int i = 0; i < count; i++) {
				ret = vkCreateSemaphore(device, semaphoreCreateInfo, null, pSemaphore);
				VkUtils.check(ret, "Failed to create semaphore " +i + "/" + count);
				renderCompleteSemaphores[i] = pSemaphore.get(0);
			}
		}
	}
	
	public void waitForLastRender(VkDevice device, int idx) {
		vkWaitForFences(device, renderFences[idx], true, Long.MAX_VALUE);
		vkResetFences(device, renderFences[idx]);
	}
	
	public long getRenderFence(int idx) {
		return renderFences[idx];
	}
	
	public long getImageAcquireSemaphore(int idx) {
		return imageAcquireSemaphores[idx];
	}
	
	public long getRenderCompleteSemaphores(int idx) {
		return renderCompleteSemaphores[idx];
	}
	
	public void free(VkDevice device) {
		for (long renderFence : renderFences) {
			vkDestroyFence(device, renderFence, null);
		}
		
		for (long imageAcquireSemaphore : imageAcquireSemaphores) {
			vkDestroySemaphore(device, imageAcquireSemaphore, null);
		}
		for (long renderCompleteSemaphore : renderCompleteSemaphores) {
			vkDestroySemaphore(device, renderCompleteSemaphore, null);
		}
	}
}
