package com.graphics; // Coloca la aplicación en el paquete usado por los ejemplos originales.

import java.util.HashMap; // Permite guardar las ubicaciones de las variables del shader.
import java.util.Map; // Define el tipo del diccionario de ubicaciones.
import org.lwjgl.glfw.Callbacks; // Permite liberar los callbacks de la ventana al cerrar.
import org.lwjgl.glfw.GLFWErrorCallback; // Muestra los errores de GLFW en la consola.
import org.lwjgl.opengl.GL; // Carga las funciones OpenGL disponibles en el equipo.
import static org.lwjgl.glfw.GLFW.*; // Importa las funciones y constantes de ventanas y teclado.
import static org.lwjgl.opengl.GL33.*; // Importa las funciones OpenGL hasta la versión 3.3.

/**
 * CLASE 1: CREAR LA CIUDAD AMPLIADA (11x11).
 * Cumple los requisitos del examen: red vial conectada, 16 edificios y 9 parques.
 * Coordenadas: X = izquierda/derecha; Y = altura; Z = profundidad.
 */
public class clase1 {

    // ==================== 1. VARIABLES DE LA APLICACIÓN ====================
    protected long ventana; // Identificador que GLFW asigna a la ventana.
    protected int programa; // Identificador del programa que une ambos shaders.
    protected int vao; // Identificador del VAO: describe cómo leer los vértices.
    protected int vbo; // Identificador del VBO: contiene los vértices en la GPU.
    protected int ancho = 1100; // Ancho inicial de la ventana; después contiene píxeles del framebuffer.
    protected int alto = 760; // Alto inicial de la ventana; después contiene píxeles del framebuffer.
    protected float orbita = 0.6f; // Ángulo inicial de la cámara alrededor de la ciudad, en radianes.
    protected boolean vistaMapa = false; // Clase4 lo activa cuando dibuja la vista superior.
    protected static final float LIMITE = 55; // 11 celdas de 10 unidades: el mapa mide 110x110 (origen a borde: 55).
    protected static final float CELDA = 10; // Ancho y profundidad de cada celda del mapa.
    private final Map<String, Integer> uniforms = new HashMap<>(); // Evita buscar repetidamente el mismo uniform.

