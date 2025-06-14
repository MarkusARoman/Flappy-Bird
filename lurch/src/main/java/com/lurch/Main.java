package com.lurch;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Random;

import com.lurch.texture.Texture;
import com.lurch.texture.TextureConfig;
import com.lurch.texture.TextureLoader;

import com.lurch.display.Quad;
import com.lurch.display.Shader;
import com.lurch.display.Window;

import com.lurch.time.Timer;

public class Main 
{
    private Window window;
    private Shader shader;
    private Timer timer;
    private Quad quad;

    private final float virtualPixel = 1.5f;
    private final int width  = (int) (360 * virtualPixel);
    private final int height = (int) (640 * virtualPixel);

    private boolean running = false;

    private final float dt;

    // Background
    private Texture bgTex;
    private Matrix4f bgModel;

    // Bird
    private Texture birdTex;
    private Matrix4f birdModel;
    private float birdX = width / 3f;
    private float birdY = height / 2f;
    private float birdWidth = 25.5f * virtualPixel;
    private float birdHeight = 18f * virtualPixel;
    private float birdVelY = 0.0f;
    private float birdRotation = 0.0f;

    private final float GRAVITY = 750f * virtualPixel;
    private final float JUMP_FORCE = -300f * virtualPixel;

    // Ground
    private Texture groundTex;
    private Matrix4f groundModel;

    // Pipes
    private Texture pipeTex;

    private final float pipeWidth = 30f * virtualPixel;
    private final float pipeHeight = 300f * virtualPixel;

    private final int numPipes = 10;

    // Horizontal spacing can be between anywhere 50 and 150
    private final float gapX = 200f * virtualPixel;
    private final float maxOffsetX = 50f * virtualPixel;

    // Where the center of the pipes are vertically
    private final float centerY = (float) height/2f;
    private final float maxOffcenterY = (float) height/4f;

    // Vertical spacing can be between anywhere 50 and 150
    private final float gapY = 150f * virtualPixel;
    private final float maxOffsetY = 50f * virtualPixel;

    private float pipeSpeed = -100f * virtualPixel;

    private ArrayList<Pipe> pipes = new ArrayList<>();

    private Random random = new Random();

    class Pipe 
    {
        float x;
        float y1; // Bottom of top pipe
        float y2; // Top of bottom pipe

        Matrix4f topModel;
        Matrix4f bottomModel;

        public Pipe(float x, float y1, float y2)
        {
            this.x = x;
            this.y1 = y1;
            this.y2 = y2;
            updateModels();
        }

        public void updateModels()
        {
            bottomModel = new Matrix4f()
                .translate(x, y1+pipeHeight, 0f)
                .scale(pipeWidth, pipeHeight, 1f) 
                .rotate((float) Math.toRadians(180), 0f, 0f, 1f); // Flip top pipe
            topModel = new Matrix4f()
                .translate(x, y2-pipeHeight, 0f)
                .scale(pipeWidth, pipeHeight, 1f);
        }
    }

    private Pipe generatePipe(float startX) {
        float pipeGap = gapY + random.nextFloat(-1, 1) * maxOffsetY;
        float offset = centerY + random.nextFloat(-1, 1) * maxOffcenterY;
        float y1 = offset + pipeGap / 2f;
        float y2 = offset - pipeGap / 2f;
        return new Pipe(startX, y1, y2);
    }


    public Main()
    {
        window = new Window(width, height, "Flappy Bird");

        shader = new Shader("texture");
        shader.compile();

        timer = new Timer();

        quad = new Quad();

        dt =  1f / timer.getUPS(); 
    }

    public void run() 
    {
        init();
        loop();
        free();
    }

