#version 460 core

in vec3 FragPos;
in vec3 Normal;

out vec4 FragColor;

uniform vec3 u_LightDir;    // normalized light direction
uniform vec3 u_LightColor;  // e.g., vec3(1.0, 1.0, 1.0)
uniform vec3 u_ObjectColor; // e.g., vec3(1.0, 0.5, 0.31)

void main() {
    vec3 norm = normalize(Normal);

    // --- Diffuse shading ---
    float diff = max(dot(norm, -u_LightDir), 0.0);
    vec3 diffuse = diff * u_LightColor;

    // --- Ambient shading ---
    vec3 ambient = 0.2 * u_ObjectColor; // 20% ambient brightness

    // --- Combine ambient + diffuse ---
    vec3 result = (ambient + diffuse) * u_ObjectColor;

    FragColor = vec4(result, 1.0);
}
