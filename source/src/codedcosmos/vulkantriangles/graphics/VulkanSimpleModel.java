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
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

public class VulkanSimpleModel implements VulkanModel {
	
	
	private int length;
	
	// Vertex
	private long vertexBuffer;
	private LongBuffer pVertexBuffer;
	
	private long vertexMemory;
	private LongBuffer pVertexOffsets;
	
	// Index
	private long indexBuffer;
	
	private long indexMemory;
	
	public VulkanSimpleModel(VulkanDevice device, float[] vertices, int[] indices) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			length = indices.length;
			
			// ------------------
			// Vertex buffer
			
			ByteBuffer vertexByteBuffer = stack.malloc(vertices.length * 4);
			FloatBuffer vertexFb = vertexByteBuffer.asFloatBuffer();
			
			// Put data in buffer
			for (int i = 0; i < vertices.length; i++) {
				vertexFb.put(vertices[i]);
			}
			
			// Allocate Buffer
			VkMemoryRequirements vertexMemoryRequirements = VkMemoryRequirements.callocStack(stack);
			
			VkBufferCreateInfo vertexBufferCreateInfo = VkBufferCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
					.size(vertexByteBuffer.remaining())
					.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
			
			pVertexBuffer = MemoryUtil.memAllocLong(1);
			ret = vkCreateBuffer(device.get(), vertexBufferCreateInfo, null, pVertexBuffer);
			VkUtils.check(ret, "Failed to create buffer for model");
			vertexBuffer = pVertexBuffer.get(0);
			
			// Get memory requirements
			vkGetBufferMemoryRequirements(device.get(), vertexBuffer, vertexMemoryRequirements);
			IntBuffer vertexMemoryTypeIndex = stack.mallocInt(1);
			device.getMemoryType(vertexMemoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, vertexMemoryTypeIndex);
			
			VkMemoryAllocateInfo vertexMemoryAllocateInfo = VkMemoryAllocateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
					.allocationSize(vertexMemoryRequirements.size())
					.memoryTypeIndex(vertexMemoryTypeIndex.get(0));
			
			// Memory
			LongBuffer pVertexMemory = stack.mallocLong(1);
			ret = vkAllocateMemory(device.get(), vertexMemoryAllocateInfo, null, pVertexMemory);
			VkUtils.check(ret, "Failed to allocate vertex model memory");
			vertexMemory = pVertexMemory.get(0);
			
			// Map
			PointerBuffer pVertexData = stack.mallocPointer(1);
			ret = vkMapMemory(device.get(), vertexMemory, 0, vertexByteBuffer.remaining(), 0, pVertexData);
			VkUtils.check(ret, "Failed to map vertex model memory");
			long vertexData = pVertexData.get(0);
			
			// Copy
			memCopy(memAddress(vertexByteBuffer), vertexData, vertexByteBuffer.remaining());
			vkUnmapMemory(device.get(), vertexMemory);
			ret = vkBindBufferMemory(device.get(), vertexBuffer, vertexMemory, 0);
			VkUtils.check(ret, "Failed to copy memory to vertex buffer");
			
			// Create offsets
			pVertexOffsets = MemoryUtil.memAllocLong(1);
			pVertexOffsets.put(0, 0L);
			
			// ------------------
			// Index buffer
			
			ByteBuffer indexByteBuffer = stack.malloc(indices.length * 4);
			IntBuffer indexIb = indexByteBuffer.asIntBuffer();
			
			// Put data in buffer
			for (int i = 0; i < indices.length; i++) {
				indexIb.put(indices[i]);
			}
			
			// Allocate Buffer
			VkMemoryAllocateInfo indexMemoryAllocateInfo = VkMemoryAllocateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
			
			VkMemoryRequirements indexMemoryRequirements = VkMemoryRequirements.callocStack(stack);
			
			VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
					.size(indexByteBuffer.remaining())
					.usage(VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
			
			LongBuffer pIndexBuffer = stack.mallocLong(1);
			ret = vkCreateBuffer(device.get(), bufferCreateInfo, null, pIndexBuffer);
			VkUtils.check(ret, "Failed to create buffer for model");
			indexBuffer = pIndexBuffer.get(0);
			
			//
			vkGetBufferMemoryRequirements(device.get(), indexBuffer, indexMemoryRequirements);
			IntBuffer memoryTypeIndex = stack.mallocInt(1);
			device.getMemoryType(indexMemoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, memoryTypeIndex);
			indexMemoryAllocateInfo
					.allocationSize(indexMemoryRequirements.size())
					.memoryTypeIndex(memoryTypeIndex.get(0));
			
			// Memory
			LongBuffer pMemory = stack.mallocLong(1);
			ret = vkAllocateMemory(device.get(), indexMemoryAllocateInfo, null, pMemory);
			VkUtils.check(ret, "Failed to allocate model memory");
			indexMemory = pMemory.get(0);
			
			// Map
			PointerBuffer pData = stack.mallocPointer(1);
			ret = vkMapMemory(device.get(), indexMemory, 0, indexByteBuffer.remaining(), 0, pData);
			VkUtils.check(ret, "Failed to map model memory");
			long data = pData.get(0);
			
			// Copy
			memCopy(memAddress(indexByteBuffer), data, indexByteBuffer.remaining());
			vkUnmapMemory(device.get(), indexMemory);
			ret = vkBindBufferMemory(device.get(), indexBuffer, indexMemory, 0);
			VkUtils.check(ret, "Failed to copy memory to vertex buffer");
			
			// ------------------
			// Finish
			Log.print("Created new vulkan model");
		}
	}
	
	@Override
	public LongBuffer getVertexPointer() {
		return pVertexBuffer;
	}
	
	@Override
	public long getIndexBuffer() {
		return indexBuffer;
	}
	
	@Override
	public LongBuffer getVertexOffsets() {
		return pVertexOffsets;
	}
	
	@Override
	public int getLength() {
		return length;
	}
	
	public void free(VkDevice device) {
		MemoryUtil.memFree(pVertexBuffer);
		MemoryUtil.memFree(pVertexOffsets);
		
		vkDestroyBuffer(device, vertexBuffer, null);
		vkFreeMemory(device, vertexMemory, null);
		
		vkDestroyBuffer(device, indexBuffer, null);
		vkFreeMemory(device, indexMemory, null);
	}
}
