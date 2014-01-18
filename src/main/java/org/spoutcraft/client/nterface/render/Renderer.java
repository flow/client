/**
 * This file is part of Client, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013-2014 Spoutcraft <http://spoutcraft.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spoutcraft.client.nterface.render;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flowpowered.commons.TPSMonitor;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.matrix.Matrix3f;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3f;

import org.lwjgl.opengl.GLContext;

import org.spout.renderer.api.Camera;
import org.spout.renderer.api.GLImplementation;
import org.spout.renderer.api.GLVersioned.GLVersion;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.data.Color;
import org.spout.renderer.api.data.Uniform.ColorUniform;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
import org.spout.renderer.api.data.Uniform.Vector3Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.Context.Capability;
import org.spout.renderer.api.gl.GLFactory;
import org.spout.renderer.api.gl.Program;
import org.spout.renderer.api.gl.Shader;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.gl.VertexArray;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.model.StringModel;
import org.spout.renderer.api.util.MeshGenerator;
import org.spout.renderer.api.util.Rectangle;
import org.spout.renderer.lwjgl.LWJGLUtil;

import org.spoutcraft.client.nterface.Interface;
import org.spoutcraft.client.nterface.render.stage.RenderGUIStage;
import org.spoutcraft.client.nterface.render.stage.RenderModelsStage;
import org.spoutcraft.client.nterface.render.stage.SSAOStage;

/**
 * The default renderer. Support OpenGL 2.1 and 3.2. Can render fully textured models with normal and specular mapping, ambient occlusion (SSAO), shadow mapping, Phong shading, motion blur and edge
 * detection anti-aliasing. The default OpenGL version is 3.2.
 */
public class Renderer {
    // CONSTANTS
    private static final String WINDOW_TITLE = "Spoutcraft";
    public static final Vector2f WINDOW_SIZE = new Vector2f(1200, 800);
    public static final Vector2f SHADOW_SIZE = new Vector2f(2048, 2048);
    public static final float ASPECT_RATIO = WINDOW_SIZE.getX() / WINDOW_SIZE.getY();
    public static final float FIELD_OF_VIEW = 60;
    public static final float TAN_HALF_FOV = (float) Math.tan(Math.toRadians(FIELD_OF_VIEW) / 2);
    public static final float NEAR_PLANE = 0.1f;
    public static final float FAR_PLANE = 1000;
    public static final Vector2f PROJECTION = new Vector2f(FAR_PLANE / (FAR_PLANE - NEAR_PLANE), (-FAR_PLANE * NEAR_PLANE) / (FAR_PLANE - NEAR_PLANE));
    private static final DateFormat SCREENSHOT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    // SETTINGS
    private boolean cullBackFaces = true;
    // EFFECT UNIFORMS
    private final Vector3Uniform lightDirectionUniform = new Vector3Uniform("lightDirection", Vector3f.FORWARD);
    private final Matrix4Uniform inverseViewMatrixUniform = new Matrix4Uniform("inverseViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightViewMatrixUniform = new Matrix4Uniform("lightViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightProjectionMatrixUniform = new Matrix4Uniform("lightProjectionMatrix", new Matrix4f());
    private final Matrix4Uniform previousViewMatrixUniform = new Matrix4Uniform("previousViewMatrix", new Matrix4f());
    private final Matrix4Uniform previousProjectionMatrixUniform = new Matrix4Uniform("previousProjectionMatrix", new Matrix4f());
    private final FloatUniform blurStrengthUniform = new FloatUniform("blurStrength", 1);
    // CAMERAS
    private final Camera modelCamera = Camera.createPerspective(FIELD_OF_VIEW, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), NEAR_PLANE, FAR_PLANE);
    private final Camera lightCamera = Camera.createOrthographic(50, -50, 50, -50, -50, 50);
    private final Camera guiCamera = Camera.createOrthographic(1, 0, 1 / ASPECT_RATIO, 0, NEAR_PLANE, FAR_PLANE);
    // OPENGL VERSION AND FACTORY
    private GLVersion glVersion;
    private GLFactory glFactory;
    // CONTEXT
    private Context context;
    // RENDER LISTS
    private final List<Model> modelRenderList = new ArrayList<>();
    private final List<Model> transparentModelList = new ArrayList<>();
    private final List<Model> guiRenderList = new ArrayList<>();
    // PROGRAMS
    private final Map<String, Program> programs = new HashMap<>();
    // MATERIALS
    private final Map<String, Material> materials = new HashMap<>();
    // VERTEX ARRAYS
    private VertexArray screenVertexArray;
    // EFFECTS
    private RenderModelsStage renderModelsStage;
    private SSAOStage ssaoStage;
    private RenderGUIStage renderGUIStage;
    // MODEL PROPERTIES
    private Color solidModelColor;
    // FPS MONITOR
    private final TPSMonitor fpsMonitor = new TPSMonitor();
    private StringModel fpsMonitorModel;
    private boolean fpsMonitorStarted = false;

