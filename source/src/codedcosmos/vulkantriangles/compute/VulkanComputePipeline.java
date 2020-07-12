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
import codedcosmos.vulkantriangles.graphics.VulkanShader;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanComputePipeline {
	
	private long pipeline;
	private long layout;
	private long descriptorSetLayout;
	
	private VulkanShader computeShader;
	
	public VulkanComputePipeline(VkDevice device) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// Shaders
			computeShader = new VulkanShader("shaders/compute.glsl", device, VK_SHADER_STAGE_COMPUTE_BIT);
			
			VkPipelineShaderStageCreateInfo shaderStage = VkPipelineShaderStageCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
					.flags(0)
					.stage(VK_SHADER_STAGE_COMPUTE_BIT)
					.module(computeShader.getShaderModule())
					.pName(stack.UTF8Safe("main"));
			
			// Layout
			VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.callocStack(3, stack);
			
			layoutBindings.get(0)
					.binding(0)
					.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
					.descriptorCount(1)
					.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
			
			layoutBindings.get(1)
					.binding(1)
					.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
					.descriptorCount(1)
					.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
			
			layoutBindings.get(2)
					.binding(2)
					.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
					.descriptorCount(1)
					.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
			
			VkDescriptorSetLayoutCreateInfo descriptorLayout = VkDescriptorSetLayoutCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
					.pBindings(layoutBindings);
			
			LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
			ret = vkCreateDescriptorSetLayout(device, descriptorLayout, null, pDescriptorSetLayout);
			VkUtils.check(ret, "Failed to create descriptor set layout for compute pipeline");
			descriptorSetLayout = pDescriptorSetLayout.get(0);
			
			VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
					.pSetLayouts(pDescriptorSetLayout);
			
			LongBuffer pPipelineLayout = stack.mallocLong(1);
			ret = vkCreatePipelineLayout(device, pipelineLayoutCreateInfo, null, pPipelineLayout);
			VkUtils.check(ret, "Failed to create pipeline layout");
			layout = pPipelineLayout.get(0);
			
			// Pipeline
			VkComputePipelineCreateInfo.Buffer pipelineCreateInfo = VkComputePipelineCreateInfo.callocStack(1, stack)
					.sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
					.layout(layout)
					.flags(0)
					.stage(shaderStage);
			
			// Create compute pipeline
			LongBuffer pPipeline = stack.mallocLong(1);
			ret = vkCreateComputePipelines(device, VK_NULL_HANDLE, pipelineCreateInfo, null, pPipeline);
			VkUtils.check(ret, "Failed to create vulkan compute mesh pipeline");
			pipeline = pPipeline.get(0);
			
			Log.print("Created vulkan compute pipeline");
		}
	}
	
	public long get() {
		return pipeline;
	}
	
	public long getLayout() {
		return layout;
	}
	
	public long getDescriptorSetLayout() {
		return descriptorSetLayout;
	}
	
	public void free(VkDevice device) {
		computeShader.free(device);
		
		vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
		vkDestroyPipelineLayout(device, layout, null);
		vkDestroyPipeline(device, pipeline, null);
	}
}