    // Matriz 11x11: 0 = calle, 1 = edificio (16 manzanas), 2 = parque (9 manzanas). Red vial 100% conectada.
    protected static final int[][] MAPA = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 0: Avenida perimetral norte
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0}, // Fila 1: Manzanas (Edificios y Parque noreste)
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 2: Calle transversal
        {0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0}, // Fila 3: Manzanas (Edificios y Parques intermedios)
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 4: Calle transversal
        {0, 2, 0, 1, 0, 2, 0, 1, 0, 2, 0}, // Fila 5: Eje central con gran parque central y esquinas verdes
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 6: Calle transversal
        {0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0}, // Fila 7: Manzanas (Edificios y Parques intermedios)
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 8: Calle transversal
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0}, // Fila 9: Manzanas (Edificios y Parque sur)
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}  // Fila 10: Avenida perimetral sur
    };

    // ==================== 2. INICIO, CICLO Y LIMPIEZA ====================

    public void run() {
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err);
        errores.set();
        try {
            iniciar();
            loop();
        } finally {
            limpiar();
            glfwSetErrorCallback(null);
            errores.free();
        }
    }

    private void iniciar() {
        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        ventana = glfwCreateWindow(ancho, alto, "Ciudad Interactiva 11x11 | Flechas: orbitar | ESC: salir", 0, 0);
        if (ventana == 0) {
            throw new IllegalStateException("No se pudo crear la ventana OpenGL");
        }
        glfwMakeContextCurrent(ventana);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glfwSetKeyCallback(ventana, (ventanaEvento, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                tecla(key);
            }
        });
        crearPrograma();
        crearCubo();
    }

    private void loop() {
        double tiempoAnterior = glfwGetTime();
        int cuadros = 0;
        int maxCuadros = Integer.getInteger("demo.frames", 0);
        int[] anchoReal = new int[1];
        int[] altoReal = new int[1];

        while (!glfwWindowShouldClose(ventana)) {
            glfwPollEvents();
            double tiempoActual = glfwGetTime();
            float deltaTime = (float) Math.min(tiempoActual - tiempoAnterior, 0.05);
            tiempoAnterior = tiempoActual;
            actualizar(deltaTime);
            glfwGetFramebufferSize(ventana, anchoReal, altoReal);
            ancho = anchoReal[0];
            alto = altoReal[0];
            if (ancho > 0 && alto > 0) {
                dibujarFrame();
            }
            glfwSwapBuffers(ventana);
            cuadros++;
            if (maxCuadros > 0 && cuadros >= maxCuadros) {
                glfwSetWindowShouldClose(ventana, true);
            }
        }
    }

    private void limpiar() {
        if (programa != 0) {
            glDeleteProgram(programa);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (ventana != 0) {
            Callbacks.glfwFreeCallbacks(ventana);
            glfwDestroyWindow(ventana);
        }
        glfwTerminate();
    }

    // ==================== 3. TECLADO Y CÁMARA ====================

    protected void tecla(int key) {
        if (key == GLFW_KEY_ESCAPE) {
            glfwSetWindowShouldClose(ventana, true);
        }
    }

    protected boolean pulsada(int key) {
        return glfwGetKey(ventana, key) == GLFW_PRESS;
    }

    protected void actualizar(float deltaTime) {
        if (pulsada(GLFW_KEY_LEFT)) {
            orbita -= deltaTime;
        }
        if (pulsada(GLFW_KEY_RIGHT)) {
            orbita += deltaTime;
        }
    }

    /** Cámara ajustada para el volumen ampliado de la ciudad (radio 100, altura 75). */
    protected void configurarCamara() {
        float camaraX = (float) Math.sin(orbita) * 100;
        float camaraZ = (float) Math.cos(orbita) * 100;
        vector("uOjo", camaraX, 75, camaraZ);
        vector("uObjetivo", 0, 0, 0);
        decimal("uAspecto", (float) ancho / alto);
    }

    protected void prepararLuces() {
    }

    // ==================== 4. CIUDAD A PARTIR DE UNA MATRIZ ====================

    protected static float centro(int indice) {
        return -LIMITE + CELDA * (indice + 0.5f);
    }

    protected void dibujarFrame() {
        glViewport(0, 0, ancho, alto);
        glClearColor(0.12f, 0.20f, 0.30f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(programa);
        glBindVertexArray(vao);
        entero("uMapa", 0);
        configurarCamara();
        prepararLuces();
        escena();
        if (glGetError() != GL_NO_ERROR) {
            throw new IllegalStateException("Error OpenGL al dibujar");
        }
    }

    /** Dibuja la base de asfalto ampliada (110x110) y las 121 parcelas de la ciudad. */
    protected void escena() {
        caja(0, -0.25f, 0, 110, 0.5f, 110, 0.16f, 0.19f, 0.23f); // Suelo asfáltico completo de 110x110.
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                float x = centro(columna);
                float z = centro(fila);
                int tipo = MAPA[fila][columna];
                if (tipo == 0) {
                    dibujarMarcasCalle(fila, columna, x, z);
                } else {
                    caja(x, 0.15f, z, 10, 0.3f, 10, 0.60f, 0.64f, 0.66f); // Acera de 10x10.
                    if (tipo == 1) {
                        float altura = 6 + (fila * 4 + columna * 7) % 11; // Alturas variables entre 6 y 16.
                        float rojo = 0.25f + (columna % 5) * 0.08f;
                        float azul = 0.35f + (fila % 5) * 0.08f;
                        caja(x, altura / 2 + 0.3f, z, 7, altura, 7, rojo, 0.40f, azul);
                        caja(x, altura + 0.45f, z, 7.3f, 0.3f, 7.3f, 0.20f, 0.26f, 0.32f);
                    } else {
                        caja(x, 0.32f, z, 9, 0.1f, 9, 0.20f, 0.45f, 0.28f); // Césped del parque.
                    }
                }
            }
        }
    }

    private void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        if (fila % 2 == 0 && columna % 2 == 1) {
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) {
                caja(x + desplazamiento, 0.025f, z, 1.6f, 0.03f, 0.13f, 1, 0.84f, 0.35f);
            }
        }
        if (columna % 2 == 0 && fila % 2 == 1) {
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) {
                caja(x, 0.025f, z + desplazamiento, 0.13f, 0.03f, 1.6f, 1, 0.84f, 0.35f);
            }
        }
    }

    // ==================== 5. DIBUJAR CAJAS Y ENVIAR UNIFORMS ====================

    protected void caja(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0);
    }

    protected void cajaGirada(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float angulo) {
        vector("uPos", x, y, z);
        vector("uEscala", sx, sy, sz);
        vector("uColor", r, g, b);
        decimal("uGiro", angulo);
        glDrawArrays(GL_TRIANGLES, 0, 36);
    }

    protected int uniform(String nombre) {
        if (!uniforms.containsKey(nombre)) {
            int ubicacion = glGetUniformLocation(programa, nombre);
            uniforms.put(nombre, ubicacion);
        }
        return uniforms.get(nombre);
    }

    protected void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z);
    }

    protected void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor);
    }

    protected void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor);
    }

    // ==================== 6. SHADERS (PROYECCIÓN MINIMAPA ESCALADA) ====================

    private String vertexShader() {
        return """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;
            uniform vec3 uPos;
            uniform vec3 uEscala;
            uniform vec3 uOjo;
            uniform vec3 uObjetivo;
            uniform float uGiro;
            uniform float uAspecto;
            uniform int uMapa;
            out vec3 vMundo;
            out vec3 vNormal;

            void main() {
                float coseno = cos(uGiro);
                float seno = sin(uGiro);
                mat3 giro = mat3(
                    coseno, 0.0, -seno,
                    0.0, 1.0, 0.0,
                    seno, 0.0, coseno
                );
                vMundo = giro * (aPos * uEscala) + uPos;
                vNormal = normalize(giro * (aNormal / uEscala));

                if (uMapa == 1) {
                    // Escala ortográfica para mapa 11x11 (límite 55 -> divisor 58.0 para encuadre completo)
                    float pantallaX = vMundo.x / 58.0;
                    float pantallaY = -vMundo.z / 58.0;
                    float profundidad = -vMundo.y / 120.0;
                    gl_Position = vec4(pantallaX, pantallaY, profundidad, 1.0);
                } else {
                    vec3 frente = normalize(uObjetivo - uOjo);
                    vec3 derecha = normalize(cross(frente, vec3(0.0, 1.0, 0.0)));
                    vec3 arriba = cross(derecha, frente);
                    vec3 diferencia = vMundo - uOjo;
                    float vistaX = dot(diferencia, derecha);
                    float vistaY = dot(diferencia, arriba);
                    float vistaZ = -dot(diferencia, frente);
                    float factor = 1.0 / tan(radians(55.0) * 0.5);
                    float cerca = 0.1;
                    float lejos = 350.0; // Rango aumentado para la ciudad más grande
                    float clipX = vistaX * factor / uAspecto;
                    float clipY = vistaY * factor;
                    float clipZ = (lejos + cerca) / (cerca - lejos) * vistaZ;
                    clipZ += 2.0 * lejos * cerca / (cerca - lejos);
                    gl_Position = vec4(clipX, clipY, clipZ, -vistaZ);
                }
            }
            """;
    }

    protected String fragmentShader() {
        return """
            #version 330 core
            uniform vec3 uColor;
            out vec4 color;
            void main() {
                color = vec4(uColor, 1.0);
            }
            """;
    }

    private int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo);
        glShaderSource(shader, fuente);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException(error);
        }
        return shader;
    }

    private void crearPrograma() {
        int shaderVertices = compilar(GL_VERTEX_SHADER, vertexShader());
        int shaderFragmentos = 0;
        try {
            shaderFragmentos = compilar(GL_FRAGMENT_SHADER, fragmentShader());
            programa = glCreateProgram();
            glAttachShader(programa, shaderVertices);
            glAttachShader(programa, shaderFragmentos);
            glLinkProgram(programa);
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {
                throw new IllegalStateException(glGetProgramInfoLog(programa));
            }
        } finally {
            glDeleteShader(shaderVertices);
            if (shaderFragmentos != 0) {
                glDeleteShader(shaderFragmentos);
            }
        }
    }

    // ==================== 7. GEOMETRÍA DEL CUBO ====================

    private void crearCubo() {
        float[] vertices = {
            -0.5f, -0.5f, 0.5f, 0, 0, 1,
            0.5f, -0.5f, 0.5f, 0, 0, 1,
            0.5f, 0.5f, 0.5f, 0, 0, 1,
            0.5f, 0.5f, 0.5f, 0, 0, 1,
            -0.5f, 0.5f, 0.5f, 0, 0, 1,
            -0.5f, -0.5f, 0.5f, 0, 0, 1,

            -0.5f, -0.5f, -0.5f, 0, 0, -1,
            -0.5f, 0.5f, -0.5f, 0, 0, -1,
            0.5f, 0.5f, -0.5f, 0, 0, -1,
            0.5f, 0.5f, -0.5f, 0, 0, -1,
            0.5f, -0.5f, -0.5f, 0, 0, -1,
            -0.5f, -0.5f, -0.5f, 0, 0, -1,

            -0.5f, 0.5f, 0.5f, -1, 0, 0,
            -0.5f, 0.5f, -0.5f, -1, 0, 0,
            -0.5f, -0.5f, -0.5f, -1, 0, 0,
            -0.5f, -0.5f, -0.5f, -1, 0, 0,
            -0.5f, -0.5f, 0.5f, -1, 0, 0,
            -0.5f, 0.5f, 0.5f, -1, 0, 0,

            0.5f, 0.5f, 0.5f, 1, 0, 0,
            0.5f, -0.5f, 0.5f, 1, 0, 0,
            0.5f, -0.5f, -0.5f, 1, 0, 0,
            0.5f, -0.5f, -0.5f, 1, 0, 0,
            0.5f, 0.5f, -0.5f, 1, 0, 0,
            0.5f, 0.5f, 0.5f, 1, 0, 0,

            -0.5f, 0.5f, -0.5f, 0, 1, 0,
            -0.5f, 0.5f, 0.5f, 0, 1, 0,
            0.5f, 0.5f, 0.5f, 0, 1, 0,
            0.5f, 0.5f, 0.5f, 0, 1, 0,
            0.5f, 0.5f, -0.5f, 0, 1, 0,
            -0.5f, 0.5f, -0.5f, 0, 1, 0,

            -0.5f, -0.5f, -0.5f, 0, -1, 0,
            0.5f, -0.5f, -0.5f, 0, -1, 0,
            0.5f, -0.5f, 0.5f, 0, -1, 0,
            0.5f, -0.5f, 0.5f, 0, -1, 0,
            -0.5f, -0.5f, 0.5f, 0, -1, 0,
            -0.5f, -0.5f, -0.5f, 0, -1, 0
        };
        vao = glGenVertexArrays();
        glBindVertexArray(vao);
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        int bytesPorVertice = 6 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, bytesPorVertice, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, bytesPorVertice, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public static void main(String[] args) {
        clase1 aplicacion = new clase1();
        aplicacion.run();
    }
}