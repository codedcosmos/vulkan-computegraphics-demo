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
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanShader {
	
	private long shaderModule;
	
	public VulkanShader(String path, VkDevice device, int stage) throws VulkanException {
		try {
			// Load raw shader source
			String shaderSrc = loadTextFile(path);
			
			// Get shaderc kind
			int shadercStage = VkUtils.vulkanShaderStageToShadercKind(stage);
			
			// Create compiler
			long compiler = shaderc_compiler_initialize();
			if (compiler == NULL) {
				throw new VulkanException("Failed to create Shaderc SPIR-V compiler");
			}
			
			// Compile and check status
			long result = shaderc_compile_into_spv(compiler, shaderSrc, shadercStage, path, "main", NULL);
			if (result == NULL) {
				throw new VulkanException("Failed to compile shader " + path + " into SPIR-V");
			}
			
			if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
				throw new VulkanException("Failed to compile shader " + path + " into SPIR-V\n" + shaderc_result_get_error_message(result));
			}
			
			// Free compiler
			shaderc_compiler_release(compiler);
			
			// Create Shader module
			ByteBuffer spirvCode = shaderc_result_get_bytes(result);
			shaderModule = createShaderModule(device, spirvCode);
			
			// Make sure to free the shaderc result handle
			shaderc_result_release(result);
		} catch (IOException e) {
			throw new VulkanException("Encountered IOException when trying to load shader: " + e.getMessage());
		}
	}
	
	private long createShaderModule(VkDevice device, ByteBuffer spirvCode) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);
			
			createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
			createInfo.pCode(spirvCode);
			
			LongBuffer pShaderModule = stack.mallocLong(1);
			
			ret = vkCreateShaderModule(device, createInfo, null, pShaderModule);
			VkUtils.check(ret, "Failed to create shader module");
			
			return pShaderModule.get(0);
		}
	}
	
	private String loadTextFile(String resource) throws IOException {
		// Load file
		URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
		if (url == null) {
			throw new IOException("Classpath resource not found: " + resource);
		}
		
		File file = new File(url.getFile());
		if (!file.isFile()) {
			throw new IOException("Cannot load folder resource");
		}
		if (!file.exists()) {
			throw new IOException("File to load doesn't exist");
		}
		
		// Load as text
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		StringBuilder builder = new StringBuilder();
		String line = br.readLine();
		
		while (line != null) {
			builder.append(line);
			builder.append(System.lineSeparator());
			line = br.readLine();
		}
		br.close();
		
		return builder.toString();
	}
	
	public long getShaderModule() {
		return shaderModule;
	}
	
	public void free(VkDevice device) {
		vkDestroyShaderModule(device, shaderModule, null);
	}
}
