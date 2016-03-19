package com.owens.oobjloader.lwjgl;

// This code was written by myself, Sean R. Owens, sean at guild dot net,
// and is released to the public domain. Share and enjoy. Since some
// people argue that it is impossible to release software to the public
// domain, you are also free to use this code under any version of the
// GPL, LPGL, Apache, or BSD licenses, or contact me for use of another
// license.  (I generally don't care so I'll almost certainly say yes.)
// In addition this code may also be used under the "unlicense" described
// at http://unlicense.org/ .  See the file UNLICENSE in the repo.

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.owens.oobjloader.builder.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.owens.oobjloader.parser.Parse;
import org.lwjgl.util.glu.Sphere;
import org.lwjgl.util.vector.Vector3f;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

// Based on tutorial code from http://lwjgl.org/wiki/doku.php/lwjgl/tutorials/opengl/basicopengl
public class DisplayTest {

    private static Logger log = Logger.getLogger(DisplayTest.class.getName());

    public static final String WINDOW_TITLE = "Test OBJ loader";
    /**
     * Desired frame time
     */
    private static final int FRAMERATE = 60;
    private static boolean finished;

    static float radius = 1000f;
    static float phi = 0.0f;
    static float theta = 0.0f;
    static float tilda = 0.0f;
    static float oldX, oldY;
    static LinkedList<Sphere> spheres = new LinkedList<Sphere>();
    static LinkedList<Vector3f> sphereCoords = new LinkedList<Vector3f>();
    static DisplayModel scene;
    static boolean leftButtonPressed = false;
    static float pointSize = 30;
    /**
     * Application init
     *
     * @param args Commandline args
     */
    public static void main(String[] args) {

        String filename = null;
        String defaultTextureMaterial = null;

        boolean fullscreen = false;

        for (int loopi = 0; loopi < args.length; loopi++) {
            if (null == args[loopi]) {
                continue;
            }
            if (args[loopi].equals("-fullscreen")) {
                fullscreen = true;
            } else if (args[loopi].equals("-defaulttexture") && args.length >= (loopi + 1)) {
                defaultTextureMaterial = args[++loopi];
            } else {
                filename = args[loopi];
            }
        }

        try {
            init(fullscreen);
            run(filename, defaultTextureMaterial);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            Sys.alert(WINDOW_TITLE, "An error occured and the program will exit.");
        } finally {
            cleanup();
        }
        System.exit(0);
    }

    // iterate over face list from builder, and break it up into a set of face lists by material, i.e. each for each face list, all faces in that specific list use the same material
    private static ArrayList<ArrayList<Face>> createFaceListsByMaterial(Build builder) {
        ArrayList<ArrayList<Face>> facesByTextureList = new ArrayList<ArrayList<Face>>();
        Material currentMaterial = null;
        ArrayList<Face> currentFaceList = new ArrayList<Face>();
        for (Face face : builder.faces) {
            if (face.material != currentMaterial) {
                if (!currentFaceList.isEmpty()) {
                    log.log(INFO, "Adding list of " + currentFaceList.size() + " triangle faces with material " + currentMaterial + "  to our list of lists of faces.");
                    facesByTextureList.add(currentFaceList);
                }
                log.log(INFO, "Creating new list of faces for material " + face.material);
                currentMaterial = face.material;
                currentFaceList = new ArrayList<Face>();
            }
            currentFaceList.add(face);
        }
        if (!currentFaceList.isEmpty()) {
            log.log(INFO, "Adding list of " + currentFaceList.size() + " triangle faces with material " + currentMaterial + "  to our list of lists of faces.");
            facesByTextureList.add(currentFaceList);
        }
        return facesByTextureList;
    }

    // @TODO: This is a crappy way to calculate vertex normals if we are missing said normals.  I just wanted 
    // something that would add normals since my simple VBO creation code expects them.  There are better ways
    // to generate normals,  especially given that the .obj file allows specification of "smoothing groups".
    private static void calcMissingVertexNormals(ArrayList<Face> triangleList) {
        for (Face face : triangleList) {
            face.calculateTriangleNormal();
            for (int loopv = 0; loopv < face.vertices.size(); loopv++) {
                FaceVertex fv = face.vertices.get(loopv);
                if (face.vertices.get(0).n == null) {
                    FaceVertex newFv = new FaceVertex();
                    newFv.v = fv.v;
                    newFv.t = fv.t;
                    newFv.n = face.faceNormal;
                    face.vertices.set(loopv, newFv);
                }
            }
        }
    }