    public Renderer() {
        // Set the default OpenGL version to GL32
        setGLVersion(GLVersion.GL32);
    }

    /**
     * Creates the OpenGL context and initializes the internal resources for the renderer
     */
    public void init() {
        initContext();
        initPrograms();
        initVertexArrays();
        initEffects();
        initMaterials();
        addDefaultObjects();
    }

    private void initContext() {
        context = glFactory.createContext();
        context.setWindowTitle(WINDOW_TITLE);
        context.setWindowSize(WINDOW_SIZE);
        context.create();
        context.setClearColor(new Color(0, 0, 0, 0));
        context.setCamera(modelCamera);
        if (cullBackFaces) {
            context.enableCapability(Capability.CULL_FACE);
        }
        context.enableCapability(Capability.DEPTH_TEST);
        if (glVersion == GLVersion.GL32 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
            context.enableCapability(Capability.DEPTH_CLAMP);
        }
        final UniformHolder uniforms = context.getUniforms();
        uniforms.add(previousViewMatrixUniform);
        uniforms.add(previousProjectionMatrixUniform);
    }

    private void initEffects() {
        final Texture colors = createTexture(WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RGBA, InternalFormat.RGBA8);
        colors.setWrapS(WrapMode.CLAMP_TO_EDGE);
        colors.setWrapT(WrapMode.CLAMP_TO_EDGE);
        colors.setMagFilter(FilterMode.LINEAR);
        colors.setMinFilter(FilterMode.LINEAR);
        colors.create();

        final Texture normals = createTexture(WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RGBA, InternalFormat.RGBA8);
        normals.create();

        final Texture depths = createTexture(WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.DEPTH, InternalFormat.DEPTH_COMPONENT32);
        depths.setWrapS(WrapMode.CLAMP_TO_EDGE);
        depths.setWrapT(WrapMode.CLAMP_TO_EDGE);
        depths.create();

        final Texture occlusion = createTexture(WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RED, InternalFormat.R8);
        occlusion.create();

        final int blurSize = 2;
        // RENDER MODELS
        renderModelsStage = new RenderModelsStage(this);
        renderModelsStage.setColorsOutput(colors);
        renderModelsStage.setNormalsOutput(normals);
        renderModelsStage.setDepthsOutput(depths);
        renderModelsStage.create();
        // SSAO
        ssaoStage = new SSAOStage(this);
        ssaoStage.setNormalsInput(normals);
        ssaoStage.setDepthsInput(depths);
        ssaoStage.setKernelSize(8);
        ssaoStage.setNoiseSize(blurSize);
        ssaoStage.setRadius(0.5f);
        ssaoStage.setThreshold(0.15f);
        ssaoStage.setPower(2);
        ssaoStage.setOcclusionOutput(occlusion);
        ssaoStage.create();
        // RENDER GUI
        renderGUIStage = new RenderGUIStage(this);
        renderGUIStage.create();
    }

    private Texture createTexture(int width, int height, Format format, InternalFormat internalFormat) {
        final Texture texture = glFactory.createTexture();
        texture.setFormat(format);
        texture.setInternalFormat(internalFormat);
        texture.setImageData(null, width, height);
        return texture;
    }

