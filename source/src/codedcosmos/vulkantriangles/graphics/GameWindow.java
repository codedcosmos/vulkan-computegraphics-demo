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

import codedcosmos.vulkantriangles.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkInstance;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;

public class GameWindow {
	// GLFW
	private long window;
	
	// Size
	private int width;
	private int height;
	
	private IntBuffer widthBuffer;
	private IntBuffer heightBuffer;
	
	// Surface
	private long surface;
	
	public GameWindow(VulkanInstance instance) throws VulkanException {
		// Set to no api, as vulkan will be used
		GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
		// Set to not visible, as graphics to render to the display are not ready yet
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		// Enable the window to be resized
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
		
		// Set default/starting window width & height
		// TODO Base these values on something less hardcoded
		width = 1024;
		height = 768;
		
		// Create Window
		window = GLFW.glfwCreateWindow(width, height, "Vulkan Triangles Compute/Graphics Demo - by codedcosmos", NULL, NULL);
		if (window == NULL) {
			Log.printErr("Window Failed to Create!");
			VulkanTriangles.stop();
			return;
		}
		
		// Create surface
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			LongBuffer pSurface = stack.mallocLong(1);
			ret = glfwCreateWindowSurface(instance.get(), window, null, pSurface);
			VkUtils.check(ret, "Failed to create vulkan surface");
			
			surface = pSurface.get(0);
		}
		
		// Allocate buffers
		widthBuffer = MemoryUtil.memAllocInt(1);
		heightBuffer = MemoryUtil.memAllocInt(1);
		
		// Complete
		Log.print("Created Game Window");
	}
	
	// Rendering related
	public boolean isRenderable() {
		return width > 0 && height > 0;
	}
	
	public boolean isDifferentSize(int width, int height) {
		return (this.width != width || this.height != height);
	}
	
	// GLFW related
	public void showWindow() {
		GLFW.glfwShowWindow(window);
	}
	
	public boolean shouldWindowClose() {
		return GLFW.glfwWindowShouldClose(window);
	}
	
	public void updateSize() {
		glfwGetFramebufferSize(window, widthBuffer, heightBuffer);
		width = widthBuffer.get(0);
		height = heightBuffer.get(0);
	}
	
	// Free
	public void free(VkInstance instance) {
		MemoryUtil.memFree(widthBuffer);
		MemoryUtil.memFree(heightBuffer);
		
		vkDestroySurfaceKHR(instance, surface, null);
		GLFW.glfwDestroyWindow(window);
	}
	
	// Getters
	public long get() {
		return window;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public long getSurface() {
		return surface;
	}
}