    private void init() 
    {
        System.out.println("Press 'SPACE' to start game.");

        shader.compile();
        timer.start();

        shader.bind();
        shader.setUniformMatrix4f("u_projection", window.getOrtho());
        shader.unbind();

        bgTex = TextureLoader.load("background.png", TextureConfig.OPAQUE_PIXEL_ART);
        groundTex = TextureLoader.load("ground.png", TextureConfig.OPAQUE_PIXEL_ART);
        birdTex = TextureLoader.load("bird.png", TextureConfig.PIXEL_ART);
        pipeTex = TextureLoader.load("pipe.png", TextureConfig.PIXEL_ART);

        bgModel = new Matrix4f()
            .translate(window.getWidth() / 2f, window.getHeight() / 2f, 0f)
            .scale(width / 2f, height / 2f, 1.0f);

        groundModel = new Matrix4f()
            .translate(window.getWidth() / 2f, window.getHeight() - 40f, 0f)
            .scale(width / 2f, 40f, 1.0f);

        // Initialize pipes
        float x = width + pipeWidth;
        for (int i = 0; i < numPipes; i++) {
            pipes.add(generatePipe(x));
            x += gapX + random.nextFloat() * maxOffsetX;
        }


        updateBirdModel();
    }

    private void loop() {
        while (!window.shouldClose()) {
            window.clear();

            handleInput();

            timer.update();
            if (running) {
                int updates = timer.getAccumulatedUpdates();
                for (int i = 0; i < updates; i++) 
                {
                    float collisionX = birdX + (float) Math.cos(birdRotation)*birdWidth;
                    float collisionY = birdY + (float) Math.sin(birdRotation)*birdWidth;
                    if (collisionY > height - 80f || collisionY < 0) {
                        running = false;
                        break;
                    }

                    for (int j = 0; j < numPipes; j++)
                    {
                        Pipe pipe = pipes.get(j);
                        pipe.x += pipeSpeed * dt;
                        pipe.updateModels();

                        // Check for collision
                        if (Math.abs(collisionX - pipe.x) < (pipeWidth + birdWidth) / 2f) {
                            if (pipe.y1 > collisionY && pipe.y2 < collisionY) 
                            {
                            } else 
                            {
                                running = false;
                                break;
                            }
                        }

                        // Recycle pipe if off-screen
                        if (pipe.x < -pipeWidth*2f) {
                            pipe.x += numPipes * (gapX + maxOffsetX / 2f);
                            Pipe newPipe = generatePipe(pipe.x);
                            pipe.y1 = newPipe.y1;
                            pipe.y2 = newPipe.y2;
                            pipe.updateModels();
                        }
                    }

                    if (!running) break;

                    update();
                }
            }

            timer.consume();
            render();
            window.refresh();
        }
    }

    private void free() 
    {
        window.delete();
        shader.delete();
        quad.delete();
    }

    private void update() 
    {
        birdVelY += GRAVITY * dt;
        birdY += birdVelY * dt;

        birdRotation = (float) Math.toRadians(birdVelY * 0.05f);

        updateBirdModel();
    }

    private void updateBirdModel() 
    {
        birdModel = new Matrix4f()
            .translate(birdX, birdY, 0f)
            .rotate(birdRotation, 0f, 0f, 1f)
            .scale(birdWidth, birdHeight, 1f);
    }

    private void render() 
    {
        shader.bind();

        shader.setUniform1i("u_texture", 0);

        // Background
        shader.setUniformMatrix4f("u_model", bgModel);
        bgTex.bind(0);
        quad.render();
        bgTex.unbind(0);

        // Bird
        shader.setUniformMatrix4f("u_model", birdModel);
        birdTex.bind(0);
        quad.render();
        birdTex.unbind(0);

        // Pipes
        pipeTex.bind(0);
        for (Pipe pipe : pipes) {
            shader.setUniformMatrix4f("u_model", pipe.topModel);
            quad.render();
            shader.setUniformMatrix4f("u_model", pipe.bottomModel);
            quad.render();
        }
        pipeTex.unbind(0);

        // Ground
        shader.setUniformMatrix4f("u_model", groundModel);
        groundTex.bind(0);
        quad.render();
        groundTex.unbind(0);

        shader.unbind();
    }

    private void handleInput() 
    {
        if (window.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE)) 
        {
            if (running) {
                birdVelY = JUMP_FORCE;
            } else {
                // Reset game state
                running = true;
                birdY = height / 2f;
                birdVelY = 0.0f;
                birdRotation = 0.0f;
                pipes.clear();
                float x = width + pipeWidth;
                for (int i = 0; i < numPipes; i++) {
                    pipes.add(generatePipe(x));
                    x += gapX + random.nextFloat() * maxOffsetX;
                }

            }
        }
    }

    public static void main(String[] args) 
    {
        new Main().run();
    }
}
