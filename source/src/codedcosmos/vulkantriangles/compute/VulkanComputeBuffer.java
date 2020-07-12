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

public class VulkanComputeBuffer {
	
	// Size
	private int size;
	private int vertexSizeBytes;
	private int indexSizeBytes;
	
	// Input
	private long inputBuffer;
	private long inputMemory;
	
	// Output
	private long vertexBuffer;
	private long vertexMemory;
	
	private long indexBuffer;
	private long indexMemory;
	
	public VulkanComputeBuffer(VulkanDevice device, float[] points) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			this.size = points.length;
			
			// ------------------
			// Input Buffer
			
			ByteBuffer dataBuffer = stack.malloc(points.length * 4);
			FloatBuffer floatBuffer = dataBuffer.asFloatBuffer();
			
			// Put data into buffer
			for (int i = 0; i < points.length; i++) {
				floatBuffer.put(points[i]);
			}
			
			// Allocate Buffer
			VkMemoryRequirements inputMemoryRequirements = VkMemoryRequirements.callocStack(stack);
			
			VkBufferCreateInfo inputBufferCreateInfo = VkBufferCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
					.size(dataBuffer.remaining())
					.usage(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT)
					.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			
			LongBuffer pInputBuffer = stack.mallocLong(1);
			ret = vkCreateBuffer(device.get(), inputBufferCreateInfo, null, pInputBuffer);
			VkUtils.check(ret, "Failed to create buffer for compute");
			inputBuffer = pInputBuffer.get(0);
			
			// Get memory requirements
			vkGetBufferMemoryRequirements(device.get(), inputBuffer, inputMemoryRequirements);
			IntBuffer inputMemoryTypeIndex = stack.mallocInt(1);
			device.getMemoryType(inputMemoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, inputMemoryTypeIndex);
			
			VkMemoryAllocateInfo inputMemoryAllocateInfo = VkMemoryAllocateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
					.allocationSize(inputMemoryRequirements.size())
					.memoryTypeIndex(inputMemoryTypeIndex.get(0));
			
			// Memory
			LongBuffer pInputMemory = stack.mallocLong(1);
			ret = vkAllocateMemory(device.get(), inputMemoryAllocateInfo, null, pInputMemory);
			VkUtils.check(ret, "Failed to allocate memory for compute");
			inputMemory = pInputMemory.get(0);
			
			// Map
			PointerBuffer pInputData = stack.mallocPointer(1);
			ret = vkMapMemory(device.get(), inputMemory, 0, dataBuffer.remaining(), 0, pInputData);
			VkUtils.check(ret, "Failed to map memory for compute");
			long data = pInputData.get(0);
			
			// Copy
			memCopy(memAddress(dataBuffer), data, dataBuffer.remaining());
			vkUnmapMemory(device.get(), inputMemory);
			
			// Bind
			ret = vkBindBufferMemory(device.get(), inputBuffer, inputMemory, 0);
			VkUtils.check(ret, "Failed to bind compute input memory");
			
			// ------------------
			// Output Vertex buffer
			
			vertexSizeBytes = size*3*3*4;
			
			// Allocate Buffer
			VkMemoryRequirements vertexMemoryRequirements = VkMemoryRequirements.callocStack(stack);
			
			VkBufferCreateInfo vertexBufferCreateInfo = VkBufferCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
					.size(vertexSizeBytes)
					.usage(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
					.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			
			LongBuffer pVertexBuffer = stack.mallocLong(1);
			ret = vkCreateBuffer(device.get(), vertexBufferCreateInfo, null, pVertexBuffer);
			VkUtils.check(ret, "Failed to create vertex buffer for compute");
			vertexBuffer = pVertexBuffer.get(0);
			
			// Get memory requirements
			vkGetBufferMemoryRequirements(device.get(), vertexBuffer, vertexMemoryRequirements);
			IntBuffer vertexMemoryTypeIndex = stack.mallocInt(1);
			device.getMemoryType(vertexMemoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, vertexMemoryTypeIndex);
			
			VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
					.allocationSize(vertexMemoryRequirements.size())
					.memoryTypeIndex(vertexMemoryTypeIndex.get(0));
			
			// Memory
			LongBuffer pVertexMemory = stack.mallocLong(1);
			ret = vkAllocateMemory(device.get(), memoryAllocateInfo, null, pVertexMemory);
			VkUtils.check(ret, "Failed to allocate vertex memory for compute");
			vertexMemory = pVertexMemory.get(0);
			
			// Bind
			ret = vkBindBufferMemory(device.get(), vertexBuffer, vertexMemory, 0);
			VkUtils.check(ret, "Failed to bind compute vertex memory");
			
			// ------------------
			// Output Index buffer
			
			indexSizeBytes = size*3*4;
			
			// Allocate Buffer
			VkMemoryRequirements indexMemoryRequirements = VkMemoryRequirements.callocStack(stack);
			
			VkBufferCreateInfo indexBufferCreateInfo = VkBufferCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
					.size(indexSizeBytes)
					.usage(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT)
					.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			
			LongBuffer pIndexBuffer = stack.mallocLong(1);
			ret = vkCreateBuffer(device.get(), indexBufferCreateInfo, null, pIndexBuffer);
			VkUtils.check(ret, "Failed to create index buffer for compute");
			indexBuffer = pIndexBuffer.get(0);
			
			// Get memory requirements
			vkGetBufferMemoryRequirements(device.get(), indexBuffer, indexMemoryRequirements);
			IntBuffer indexMemoryTypeIndex = stack.mallocInt(1);
			device.getMemoryType(indexMemoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, indexMemoryTypeIndex);
			
			VkMemoryAllocateInfo indexMemoryAllocateInfo = VkMemoryAllocateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
					.allocationSize(indexMemoryRequirements.size())
					.memoryTypeIndex(indexMemoryTypeIndex.get(0));
			
			// Memory
			LongBuffer pIndexMemory = stack.mallocLong(1);
			ret = vkAllocateMemory(device.get(), indexMemoryAllocateInfo, null, pIndexMemory);
			VkUtils.check(ret, "Failed to allocate index memory for compute");
			indexMemory = pIndexMemory.get(0);
			
			// Bind
			ret = vkBindBufferMemory(device.get(), indexBuffer, indexMemory, 0);
			VkUtils.check(ret, "Failed to bind compute index memory");
		}
	}
	
	public long getInputBuffer() {
		return inputBuffer;
	}
	
	public long getInputBufferRange() {
		return size*4;
	}
	
	public long getVertexBuffer() {
		return vertexBuffer;
	}
	
	public long getVertexBufferRange() {
		return vertexSizeBytes;
	}
	
	public long getVertexMemory() {
		return vertexMemory;
	}
	
	public long getIndexBuffer() {
		return indexBuffer;
	}
	
	public long getIndexBufferRange() {
		return indexSizeBytes;
	}
	
	public long getIndexMemory() {
		return indexMemory;
	}
	
	public void free(VkDevice device) {
		vkDestroyBuffer(device, inputBuffer, null);
		vkFreeMemory(device, inputMemory, null);
		
		vkDestroyBuffer(device, vertexBuffer, null);
		vkFreeMemory(device, vertexMemory, null);
		
		vkDestroyBuffer(device, indexBuffer, null);
		vkFreeMemory(device, indexMemory, null);
	}
}