    private void initPrograms() {
        // SOLID
        loadProgram("solid");
        // TEXTURED
        loadProgram("textured");
        /// FONT
        loadProgram("font");
        // SSAO
        loadProgram("ssao");
        // SHADOW
        loadProgram("shadow");
        // BLUR
        loadProgram("blur");
        // LIGHTING
        loadProgram("lighting");
        // MOTION BLUR
        loadProgram("motionBlur");
        // ANTI ALIASING
        loadProgram("edaa");
        // WEIGHTED SUM
        loadProgram("weightedSum");
        // TRANSPARENCY BLENDING
        loadProgram("transparencyBlending");
        // SCREEN
        loadProgram("screen");
    }

    private void loadProgram(String name) {
        final String shaderPath = "/shaders/" + glVersion.toString().toLowerCase() + "/" + name;
        // SHADERS
        final Shader vertex = glFactory.createShader();
        vertex.setSource(Renderer.class.getResourceAsStream(shaderPath + ".vert"));
        vertex.create();
        final Shader fragment = glFactory.createShader();
        fragment.setSource(Renderer.class.getResourceAsStream(shaderPath + ".frag"));
        fragment.create();
        // PROGRAM
        final Program program = glFactory.createProgram();
        program.addShader(vertex);
        program.addShader(fragment);
        program.create();
        programs.put(name, program);
    }

    private void initMaterials() {
        Material material;
        UniformHolder uniforms;
        // SOLID
        material = new Material(programs.get("solid"));
        uniforms = material.getUniforms();
        uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
        uniforms.add(new FloatUniform("specularIntensity", 1));
        uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
        materials.put("solid", material);
        // TRANSPARENCY
        material = new Material(programs.get("weightedSum"));
        uniforms = material.getUniforms();
        uniforms.add(lightDirectionUniform);
        uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
        uniforms.add(new FloatUniform("specularIntensity", 1));
        uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
        materials.put("transparency", material);
        // SCREEN
        material = new Material(programs.get("screen"));
        material.addTexture(0, renderModelsStage.getColorsOutput());
        materials.put("screen", material);
    }

    private void initVertexArrays() {
        // DEFERRED STAGE SCREEN
        screenVertexArray = glFactory.createVertexArray();
        screenVertexArray.setData(MeshGenerator.generateTexturedPlane(null, new Vector2f(2, 2)));
        screenVertexArray.create();
    }

    private void addDefaultObjects() {
        addScreen();
        addFPSMonitor();
    }

    private void addScreen() {
        guiRenderList.add(new Model(screenVertexArray, materials.get("screen")));
    }

    private void addFPSMonitor() {
        final Font ubuntu;
        try {
            ubuntu = Font.createFont(Font.TRUETYPE_FONT, Renderer.class.getResourceAsStream("/fonts/ubuntu-r.ttf"));
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return;
        }
        final StringModel sandboxModel = new StringModel(glFactory, programs.get("font"), "ClientWIPFPS0123456789-: ", ubuntu.deriveFont(Font.PLAIN, 15), WINDOW_SIZE.getFloorX());
        final float aspect = 1 / ASPECT_RATIO;
        sandboxModel.setPosition(new Vector3f(0.005, aspect / 2 + 0.315, -0.1));
        sandboxModel.setString("Client - WIP");
        guiRenderList.add(sandboxModel);
        final StringModel fpsModel = sandboxModel.getInstance();
        fpsModel.setPosition(new Vector3f(0.005, aspect / 2 + 0.285, -0.1));
        fpsModel.setString("FPS: " + fpsMonitor.getTPS());
        guiRenderList.add(fpsModel);
        fpsMonitorModel = fpsModel;
    }

    /**
     * Destroys the renderer internal resources and the OpenGL context.
     */
    public void dispose() {
        disposeEffects();
        disposePrograms();
        disposeVertexArrays();
        disposeContext();
        fpsMonitorStarted = false;
    }

    private void disposeContext() {
        // CONTEXT
        context.destroy();
    }

    private void disposeEffects() {
        // RENDER MODELS
        renderModelsStage.destroy();
        // SSAO
        ssaoStage.destroy();
        // RENDER GUI
        renderGUIStage.destroy();
    }

