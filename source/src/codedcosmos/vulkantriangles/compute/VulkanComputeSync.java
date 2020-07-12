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

package codedcosmos.vulkantriangles.compute;

import codedcosmos.vulkantriangles.VkUtils;
import codedcosmos.vulkantriangles.VulkanException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanComputeSync {
	// Sync
	private long computeFence;
	
	public VulkanComputeSync(VkDevice device) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			LongBuffer pFence = stack.mallocLong(1);
			
			VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
					.flags(VK_FENCE_CREATE_SIGNALED_BIT);
			
			ret = vkCreateFence(device, fenceCreateInfo, null, pFence);
			VkUtils.check(ret, "Failed to create compute fence");
			computeFence = pFence.get(0);
			
			vkResetFences(device, computeFence);
		}
	}
	
	public void waitForFence(VkDevice device) {
		vkWaitForFences(device, computeFence, true, Long.MAX_VALUE);
		vkResetFences(device, computeFence);
	}
	
	public long get() {
		return computeFence;
	}
	
	public void free(VkDevice device) {
		vkDestroyFence(device, computeFence, null);
	}
}
