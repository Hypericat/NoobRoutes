#version 120

uniform vec2 u_circleCenter;
uniform float u_circleRadius;
uniform vec4 u_colorCircle;
uniform vec4 u_colorBorder;
uniform float u_borderThickness;

varying vec2 f_Position;

/**
 * Draw a circle at vec2 `pos` with radius `rad` and
 * color `color`.
 */
vec4 circle(vec2 frag, vec2 pos, float rad, vec4 color) {
    float d = length(pos - frag) - rad;
    float t = clamp(d, 0.0, 1.0);
    return vec4(color.rgb, min(color.a, 1.0 - t));
}

void main() {
    vec2 pos = f_Position - u_circleCenter;
    float dist = length(pos);

    float aa = 1.5 / u_circleRadius; // Adaptive AA based on circle size
    float outerAlpha = 1.0 - smoothstep(u_circleRadius - aa, u_circleRadius + aa, dist);

    float innerEdge = u_circleRadius - u_borderThickness;
    float innerMask = smoothstep(innerEdge - aa, innerEdge + aa, dist);

    vec4 finalColor = mix(u_colorCircle, u_colorBorder, innerMask);

    finalColor.a *= outerAlpha;

    gl_FragColor = finalColor;
}