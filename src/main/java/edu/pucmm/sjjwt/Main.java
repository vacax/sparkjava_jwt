package edu.pucmm.sjjwt;

import com.google.gson.Gson;
import edu.pucmm.sjjwt.encapsulaciones.ErrorRespuesta;
import edu.pucmm.sjjwt.encapsulaciones.Estudiante;
import edu.pucmm.sjjwt.encapsulaciones.LoginResponse;
import edu.pucmm.sjjwt.encapsulaciones.Usuario;
import edu.pucmm.sjjwt.servicios.EstudianteService;
import edu.pucmm.sjjwt.utilidades.JsonUtilidades;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.KeyFactory;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import static spark.Spark.*;

public class Main {

    //Llave de 32 bytes por la firma utilizada
    public final static String LLAVE_SECRETA = "asd12D1234dfr123@#4Fsdcasdd5g78a";
    public final static String ACCEPT_TYPE_JSON = "application/json";
    public final static String ACCEPT_TYPE_XML = "application/xml";
    public final static int BAD_REQUEST = 400;
    public final static int UNAUTHORIZED = 401;
    public final static int FORBIDDEN = 403;
    public final static int ERROR_INTERNO = 500;

    public static void main(String[] args) {
        System.out.println("Aplicación Demostración JSON Web TOKEN");

        filtroCors();

        EstudianteService estudianteService = EstudianteService.getInstancia();

        get("/", (request, response) -> {
            return "Aplicación Demostración JSON Web TOKEN - SPARK JAVA";
        });

        /**
         * Proceso para autenticar y generar la trama. Como demostración vamos a tener dos usuarios
         * en forma directa.
         */
        post("/login", (request, response) -> {
            //
            response.type(ACCEPT_TYPE_JSON);
            //
            Usuario usuarioObj=null;
            String usuario = request.queryParamOrDefault("usuario","");
            String password = request.queryParamOrDefault("password", "");
            //
            String passwordAdmin = "admin";
            String passwordUsuario = "usuario";
            //
            if(usuario.equals("admin") && password.equals(passwordAdmin)){
                //Información simulando que viene de la base de datos.
                //por simplificar la demostración no implementado.
                usuarioObj = new Usuario("admin","admin","Administrador");
                //agregando los roles
                usuarioObj.setRoles(Arrays.asList("creacion","listar","actualizar", "eliminar"));
            }else if(usuario.equals("usuario") && password.equals(passwordUsuario)){

            }else{
                response.status(UNAUTHORIZED);
                return new ErrorRespuesta(UNAUTHORIZED, "Autentificación no Correcta");
            }
            return generacionJsonWebToken(usuarioObj) ;
        }, JsonUtilidades.json());


        /**
         * API REST para Desmostrar la consulta del token
         */
        path("/api", () -> {

            /**
             * Filtro para validar que exista el token en la consulta
             */
            before("/*",(request, response) ->{
                System.out.println("Analizando que exista el token");

                //si es del tipo options lo dejo pasar.
                if(request.requestMethod() == "OPTIONS"){
                    return;
                }

                //informacion para consultar en la trama.
                String header = "Authorization";
                String prefijo = "Bearer";

                //mostrando todos los header recibidos.
                Set<String> listaHeader = request.headers();
                for(String key : listaHeader){
                    System.out.println(String.format("header[%s] = %s", key, request.headers(key)));
                }

                //Verificando si existe el header de autorizacion.
                String headerAutentificacion = request.headers(header);
                if(headerAutentificacion ==null || !headerAutentificacion.startsWith(prefijo)){
                    halt(FORBIDDEN, "No tiene permiso para acceder al recurso");
                }

                //recuperando el token y validando
                String tramaJwt = headerAutentificacion.replace(prefijo, "");
                try {
                    Claims claims = Jwts.parser()
                            .setSigningKey(Keys.hmacShaKeyFor(LLAVE_SECRETA.getBytes()))
                            .parseClaimsJws(tramaJwt).getBody();
                    //mostrando la información para demostración.
                    System.out.println("Mostrando el JWT recibido: " + claims.toString());
                }catch (ExpiredJwtException | MalformedJwtException | SignatureException e){ //Excepciones comunes
                    halt(FORBIDDEN, e.getMessage());
                }

                //En este punto puedo realizar validaciones en función a los permisos del usuario.
                // tener pendiente que el JWT está formado no encriptado.


            });

            after("/*", ((request, response) -> {
                response.type(ACCEPT_TYPE_JSON);
            }));

            path("/estudiante", () -> {


                //listar todos los estudiantes.
                get("/", (request, response) -> {
                    return estudianteService.getAllEstudiantes();
                }, JsonUtilidades.json());

                //retorna un estudiante
                get("/:matricula", (request, response) -> {
                    Estudiante estudiante = estudianteService.getEstudiante(Integer.parseInt(request.params("matricula")));
                    if(estudiante==null){
                        throw new RuntimeException("No existe el cliente");
                    }
                    return  estudiante;
                }, JsonUtilidades.json());

                //crea un estudiante
                post("/", Main.ACCEPT_TYPE_JSON, (request, response) -> {

                    Estudiante estudiante = new Gson().fromJson(request.body(), Estudiante.class);

                    return estudianteService.crearEstudiante(estudiante);
                }, JsonUtilidades.json());

                //actualiza un estudiante
                put("/", Main.ACCEPT_TYPE_JSON, (request, response) -> {
                    Estudiante estudiante = new Gson().fromJson(request.body(), Estudiante.class);
                    return estudianteService.actualizarEstudiante(estudiante);
                }, JsonUtilidades.json());

                //eliminar un estudiante
                delete("/:matricula", (request, response) -> {
                    return estudianteService.eliminarEstudiante(Integer.parseInt(request.params("matricula")));
                }, JsonUtilidades.json());

            });

        });
    }

    /**
     * Metodo para la generación de la trama JWT
     * @param usuario
     * @return
     */
    private static LoginResponse generacionJsonWebToken(Usuario usuario){
        LoginResponse loginResponse = new LoginResponse();
        //generando la llave.
        SecretKey secretKey = Keys.hmacShaKeyFor(LLAVE_SECRETA.getBytes());
        //Generando la fecha valida
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(3);
        System.out.println("La fecha actual: "+localDateTime.toString());

        // creando la trama.
        String jwt = Jwts.builder()
                .setIssuer("PUCMM-ECT")
                .setSubject("Demo JWT")
                .setExpiration(Date.from(localDateTime.toInstant(ZoneOffset.ofHours(-4))))
                .claim("usuario", usuario.getUsuario())
                .claim("roles", String.join(",", usuario.getRoles()))
                .signWith(secretKey)
                .compact();
        loginResponse.setToken(jwt);
        return loginResponse;
    }

    /**
     *  Aplicando el filtro para permitir el CORS.
     *  Si estamos realizando una consulta desde un navegador es
     *  necesario dar permisos de intercambio de información.
     */
    private static void filtroCors(){

        //Enviando la información a solicitud del CORS
        options("/*", (request, response) -> {
            System.out.println("Entrando al metodo de options");
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers",accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods",accessControlRequestMethod);
            }

            return "OK";
        });

        //Filtro para validar el CORS
        before((request, response) -> {
            System.out.println("Aplicando header del API del CORS");
            response.header("Access-Control-Allow-Origin", "*");
            //response.type("application/json");
        });
    }
}
