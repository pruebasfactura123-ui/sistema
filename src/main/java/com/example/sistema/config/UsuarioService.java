package com.example.sistema.config;

import com.example.sistema.model.Usuario;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // PASE VIP: Si escribes admin, entra directo sin buscar en la base de datos
        if ("admin".equalsIgnoreCase(username)) {
            return new org.springframework.security.core.userdetails.User(
                    "admin",
                    "admin123",
                    List.of(new SimpleGrantedAuthority("ROLE_JEFE"))
            );
        }

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        String nombreRol = usuario.getRol();
        if (!nombreRol.startsWith("ROLE_")) {
            nombreRol = "ROLE_" + nombreRol;
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(nombreRol);

        return new org.springframework.security.core.userdetails.User(
                usuario.getUsername(),
                usuario.getPassword(),
                List.of(authority)
        );
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario guardarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public void cambiarRol(Long id, String accion) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + id));

        if ("MartinCM".equals(usuario.getUsername())) {
            throw new IllegalArgumentException("¡Acción denegada! El rol del jefe principal no puede ser modificado.");
        }

        if ("subir".equals(accion)) {
            usuario.setRol("GERENTE");
        } else if ("bajar".equals(accion)) {
            usuario.setRol("CONTADOR");
        }

        usuarioRepository.save(usuario);
    }

    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + id));

        if ("MartinCM".equals(usuario.getUsername())) {
            throw new IllegalArgumentException("¡Acción denegada! No se puede eliminar al jefe principal del sistema.");
        }

        usuarioRepository.delete(usuario);
    }
}