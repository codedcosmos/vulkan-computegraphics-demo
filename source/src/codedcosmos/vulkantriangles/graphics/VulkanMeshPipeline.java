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
import codedcosmos.vulkantriangles.VulkanException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanMeshPipeline {
	
	private long pipeline;
	private long layout;
	
	private VulkanShader vertexShader;
	private VulkanShader fragmentShader;
	
	public VulkanMeshPipeline(VkDevice device, long renderPass) throws VulkanException {
		int ret;
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			// Vertex Input State
			// Binding description
			VkVertexInputBindingDescription.Buffer bindingDescriptor = VkVertexInputBindingDescription.callocStack(1, stack)
					.binding(0)
					.stride(3 * 4)
					.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
			
			// Attribute descriptions
			// Describes memory layout and shader attribute locations
			VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.callocStack(1, stack);
			attributeDescriptions.get(0)
					.binding(0)
					.location(0)
					.format(VK_FORMAT_R32G32B32_SFLOAT)
					.offset(0);
			
			VkPipelineVertexInputStateCreateInfo vertexInputState = VkPipelineVertexInputStateCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
					.pVertexBindingDescriptions(bindingDescriptor)
					.pVertexAttributeDescriptions(attributeDescriptions);
			
			// Vertex input state
			// Describes the topology used with this pipeline
			VkPipelineInputAssemblyStateCreateInfo inputAssemblyState = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
					.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
			
			// Rasterization state
			VkPipelineRasterizationStateCreateInfo rasterizationState = VkPipelineRasterizationStateCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
					.polygonMode(VK_POLYGON_MODE_FILL)
					.cullMode(VK_CULL_MODE_NONE)
					.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
					.lineWidth(1.0f);
			
			// Color blend state
			// Describes blend modes and color masks
			VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachmentState = VkPipelineColorBlendAttachmentState.callocStack(1, stack)
					.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT); // <- RGBA
			
			VkPipelineColorBlendStateCreateInfo colorBlendState = VkPipelineColorBlendStateCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
					.pAttachments(colorBlendAttachmentState);
			
			// Viewport
			VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
					.viewportCount(1) // <- one viewport
					.scissorCount(1); // <- one scissor rectangle
			
			// Enable dynamic states
			// Describes the dynamic states to be used with this pipeline
			// Dynamic states can be set even after the pipeline has been created
			// So there is no need to create new pipelines just for changing
			// a viewport's dimensions or a scissor box
			IntBuffer pDynamicStates = stack.mallocInt(2);
			pDynamicStates.put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip();
			VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
					.pDynamicStates(pDynamicStates);
			
			// Depth and stencil state
			// Describes depth and stencil test and compare ops
			VkPipelineDepthStencilStateCreateInfo depthStencilState = VkPipelineDepthStencilStateCreateInfo.callocStack(stack)
					// No depth test/write and no stencil used
					.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
					.depthTestEnable(true)
					.depthWriteEnable(true)
					.depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL);
			depthStencilState.back()
					.failOp(VK_STENCIL_OP_KEEP)
					.passOp(VK_STENCIL_OP_KEEP)
					.compareOp(VK_COMPARE_OP_ALWAYS);
			depthStencilState.front(depthStencilState.back());
			
			// Multi sampling state
			VkPipelineMultisampleStateCreateInfo multisampleState = VkPipelineMultisampleStateCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
					.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
			
			// Load shaders
			VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.callocStack(2, stack);
			
			// Vertex
			vertexShader = new VulkanShader("shaders/mesh.vert", device, VK_SHADER_STAGE_VERTEX_BIT);
			shaderStages.get(0)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
					.stage(VK_SHADER_STAGE_VERTEX_BIT)
					.module(vertexShader.getShaderModule())
					.pName(stack.UTF8Safe("main"));
			
			// Fragment
			fragmentShader = new VulkanShader("shaders/mesh.frag", device, VK_SHADER_STAGE_FRAGMENT_BIT);
			shaderStages.get(1)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
					.stage(VK_SHADER_STAGE_FRAGMENT_BIT)
					.module(fragmentShader.getShaderModule())
					.pName(stack.UTF8Safe("main"));
			
			// Push constants
			VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.callocStack(1, stack)
					.stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
					.offset(0)
					.size(4*4*4);
			
			// Create the pipeline layout that is used to generate the rendering pipelines that
			// are based on this descriptor layout
			VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
					.pPushConstantRanges(pushConstantRanges);
			
			LongBuffer pPipelineLayout = stack.mallocLong(1);
			ret = vkCreatePipelineLayout(device, pipelineLayoutCreateInfo, null, pPipelineLayout);
			VkUtils.check(ret, "Failed to create pipeline layout");
			layout = pPipelineLayout.get(0);
			
			// Assign states
			VkGraphicsPipelineCreateInfo.Buffer pipelineCreateInfo = VkGraphicsPipelineCreateInfo.callocStack(1, stack)
					.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
					.layout(layout)
					.renderPass(renderPass)
					.pVertexInputState(vertexInputState)
					.pInputAssemblyState(inputAssemblyState)
					.pRasterizationState(rasterizationState)
					.pColorBlendState(colorBlendState)
					.pMultisampleState(multisampleState)
					.pViewportState(viewportState)
					.pDepthStencilState(depthStencilState)
					.pStages(shaderStages)
					.pDynamicState(dynamicState);
			
			// Create Rendering pipeline
			LongBuffer pPipeline = stack.mallocLong(1);
			ret = vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineCreateInfo, null, pPipeline);
			VkUtils.check(ret, "Failed to create vulkan mesh pipeline");
			pipeline = pPipeline.get(0);
			
			Log.print("Created vulkan pipeline mesh");
		}
	}
	
	public long get() {
		return pipeline;
	}
	
	public long getLayout() {
		return layout;
	}
	
	public void free(VkDevice device) {
		vertexShader.free(device);
		fragmentShader.free(device);
		
		vkDestroyPipelineLayout(device, layout, null);
		vkDestroyPipeline(device, pipeline, null);
	}
}