    // load and bind the texture we will be using as a default texture for any missing textures, unspecified textures, and/or 
    // any materials that are not textures, since we are pretty much ignoring/not using those non-texture materials.
    //
    // In general in this simple test code we are only using textures, not 'colors' or (so far) any of the other multitude of things that
    // can be specified via 'materials'. 
    private static int setUpDefaultTexture(TextureLoader textureLoader, String defaultTextureMaterial) {
        int defaultTextureID = -1;
        try {
            defaultTextureID = textureLoader.load(defaultTextureMaterial);
        } catch (IOException ex) {
            Logger.getLogger(DisplayTest.class.getName()).log(Level.SEVERE, null, ex);
            log.log(SEVERE, "Got an exception trying to load default texture material = " + defaultTextureMaterial + " , ex=" + ex);
            ex.printStackTrace();
        }
        log.log(INFO, "default texture ID = " + defaultTextureID);
        return defaultTextureID;
    }

    // Get the specified Material, bind it as a texture, and return the OpenGL ID.  Returns the default texture ID if we can't
    // load the new texture, or if the material is a non texture and hence we ignore it.  
    private static int getMaterialID(Material material, int defaultTextureID, Build builder, TextureLoader textureLoader) {
        int currentTextureID;
        if (material == null) {
            currentTextureID = defaultTextureID;
        } else if (material.mapKdFilename == null) {
            currentTextureID = defaultTextureID;
        } else {
            try {
                File objFile = new File(builder.objFilename);
                File mapKdFile = new File(objFile.getParent(), material.mapKdFilename);
                log.log(INFO, "Trying to load  " + mapKdFile.getAbsolutePath());
                currentTextureID = textureLoader.load(mapKdFile.getAbsolutePath());
            } catch (IOException ex) {
                Logger.getLogger(DisplayTest.class.getName()).log(Level.SEVERE, null, ex);
                log.log(INFO, "Got an exception trying to load  texture material = " + material.mapKdFilename + " , ex=" + ex);
                ex.printStackTrace();
                log.log(INFO, "Using default texture ID = " + defaultTextureID);
                currentTextureID = defaultTextureID;
            }
        }
        return currentTextureID;
    }

    // VBOFactory can only handle triangles, not faces with more than 3 vertices.  There are much better ways to 'triangulate' polygons, that
    // can be used on polygons with more than 4 sides, but for this simple test code justsplit quads into two triangles 
    // and drop all polygons with more than 4 vertices.  (I was originally just dropping quads as well but then I kept ending up with nothing
    // left to display. :-)  Or at least, not much. )
    private static ArrayList<Face> splitQuads(ArrayList<Face> faceList) {
        ArrayList<Face> triangleList = new ArrayList<Face>();
        int countTriangles = 0;
        int countQuads = 0;
        int countNGons = 0;
        for (Face face : faceList) {
            if (face.vertices.size() == 3) {
                countTriangles++;
                triangleList.add(face);
            } else if (face.vertices.size() == 4) {
                countQuads++;
                FaceVertex v1 = face.vertices.get(0);
                FaceVertex v2 = face.vertices.get(1);
                FaceVertex v3 = face.vertices.get(2);
                FaceVertex v4 = face.vertices.get(3);
                Face f1 = new Face();
                f1.map = face.map;
                f1.material = face.material;
                f1.add(v1);
                f1.add(v2);
                f1.add(v3);
                triangleList.add(f1);
                Face f2 = new Face();
                f2.map = face.map;
                f2.material = face.material;
                f2.add(v1);
                f2.add(v3);
                f2.add(v4);
                triangleList.add(f2);
            } else {
                countNGons++;
            }
        }
        int texturedCount = 0;
        int normalCount = 0;
        for (Face face : triangleList) {
            if ((face.vertices.get(0).n != null)
                    && (face.vertices.get(1).n != null)
                    && (face.vertices.get(2).n != null)) {
                normalCount++;
            }
            if ((face.vertices.get(0).t != null)
                    && (face.vertices.get(1).t != null)
                    && (face.vertices.get(2).t != null)) {
                texturedCount++;
            }
        }
        log.log(INFO, "Building VBO, originally " + faceList.size() + " faces, of which originally " + countTriangles + " triangles, " + countQuads + " quads,  and  " + countNGons + " n-polygons with more than 4 vertices that were dropped.");
        log.log(INFO, "Triangle list has " + triangleList.size() + " rendered triangles of which " + normalCount + " have normals for all vertices and " + texturedCount + " have texture coords for all vertices.");
        return triangleList;
    }

