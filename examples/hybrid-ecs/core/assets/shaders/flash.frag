#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main() {
    float alpha = texture2D(u_texture, v_texCoords)[3];

    gl_FragColor = vec4(v_color.r, v_color.g, v_color.b, alpha * v_color.a);
}
