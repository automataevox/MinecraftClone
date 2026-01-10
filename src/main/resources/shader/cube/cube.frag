#version 330 core

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

float ShadowCalculation(vec4 fragPosLightSpace) {
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    float closestDepth = texture(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;

    float bias = 0.005;
    float shadow = currentDepth - bias > closestDepth ? 0.5 : 0.0; // simple shadow
    return shadow;
}

void main() {
    vec3 color = texture(u_Texture, TexCoord).rgb;

    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(-u_LightDir);

    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * u_LightColor;

    float shadow = ShadowCalculation(FragPosLightSpace);
    vec3 lighting = (u_Ambient + (1.0 - shadow) * diffuse) * color;

    FragColor = vec4(lighting, 1.0);
}
