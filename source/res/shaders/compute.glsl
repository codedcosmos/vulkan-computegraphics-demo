#version 440

layout(set = 0, binding = 0) readonly buffer InBuffer
{
    vec3 positions[];
} inVars;

layout(std430, set = 0, binding = 1) buffer VertexBuffer
{
    float vertices[];
} vertex;

layout(std430, set = 0, binding = 2) buffer IndexBuffer
{
    uint indices[];
} index;

void main() {
    vec3 point = inVars.positions[gl_GlobalInvocationID.x];

    // Vertex 1
    vertex.vertices[gl_GlobalInvocationID.x*3*3 + 0*3 + 0] = point.x-0.5;
    vertex.vertices[gl_GlobalInvocationID.x*3*3 + 0*3 + 1] = point.y+0.0;
    vertex.vertices[gl_GlobalInvocationID.x*3*3 + 0*3 + 2] = point.z+0.0;

    // Vertex 2
    vertex.vertices[gl_GlobalInvocationID.x*3*3 + 1*3 + 0] = point.x+0.0;
    vertex.vertices[gl_GlobalInvocationID.x*3*3 + 1*3 + 1] = point.y+1.0;
    vertex.vertices[gl_GlobalInvocationID.x*3*3 + 1*3 + 2] = point.z+0.0;

    // Vertex 3
    vertex.vertices[gl_GlobalInvocationID.x*3*3 + 2*3 + 0] = point.x+0.5;
    vertex.vertices[gl_GlobalInvocationID.x*3*3 + 2*3 + 1] = point.y+0.0;
    vertex.vertices[gl_GlobalInvocationID.x*3*3 + 2*3 + 2] = point.z+0.0;

    // Indices
    index.indices[gl_GlobalInvocationID.x*3 + 0] = gl_GlobalInvocationID.x*3 + 0;
    index.indices[gl_GlobalInvocationID.x*3 + 1] = gl_GlobalInvocationID.x*3 + 1;
    index.indices[gl_GlobalInvocationID.x*3 + 2] = gl_GlobalInvocationID.x*3 + 2;
}