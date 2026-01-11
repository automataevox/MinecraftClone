#version 330 core
uniform float u_Sunlit;
in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;
in vec4 FragPosLightSpace;
out vec4 FragColor;
uniform sampler2D u_Texture;
uniform sampler2D shadowMap;
uniform vec3 u_LightDir;
uniform vec3 u_LightColor;
uniform vec3 u_Ambient;


// Analytic ray-marched shadow for flat ground at y=0
float RaymarchShadow(vec3 origin, vec3 dir) {
    float shadow = 0.0;
    float t = 0.1;
    for (int i = 0; i < 32; i++) {
        vec3 p = origin + dir * t;
        if (p.y < 0.05) { // hit ground
            shadow = 1.0;
            break;
        }
        t += 0.2;
        if (t > 32.0) break;
    }
    return shadow;
}

void main() {
    vec3 color = texture(u_Texture, TexCoord).rgb;
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(-u_LightDir);

    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * u_LightColor;

    float shadow = RaymarchShadow(FragPos + 0.05 * lightDir, lightDir);

    // Sun color and global illumination
    vec3 sunLight = u_Sunlit * (1.0 - 0.7 * shadow) * diffuse;
    vec3 globalIllum = u_Ambient;
    vec3 lighting = (sunLight + globalIllum) * color;

    FragColor = vec4(lighting, 1.0);
}