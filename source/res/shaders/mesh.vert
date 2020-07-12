#version 450 core

layout(location=0) in vec3 position;

layout(push_constant) uniform PushConstants {
    mat4 final;
} pushConstants;

void main() {
    gl_Position = pushConstants.final * vec4(position, 1.0);
}