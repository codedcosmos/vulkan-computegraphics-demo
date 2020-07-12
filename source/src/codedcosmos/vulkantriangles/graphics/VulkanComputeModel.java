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

import org.lwjgl.system.MemoryUtil;

import java.nio.LongBuffer;

public class VulkanComputeModel implements VulkanModel {
	
	private int length;
	
	private long indexBuffer;
	private LongBuffer pVertexBuffer;
	private LongBuffer pVertexOffsets;
	
	public VulkanComputeModel(int length, long vertexBuffer, long indexBuffer) {
		this.length = length;
		this.indexBuffer = indexBuffer;
		
		pVertexBuffer = MemoryUtil.memAllocLong(1);
		pVertexBuffer.put(0, vertexBuffer);
		
		pVertexOffsets = MemoryUtil.memAllocLong(1);
		pVertexOffsets.put(0, 0L);
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
	
	public void free() {
		MemoryUtil.memFree(pVertexBuffer);
		MemoryUtil.memFree(pVertexOffsets);
	}
}