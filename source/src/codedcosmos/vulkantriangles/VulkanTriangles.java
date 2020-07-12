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

package codedcosmos.vulkantriangles;

import codedcosmos.vulkantriangles.compute.VulkanComputeBuffer;
import codedcosmos.vulkantriangles.compute.VulkanComputeDescriptorSet;
import codedcosmos.vulkantriangles.compute.VulkanComputePipeline;
import codedcosmos.vulkantriangles.compute.VulkanComputeSync;
import codedcosmos.vulkantriangles.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanTriangles {
	// Configurable
	public static final boolean USE_DEBUG = true;
	
	// State
	private static boolean RUNNING = false;
	public static void stop() {
		RUNNING = false;
	}
	
	// Init
	public static void main(String[] args) throws VulkanException {
		RUNNING = true;
		
		// Init glfw
		if (!GLFW.glfwInit()) {
			Log.printErr("GLFW Failed to initalise GLFW");
			return;
		}
		
		// Vulkan Instance
		VulkanInstance instance = new VulkanInstance();
		
		// Create window
		GameWindow window = new GameWindow(instance);
		
		// Vulkan
		VulkanDevice vulkanDevice = new VulkanDevice(instance, window);
		
		// Vulkan Compute
		int size = 500;
		int scale = 50;
		float[] points = new float[3*size];
		Random random = new Random();
		for (int i = 0; i < points.length; i++) {
			points[i] = random.nextInt(scale*2)-scale;
		}
		VulkanComputeBuffer computeBuffer = new VulkanComputeBuffer(vulkanDevice, points);
		
		VulkanComputePipeline computePipeline = new VulkanComputePipeline(vulkanDevice.get());
		VulkanReusableCommandBuffer computeCommandBuffer = new VulkanReusableCommandBuffer(vulkanDevice.get(), vulkanDevice.getComputeQueueFamily(), 1);
		VulkanComputeDescriptorSet descriptorSet = new VulkanComputeDescriptorSet(vulkanDevice.get(), computePipeline.getDescriptorSetLayout(), computeBuffer);
		VulkanComputeSync computeSync = new VulkanComputeSync(vulkanDevice.get());
		
		
		// Run compute
		try (MemoryStack stack = MemoryStack.stackPush()) {
			vulkanDevice.waitIdle();
			
			// Record
			computeCommandBuffer.begin(0);
			
			vkCmdBindPipeline(computeCommandBuffer.get(0), VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.get());
			
			LongBuffer descriptorSets = stack.mallocLong(1).put(0, descriptorSet.getDescriptorSet());
			vkCmdBindDescriptorSets(computeCommandBuffer.get(0), VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.getLayout(), 0, descriptorSets, null);
			
			vkCmdDispatch(computeCommandBuffer.get(0), size, 1, 1);
			
			computeCommandBuffer.end(0);
			
			// Submit
			PointerBuffer pCommandbuffer = stack.mallocPointer(1);
			pCommandbuffer.put(0, computeCommandBuffer.get(0));
			
			VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
					.pCommandBuffers(pCommandbuffer);
			
			int ret = vkQueueSubmit(vulkanDevice.getComputeQueue(), submitInfo, computeSync.get());
			VkUtils.check(ret, "Failed to submit compute command");
			
			// Wait
			computeSync.waitForFence(vulkanDevice.get());
		}
		
		VulkanComputeModel computeModel = new VulkanComputeModel(size*3, computeBuffer.getVertexBuffer(), computeBuffer.getIndexBuffer());
		
		// Vulkan Graphics
		VulkanRenderPass renderPass = new VulkanRenderPass(vulkanDevice.get(), window.getSurface());
		VulkanSwapchain swapchain = new VulkanSwapchain(vulkanDevice, renderPass, window);
		VulkanReusableCommandBuffer graphicsCommandBuffer = new VulkanReusableCommandBuffer(vulkanDevice.get(), vulkanDevice.getGraphicsQueueFamily(), swapchain.getImageCount());
		VulkanFrameDirector frameDirector = new VulkanFrameDirector(vulkanDevice.get(), swapchain.getImageCount());
		
		Renderer renderer = new Renderer(vulkanDevice, renderPass.get());
		
		// Show window
		window.showWindow();
		
		// Rendering variables
		int idx = 0;
		
		while (RUNNING) {
			// GLFW events
			GLFW.glfwPollEvents();
			if (window.shouldWindowClose()) RUNNING = false;
			
			window.updateSize();
			
			// Check if renderable/minimized
			if (!window.isRenderable()) {
				continue;
			}
			
			// Check for resize
			if (window.isDifferentSize(swapchain.getWidth(), swapchain.getHeight())) {
				// Resize
				vulkanDevice.waitIdle();
				swapchain.recreateSwapchain(vulkanDevice, renderPass, window);
				
				// Reset
				idx = 0;
			}
			
			// Wait for fences
			frameDirector.waitForLastRender(vulkanDevice.get(), idx);
			
			// Record
			graphicsCommandBuffer.reset(idx);
			graphicsCommandBuffer.begin(idx);
			
			// Bind
			renderer.bind(graphicsCommandBuffer.get(idx), renderer.getCubeModel(), swapchain, renderPass.get(), swapchain.getFramebuffer(idx));
			
			// Draw cubes
			renderer.bindModel(graphicsCommandBuffer.get(idx), renderer.getCubeModel());
			
			//renderer.drawRect(graphicsCommandBuffer.get(idx), renderer.getCubeModel(), swapchain, 0f, 0f, -5f);
			//renderer.drawRect(graphicsCommandBuffer.get(idx), renderer.getCubeModel(), swapchain, 0f, 5f, -10f);
			//renderer.drawRect(graphicsCommandBuffer.get(idx), renderer.getCubeModel(), swapchain, 0f, -5f, -10f);
			
			// Draw compute model
			renderer.bindModel(graphicsCommandBuffer.get(idx), computeModel);
			renderer.drawRect(graphicsCommandBuffer.get(idx), computeModel, swapchain, 0f, 0f, -50f);
			
			// End
			vkCmdEndRenderPass(graphicsCommandBuffer.get(idx));
			
			graphicsCommandBuffer.end(idx);
			
			// Perform render
			swapchain.submitAndPresent(vulkanDevice.get(), vulkanDevice.getGraphicsQueue(), frameDirector.getRenderFence(idx), frameDirector.getImageAcquireSemaphore(idx), frameDirector.getRenderCompleteSemaphores(idx), graphicsCommandBuffer.get(idx));
			
			// Increment IDX
			idx = (idx + 1) % swapchain.getImageCount();
		}
		
		
		// Free
		vulkanDevice.waitIdle();
		
		Log.print("Freeing Vulkan compute");
		computeModel.free();
		computeSync.free(vulkanDevice.get());
		computeBuffer.free(vulkanDevice.get());
		computePipeline.free(vulkanDevice.get());
		computeCommandBuffer.free(vulkanDevice.get());
		descriptorSet.free(vulkanDevice.get());
		
		Log.print("Freeing Renderer");
		renderer.free(vulkanDevice.get());
		
		Log.print("Freeing Frame Director");
		frameDirector.free(vulkanDevice.get());
		
		Log.print("Freeing Command Buffer");
		graphicsCommandBuffer.free(vulkanDevice.get());
		
		Log.print("Freeing Swapchain");
		swapchain.free(vulkanDevice.get());
		
		Log.print("Freeing Render Pass");
		renderPass.free(vulkanDevice.get());
		
		Log.print("Freeing Device");
		vulkanDevice.free();
		
		Log.print("Freeing Window");
		window.free(instance.get());
		GLFW.glfwTerminate();
		
		Log.print("Freeing Instance");
		instance.free();
		
		Log.print("Exiting");
	}
}
