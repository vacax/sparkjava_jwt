package edu.pucmm.sjjwt.encapsulaciones;

/**
 * Objeto para encapsular la salida
 */
public class LoginResponse {
    String token;
    long expires_in;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(long expires_in) {
        this.expires_in = expires_in;
    }
}