    /**
     * @throws Exception if init fails
     */
    private static void init(boolean fullscreen) throws Exception {
        // Create a fullscreen window with 1:1 orthographic 2D projection (default)
        Display.setTitle(WINDOW_TITLE);
        Display.setFullscreen(fullscreen);

        // Enable vsync if we can (due to how OpenGL works, it cannot be guarenteed to always work)
        Display.setVSyncEnabled(true);

        // Create default display of 640x480
        Display.setResizable(true);
        Display.setDisplayMode(new DisplayMode(1024, 768));
        Display.create();

        // double eyeX = 0 + 1000*Math.cos(1)*Math.sin(1);
        //double eyeY = 0 + 1000*Math.sin(1)*Math.sin(1);
        // double eyeZ = 0 + 1000*Math.cos(1);
        // GLU.gluLookAt((float)eyeX, (float)eyeY, (float)eyeZ, 0, 0, 0, 0, 0, 1);


        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        float fAspect = (float) Display.getDisplayMode().getWidth() / (float) Display.getDisplayMode().getHeight();
        GLU.gluPerspective(45.0f, fAspect, 0.1f, 10000f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glViewport(0, 0, Display.getDisplayMode().getWidth(), Display.getDisplayMode().getHeight());
    }

    /**
     * Runs the program (the "main loop")
     */
    private static void run(String filename, String defaultTextureMaterial) {
        scene = new DisplayModel();

        log.log(INFO, "Parsing WaveFront OBJ file");
        Build builder = new Build();
        Parse obj = null;
        try {
            obj = new Parse(builder, filename);
        } catch (java.io.FileNotFoundException e) {
            log.log(SEVERE, "Exception loading object!  e=" + e);
            e.printStackTrace();
        } catch (java.io.IOException e) {
            log.log(SEVERE, "Exception loading object!  e=" + e);
            e.printStackTrace();
        }
        log.log(INFO, "Done parsing WaveFront OBJ file");


        setUpLighting();


        double minX, minY, minZ, maxX, maxY, maxZ;

        minX = minY = minZ = Double.MAX_VALUE;
        maxX = maxY = maxZ = -Double.MAX_VALUE;

        for (VertexGeometric g : builder.verticesG) {
            if (minX > g.x)
                minX = g.x;
            if (minY > g.y)
                minY = g.y;
            if (minZ > g.z)
                minZ = g.z;

            if (maxX < g.x)
                maxX = g.x;
            if (maxY < g.y)
                maxY = g.y;
            if (maxZ < g.z)
                maxZ = g.z;
        }

        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        double centerZ = (minZ + maxZ) / 2;


        for (VertexGeometric g : builder.verticesG) {
            g.x -= centerX;
            g.y -= centerY;
            g.z -= centerZ;
        }

        log.log(INFO, "Splitting OBJ file faces into list of faces per material");
        ArrayList<ArrayList<Face>> facesByTextureList = createFaceListsByMaterial(builder);
        log.log(INFO, "Done splitting OBJ file faces into list of faces per material, ended up with " + facesByTextureList.size() + " lists of faces.");

        TextureLoader textureLoader = new TextureLoader();
        int defaultTextureID = 0;
        if (defaultTextureMaterial != null) {
            log.log(INFO, "Loading default texture =" + defaultTextureMaterial);
            defaultTextureID = setUpDefaultTexture(textureLoader, defaultTextureMaterial);
            log.log(INFO, "Done loading default texture =" + defaultTextureMaterial);
        }
        if (defaultTextureID == -1) {
            BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, 256, 256);
            g.setColor(Color.RED);
            for (int loop = 0; loop < 256; loop++) {
                g.drawLine(loop, 0, loop, 255);
                g.drawLine(0, loop, 255, loop);
            }
            defaultTextureID = textureLoader.convertToTexture(img);
        }
        int currentTextureID = -1;
        for (ArrayList<Face> faceList : facesByTextureList) {
            if (faceList.isEmpty()) {
                log.log(INFO, "ERROR: got an empty face list.  That shouldn't be possible.");
                continue;
            }
            log.log(INFO, "Getting material " + faceList.get(0).material);
            currentTextureID = getMaterialID(faceList.get(0).material, defaultTextureID, builder, textureLoader);
            log.log(INFO, "Splitting any quads and throwing any faces with > 4 vertices.");
            ArrayList<Face> triangleList = splitQuads(faceList);
            log.log(INFO, "Calculating any missing vertex normals.");
            calcMissingVertexNormals(triangleList);
            log.log(INFO, "Ready to build VBO of " + triangleList.size() + " triangles");
            ;

            if (triangleList.size() <= 0) {
                continue;
            }
            log.log(INFO, "Building VBO");

            VBO vbo = VBOFactory.build(currentTextureID, triangleList);

            log.log(INFO, "Adding VBO with text id " + currentTextureID + ", with " + triangleList.size() + " triangles to scene.");
            scene.addVBO(vbo);

        }
        log.log(INFO, "Finally ready to draw things.");

        float eyeX;
        float eyeY;
        float eyeZ;
        while (!finished) {
            //phi+=0.01f;
            //theta+=0.001f;
            eyeX = (float) (0 + radius * Math.cos(phi) * Math.sin(theta));
            eyeY = (float) (0 + radius * Math.sin(phi) * Math.sin(theta));
            eyeZ = (float) (0 + radius * Math.cos(theta));

            pollInput(eyeX, eyeY, eyeZ, builder.verticesG);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            // eyeX+=0.01f;
            // eyeY+=0.01f;
            //eyeZ+=0.01f;
            GLU.gluLookAt((float) eyeX, (float) eyeY, (float) eyeZ, 0, 0, 0, 0, 1, 0);

            // Always call Window.update(), all the time - it does some behind the
            // scenes work, and also displays the rendered output
            Display.update();

            // Check for close requests
            if (Display.isCloseRequested()) {
                finished = true;
            } // The window is in the foreground, so render!
            else if (Display.isActive()) {
                logic();
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                scene.render();
                renderSpheres();
                Display.sync(FRAMERATE);
            } // The window is not in the foreground, so we can allow other stuff to run and infrequently update
            else {
               // try {
                    //Thread.sleep(100);
               // } catch (InterruptedException e) {
                //}
                logic();

                // Only bother rendering if the window is visible or dirty
                if (Display.isVisible() || Display.isDirty()) {
                    scene.render();
                    renderSpheres();
                }
            }
        }
    }

