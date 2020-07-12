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

import codedcosmos.vulkantriangles.ResourceUtils;
import codedcosmos.vulkantriangles.VulkanDevice;
import codedcosmos.vulkantriangles.VulkanException;
import org.joml.Matrix4f;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class Renderer {
	
	private VkRenderPassBeginInfo renderPassBeginInfo;
	
	private VkClearValue.Buffer clearValues;
	private VkViewport.Buffer viewport;
	private VkRect2D.Buffer scissor;
	
	private VulkanMeshPipeline meshPipeline;
	private VulkanSimpleModel model;
	
	public Renderer(VulkanDevice device, long renderPass) throws VulkanException {
		// Clear Values
		clearValues = VkClearValue.calloc(2);
		clearValues.get(0).color()
				.float32(0, 0f)
				.float32(1, 0.23f)
				.float32(2, 0.9f)
				.float32(3, 1f);
		
		clearValues.get(1)
				.depthStencil()
				.depth(1.0f)
				.stencil(0);
		
		// Render Pass info
		renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
		
		// Viewport
		viewport = VkViewport.calloc(1)
				.minDepth(0f)
				.maxDepth(1f);
		
		// Scissor
		scissor = VkRect2D.calloc(1);
		
		// Vulkan
		meshPipeline = new VulkanMeshPipeline(device.get(), renderPass);
		model = new VulkanSimpleModel(device, ResourceUtils.getCubeVertices(), ResourceUtils.getCubeIndices());
	}
	
	public void bind(VkCommandBuffer commandBuffer, VulkanModel model, VulkanSwapchain swapchain, long renderPass, long frameBuffer) {
		// Render pass
		renderPassBeginInfo
				.renderPass(renderPass)
				.pClearValues(clearValues)
				.framebuffer(frameBuffer)
				.renderArea(a -> a.extent().set(swapchain.getWidth(), swapchain.getHeight()));
		
		vkCmdBeginRenderPass(commandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
		
		// Viewport state
		viewport
				.width(swapchain.getWidth())
				.height(swapchain.getHeight());
		
		vkCmdSetViewport(commandBuffer, 0, viewport);
		
		// Scissor state
		scissor.extent().set(swapchain.getWidth(), swapchain.getHeight());
		scissor.offset().set(0, 0);
		vkCmdSetScissor(commandBuffer, 0, scissor);
		
		// Bind
		vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, meshPipeline.get());
	}
	
	public void bindModel(VkCommandBuffer commandBuffer, VulkanModel model) {
		vkCmdBindVertexBuffers(commandBuffer, 0, model.getVertexPointer(), model.getVertexOffsets());
		vkCmdBindIndexBuffer(commandBuffer, model.getIndexBuffer(), 0, VK_INDEX_TYPE_UINT32);
	}
	
	public void free(VkDevice device) {
		clearValues.free();
		renderPassBeginInfo.free();
		viewport.free();
		scissor.free();
		
		meshPipeline.free(device);
		model.free(device);
	}
	
	public VulkanModel getCubeModel() {
		return model;
	}
	
	public void drawRect(VkCommandBuffer commandBuffer, VulkanModel model, VulkanSwapchain swapchain, float x, float y, float z) {
		// Push constants
		Matrix4f projection_mat = new Matrix4f().identity();
		float aspectRatio = (float)swapchain.getWidth() / (float)swapchain.getHeight();
		projection_mat.setPerspective((float) Math.toRadians(70), aspectRatio, 0.01f, 1000.0f);
		
		Matrix4f transformObject_mat = new Matrix4f().identity();
		transformObject_mat.translate(x, y, z);
		
		Matrix4f finalmat = new Matrix4f().identity();
		finalmat.mul(projection_mat).mul(transformObject_mat);
		
		float[] finalf = new float[4*4];
		finalmat.get(finalf);
		vkCmdPushConstants(commandBuffer, meshPipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, finalf);
		
		// Draw
		vkCmdDrawIndexed(commandBuffer, model.getLength(), 1, 0, 0, 0);
	}
}
