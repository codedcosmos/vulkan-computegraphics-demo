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

import codedcosmos.vulkantriangles.Log;
import codedcosmos.vulkantriangles.VkUtils;
import codedcosmos.vulkantriangles.VulkanException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanComputeDescriptorSet {
	
	private long descriptorPool;
	private long descriptorSet;
	
	public VulkanComputeDescriptorSet(VkDevice device, long descriptorSetLayout, VulkanComputeBuffer buffer) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// ------------------
			// Descriptor Pool
			
			VkDescriptorPoolSize.Buffer typeCounts = VkDescriptorPoolSize.callocStack(1, stack)
					.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
					.descriptorCount(3);
			
			VkDescriptorPoolCreateInfo descriptorPoolInfo = VkDescriptorPoolCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
					.pPoolSizes(typeCounts)
					.maxSets(1);
			
			LongBuffer pDescriptorPool = stack.mallocLong(1);
			ret = vkCreateDescriptorPool(device, descriptorPoolInfo, null, pDescriptorPool);
			VkUtils.check(ret, "Failed to create descriptor pool");
			descriptorPool = pDescriptorPool.get(0);
			
			// ------------------
			// Create Descriptor Sets
			
			LongBuffer pDescriptorSetLayout = stack.mallocLong(1).put(0, descriptorSetLayout);
			
			VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.calloc()
					.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
					.descriptorPool(descriptorPool)
					.pSetLayouts(pDescriptorSetLayout);
			
			LongBuffer pDescriptorSet = stack.mallocLong(1);
			ret = vkAllocateDescriptorSets(device, allocateInfo, pDescriptorSet);
			VkUtils.check(ret, "Failed to allocate descriptor set");
			descriptorSet = pDescriptorSet.get(0);
			
			// ------------------
			// Write Descriptor Sets
			
			// Binding 0 Input
			VkDescriptorBufferInfo.Buffer inputDescriptor = VkDescriptorBufferInfo.calloc(1)
					.buffer(buffer.getInputBuffer())
					.range(buffer.getInputBufferRange())
					.offset(0L);
			
			VkWriteDescriptorSet.Buffer inputWriteDescriptorSet = VkWriteDescriptorSet.calloc(1)
					.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
					.dstSet(descriptorSet)
					.descriptorCount(1)
					.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
					.pBufferInfo(inputDescriptor)
					.dstBinding(0);
			
			vkUpdateDescriptorSets(device, inputWriteDescriptorSet, null);
			
			// Binding 1 Output vertices
			VkDescriptorBufferInfo.Buffer vertexDescriptor = VkDescriptorBufferInfo.calloc(1)
					.buffer(buffer.getVertexBuffer())
					.range(buffer.getVertexBufferRange())
					.offset(0L);
			
			VkWriteDescriptorSet.Buffer vertexWriteDescriptorSet = VkWriteDescriptorSet.calloc(1)
					.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
					.dstSet(descriptorSet)
					.descriptorCount(1)
					.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
					.pBufferInfo(vertexDescriptor)
					.dstBinding(1);
			
			vkUpdateDescriptorSets(device, vertexWriteDescriptorSet, null);
			
			// Binding 2 Output indices
			VkDescriptorBufferInfo.Buffer indexDescriptor = VkDescriptorBufferInfo.calloc(1)
					.buffer(buffer.getIndexBuffer())
					.range(buffer.getIndexBufferRange())
					.offset(0L);
			
			VkWriteDescriptorSet.Buffer indexWriteDescriptorSet = VkWriteDescriptorSet.calloc(1)
					.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
					.dstSet(descriptorSet)
					.descriptorCount(1)
					.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
					.pBufferInfo(indexDescriptor)
					.dstBinding(2);
			
			vkUpdateDescriptorSets(device, indexWriteDescriptorSet, null);
		}
	}
	
	public long getDescriptorSet() {
		return descriptorSet;
	}
	
	public void free(VkDevice device) {
		//vkFreeDescriptorSets(device, descriptorPool, descriptorSet);
		vkDestroyDescriptorPool(device, descriptorPool, null);
	}
}
