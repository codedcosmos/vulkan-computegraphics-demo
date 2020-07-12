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
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanReusableCommandBuffer {
	
	// Pool
	private long pool;
	
	// Command buffers
	private VkCommandBuffer[] commandBuffers;
	private int count;
	
	public VulkanReusableCommandBuffer(VkDevice device, int queueFamily, int count) throws VulkanException {
		this.count = count;
		
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// --------------------------------------
			// Create pool
			VkCommandPoolCreateInfo createInfo = VkCommandPoolCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
					.queueFamilyIndex(queueFamily)
					.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
			
			LongBuffer pCommandPool = stack.mallocLong(1);
			ret = vkCreateCommandPool(device, createInfo, null, pCommandPool);
			VkUtils.check(ret, "Failed to create command pool for queueFamily " + queueFamily);
			
			pool = pCommandPool.get(0);
			
			Log.print("Created vulkan command pool");
			
			// --------------------------------------
			// Create pool
			commandBuffers = new VkCommandBuffer[count];
			
			VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
					.commandPool(pool)
					.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
					.commandBufferCount(count);
			
			PointerBuffer pCommandBuffer = stack.mallocPointer(4);
			ret = vkAllocateCommandBuffers(device, allocateInfo, pCommandBuffer);
			VkUtils.check(ret, "Failed to allocate render command buffers");
			
			for (int i = 0; i < count; i++) {
				commandBuffers[i] = new VkCommandBuffer(pCommandBuffer.get(i), device);
			}
		}
	}
	
	public void reset(int idx) {
		vkResetCommandBuffer(commandBuffers[idx], 0);
	}
	
	public void begin(int idx) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
			
			ret = vkBeginCommandBuffer(commandBuffers[idx], beginInfo);
			VkUtils.check(ret, "Failed to begin command buffers");
		}
	}
	
	public void end(int idx) {
		vkEndCommandBuffer(commandBuffers[idx]);
	}
	
	public VkCommandBuffer get(int idx) {
		return commandBuffers[idx];
	}
	
	public long getPool() {
		return pool;
	}
	
	public void free(VkDevice device) {
		for (VkCommandBuffer commandBuffer : commandBuffers) {
			vkFreeCommandBuffers(device, pool, commandBuffer);
		}
		
		vkDestroyCommandPool(device, pool, null);
	}
}
