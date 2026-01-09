#version 410 core
in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;

out vec4 FragColor;

uniform sampler2D u_Texture;
uniform vec3 u_LightDir;
uniform vec3 u_LightColor;

void main() {
    vec3 norm = normalize(Normal);
    float diff = max(dot(norm, -u_LightDir), 0.0);
    vec3 texColor = texture(u_Texture, TexCoord).rgb;
    vec3 result = texColor * u_LightColor * diff;
    FragColor = vec4(result, 1.0);
}