    private static void renderSpheres() {
        int colour = 150;
        float red = (float) (colour >> 24 & 255) / 255.0F;
        float green = (float) (colour >> 16 & 255) / 255.0F;
        float blue = (float) (colour >> 8 & 255) / 255.0F;
        float alpha = (float) (colour & 255) / 255.0F;  
        
        GL11.glColor4f(255f, 0, 0, 0f);
        
        for(int i = 0; i < spheres.size(); i++) {
            GL11.glPushMatrix();
            Vector3f coords = sphereCoords.get(i);
            GL11.glTranslatef(coords.x, coords.y, coords.z);
            Sphere s = new Sphere();
            
            s.draw(3f, 16, 16);
            //log.log(INFO, coords.x + " " + coords.y + " " + coords.z);
            GL11.glPopMatrix();
        }
        GL11.glColor4f(1,1,1,1);
    }

    /**
     * Do any cleanup
     */
    private static void cleanup() {
        // Close the window
        Display.destroy();
    }

    /**
     * Do all calculations, handle input, etc.
     */
    private static void logic() {
        // Example input handler: we'll check for the ESC key and exit if it is pressed
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            finished = true;
        }
    }
    
    static float sqrDistPP3D(float[] v1, float[] v2) {
        return (v1[0] - v2[0]) * (v1[0] - v2[0]) +
               (v1[1] - v2[1]) * (v1[1] - v2[1]) +
               (v1[2] - v2[2]) * (v1[2] - v2[2]);
   }

    private static void pollInput(float cameraX, float cameraY, float cameraZ,List<VertexGeometric> vertices) {
    	
    	if(Mouse.hasWheel()){
    		    int dWheel = Mouse.getDWheel();
    		    if (dWheel < 0) {
    		       radius+=20f;
    		    } else if (dWheel > 0){
    		    	radius-=20f;
    		    }
    	}
    	
        if (Mouse.isButtonDown(0) && !leftButtonPressed) {
            leftButtonPressed = true;

            float windowWidth = Display.getDisplayMode().getWidth();
            float windowHeight = Display.getDisplayMode().getHeight();

            int screenX = Mouse.getX();
            int screenY = Mouse.getY();

            IntBuffer selectionBuffer = BufferUtils.createIntBuffer(100);
            GL11.glSelectBuffer(selectionBuffer);
            GL11.glRenderMode(GL11.GL_SELECT);
            GL11.glInitNames();

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();

            IntBuffer viewport = BufferUtils.createIntBuffer(4);
            viewport.put(new int[]{0, 0, (int) windowWidth, (int) windowHeight});
            viewport.flip();

            GLU.gluPickMatrix(screenX, screenY, 5f, 5f, viewport);

            GLU.gluPerspective(45.0f, windowWidth / windowHeight, 0.1f, 10000f);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();

            // Render The NAMES for the Vertices
            // on the NAME STACK
            render(cameraX, cameraY, cameraZ, vertices);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            // Had to ADD the following to Pop the Saved ModelView Matrix
            // back off the matrix stack
            GL11.glPopMatrix();
            int hits = GL11.glRenderMode(GL11.GL_RENDER);
            processHits(hits, selectionBuffer, new Vector3f(cameraX, cameraY, cameraZ), vertices);

            log.log(INFO, "LEFT BUTTON PRESSED");
        } else if(!Mouse.isButtonDown(0)){
            leftButtonPressed = false;
        }
        if (Mouse.isButtonDown(1)) {
            int x = Mouse.getX();
            int y = Mouse.getY();
            if (true) {
                //you might need to adjust this multiplier(0.01)
                theta += (x - oldX) * 0.01f;
                phi += (y - oldY) * 0.01f;

            }
            oldX = x;
            oldY = y;
            //glutPostRedisplay();
            // System.out.println("MOUSE DOWN @ X: " + x + " Y: " + y);
        } else {
            oldX = oldY = 0;
        }
    }

    private static void render(float cameraX, float cameraY, float cameraZ, List<VertexGeometric> vertices) {
        //GLU.gluLookAt((float) cameraX, (float) cameraY, (float) cameraZ, 0, 0, 0, 0, 1, 0);
        //scene.render();

        GL11.glPointSize(pointSize);
        int s = vertices.size();
        for (int i = 0; i < s; i++) {
            VertexGeometric v = vertices.get(i);
            GL11.glPushName(i);
            GL11.glBegin(GL11.GL_POINTS);
            GL11.glVertex3f(v.x, v.y, v.z);
            GL11.glEnd();
            GL11.glPopName();
        }
    }

    private static void processHits(int hits, IntBuffer selection, Vector3f camera, List<VertexGeometric> vertices) {
        if(hits == 0)
            return;

        VertexGeometric closest = new VertexGeometric(0, 0, 0);
        double distance = Integer.MAX_VALUE;

        if(hits < 0)
            hits = selection.capacity() / 4;

        for(int i = 0; i < hits; i++) {
            int numOfV = selection.get();
            selection.get();
            selection.get();

            for(int j = 0; j < numOfV; j++) {
                try {
                    VertexGeometric v = vertices.get(selection.get());
                    if (v.x == 0 && v.y == 0 && v.z == 0)
                        continue;
                    double dist = Math.sqrt(
                            Math.pow(camera.x - v.x, 2) +
                                    Math.pow(camera.y - v.y, 2) +
                                    Math.pow(camera.z - v.z, 2));

                    if (dist < distance) {
                        distance = dist;
                        closest = v;
                    }
                } catch (Exception e) { }
            }
        }

        log.log(INFO, hits + " points found: " + closest.x + " " + closest.y + " " + closest.z);

        if(spheres.size() > 1) {
            spheres.poll();
            sphereCoords.poll();
        }
        spheres.add(new Sphere());
        sphereCoords.add(new Vector3f(closest.x, closest.y, closest.z));
    }

    private static void setUpLighting() {
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT0);
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, asFlippedFloatBuffer(new float[]{1000, 1000, -10, 1}));
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE);
    }

    public static FloatBuffer asFlippedFloatBuffer(float... values) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values);
        buffer.flip();
        return buffer;
    }
}