    private void disposePrograms() {
        for (Program program : programs.values()) {
            // SHADERS
            for (Shader shader : program.getShaders()) {
                shader.destroy();
            }
            // PROGRAM
            program.destroy();
        }
    }

    private void disposeVertexArrays() {
        // DEFERRED STAGE SCREEN
        screenVertexArray.destroy();
    }

    /**
     * Renders the models to the window.
     */
    public void render() {
        if (!fpsMonitorStarted) {
            fpsMonitor.start();
            fpsMonitorStarted = true;
        }
        // UPDATE PER-FRAME UNIFORMS
        inverseViewMatrixUniform.set(modelCamera.getViewMatrix().invert());
        lightViewMatrixUniform.set(lightCamera.getViewMatrix());
        lightProjectionMatrixUniform.set(lightCamera.getProjectionMatrix());
        blurStrengthUniform.set((float) fpsMonitor.getTPS() / Interface.TPS);
        // RENDER
        renderModelsStage.render();
        ssaoStage.render();
        renderGUIStage.render();
        // UPDATE PREVIOUS FRAME UNIFORMS
        setPreviousModelMatrices();
        previousViewMatrixUniform.set(modelCamera.getViewMatrix());
        previousProjectionMatrixUniform.set(modelCamera.getProjectionMatrix());
        // UPDATE FPS
        updateFPSMonitor();
    }

    private void setPreviousModelMatrices() {
        for (Model model : modelRenderList) {
            model.getUniforms().getMatrix4("previousModelMatrix").set(model.getMatrix());
        }
        for (Model model : transparentModelList) {
            model.getUniforms().getMatrix4("previousModelMatrix").set(model.getMatrix());
        }
    }

    private void updateFPSMonitor() {
        fpsMonitor.update();
        fpsMonitorModel.setString("FPS: " + fpsMonitor.getTPS());
    }

    public GLFactory getGLFactory() {
        return glFactory;
    }

    public GLVersion getGLVersion() {
        return glVersion;
    }

    /**
     * Sets the OpenGL version. Must be done before initializing the renderer.
     *
     * @param version The OpenGL version to use
     */
    public void setGLVersion(GLVersion version) {
        switch (version) {
            case GL20:
            case GL21:
                glFactory = GLImplementation.get(LWJGLUtil.GL21_IMPL);
                glVersion = GLVersion.GL21;
                break;
            case GL30:
            case GL31:
            case GL32:
                glFactory = GLImplementation.get(LWJGLUtil.GL32_IMPL);
                glVersion = GLVersion.GL32;
        }
    }

    public Context getContext() {
        return context;
    }

    public Program getProgram(String name) {
        return programs.get(name);
    }

    public VertexArray getScreen() {
        return screenVertexArray;
    }

    /**
     * Sets whether or not to cull the back faces of the geometry.
     *
     * @param cull Whether or not to cull the back faces
     */
    public void setCullBackFaces(boolean cull) {
        cullBackFaces = cull;
    }

    /**
     * Sets the color of solid untextured objects.
     *
     * @param color The solid color
     */
    public void setSolidColor(Color color) {
        solidModelColor = color;
    }

    /**
     * Returns the renderer camera
     *
     * @return The camera
     */
    public Camera getCamera() {
        return modelCamera;
    }

    public Camera getGUICamera() {
        return guiCamera;
    }

    /**
     * Updates the light direction and camera bounds to ensure that shadows are casted inside the cuboid defined by size.
     *
     * @param direction The light direction
     * @param position The light camera position
     * @param size The size of the cuboid that must have shadows
     */
    public void updateLight(Vector3f direction, Vector3f position, Vector3f size) {
        // Set the direction uniform
        direction = direction.normalize();
        lightDirectionUniform.set(direction);
        // Set the camera position
        lightCamera.setPosition(position);
        // Calculate the camera rotation from the direction and set
        final Quaternionf rotation = Quaternionf.fromRotationTo(Vector3f.FORWARD.negate(), direction);
        lightCamera.setRotation(rotation);
        // Calculate the transformation from the camera bounds rotation to the identity rotation (its axis aligned space)
        final Matrix3f axisAlignTransform = Matrix3f.createRotation(rotation).invert();
        // Calculate the points of the box to completely include inside the camera bounds
        size = size.div(2);
        Vector3f p6 = size;
        Vector3f p0 = p6.negate();
        Vector3f p7 = new Vector3f(-size.getX(), size.getY(), size.getZ());
        Vector3f p1 = p7.negate();
        Vector3f p4 = new Vector3f(-size.getX(), size.getY(), -size.getZ());
        Vector3f p2 = p4.negate();
        Vector3f p5 = new Vector3f(size.getX(), size.getY(), -size.getZ());
        Vector3f p3 = p5.negate();
        // Transform those points to the axis aligned space of the camera bounds
        p0 = axisAlignTransform.transform(p0);
        p1 = axisAlignTransform.transform(p1);
        p2 = axisAlignTransform.transform(p2);
        p3 = axisAlignTransform.transform(p3);
        p4 = axisAlignTransform.transform(p4);
        p5 = axisAlignTransform.transform(p5);
        p6 = axisAlignTransform.transform(p6);
        p7 = axisAlignTransform.transform(p7);
        // Calculate the new camera bounds so that the box is fully included in those bounds
        final Vector3f low = p0.min(p1).min(p2).min(p3)
                .min(p4).min(p5).min(p6).min(p7);
        final Vector3f high = p0.max(p1).max(p2).max(p3)
                .max(p4).max(p5).max(p6).max(p7);
        // Calculate the size of the new camera bounds
        size = high.sub(low).div(2);
        // Update the camera to the new bounds
        lightCamera.setProjection(Matrix4f.createOrthographic(size.getX(), -size.getX(), size.getY(), -size.getY(), -size.getZ(), size.getZ()));
    }

    /**
     * Adds a model to be rendered as a solid.
     *
     * @param model The model
     */
    public void addSolidModel(Model model) {
        model.setMaterial(materials.get("solid"));
        model.getUniforms().add(new ColorUniform("modelColor", solidModelColor));
        addModel(model);
    }

    /**
     * Adds a model to be rendered as partially transparent.
     *
     * @param model The transparent model
     */
    public void addTransparentModel(Model model) {
        model.setMaterial(materials.get("transparency"));
        model.getUniforms().add(new Matrix4Uniform("previousModelMatrix", model.getMatrix()));
        transparentModelList.add(model);
    }

    /**
     * Adds a model to the renderer.
     *
     * @param model The model to add
     */
    public void addModel(Model model) {
        model.getUniforms().add(new Matrix4Uniform("previousModelMatrix", model.getMatrix()));
        modelRenderList.add(model);
    }

    /**
     * Removes a model from the renderer.
     *
     * @param model The model to remove
     */
    public void removeModel(Model model) {
        modelRenderList.remove(model);
    }

    /**
     * Removes all the models from the renderer.
     */
    public void clearModels() {
        modelRenderList.clear();
    }

    /**
     * Returns the modifiable list of the models. Changes in this list are reflected in the renderer.
     *
     * @return The modifiable list of models
     */
    public List<Model> getModels() {
        return modelRenderList;
    }

    public List<Model> getGUIModels() {
        return guiRenderList;
    }

    /**
     * Saves a screenshot (PNG) to the directory where the program is currently running, with the current date as the file name.
     *
     * @param outputDir The directory in which to output the file
     */
    public void saveScreenshot(File outputDir) {
        final ByteBuffer buffer = context.readCurrentFrame(new Rectangle(Vector2f.ZERO, WINDOW_SIZE), Format.RGB);
        final int width = context.getWindowWidth();
        final int height = context.getWindowHeight();
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int srcIndex = (x + y * width) * 3;
                final int destIndex = (x + (height - y - 1) * width) * 3;
                data[destIndex + 2] = buffer.get(srcIndex);
                data[destIndex + 1] = buffer.get(srcIndex + 1);
                data[destIndex] = buffer.get(srcIndex + 2);
            }
        }
        try {
            ImageIO.write(image, "PNG", new File(outputDir, SCREENSHOT_DATE_FORMAT.format(Calendar.getInstance().getTime()) + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
