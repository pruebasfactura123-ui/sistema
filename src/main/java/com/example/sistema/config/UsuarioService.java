package com.example.sistema.config;

import com.example.sistema.model.Usuario;
import com.example.sistema.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile; // <--- NUEVO IMPORT MÁGICO
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Profile("prod_con_db") // <--- ESTO APAGA ESTA CLASE TEMPORALMENTE EN RENDER
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // =========================================================================
    // MÉTODO EXISTENTE: Autenticación para Spring Security
    // =========================================================================
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscar usuario en la base de datos
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // Obtener el rol (ej: "JEFE") y convertirlo en autoridad de Spring
        String nombreRol = usuario.getRol();
        
        // Si el rol en BD no empieza con ROLE_, se lo agregamos para estandarizar
        if (!nombreRol.startsWith("ROLE_")) {
            nombreRol = "ROLE_" + nombreRol;
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(nombreRol);

        // Devolver el usuario mapeado para Spring Security
        return new org.springframework.security.core.userdetails.User(
                usuario.getUsername(),
                usuario.getPassword(),
                List.of(authority)
        );
    }

    // =========================================================================
    // NUEVOS MÉTODOS: Gestión de Personal con Candados de Seguridad
    // =========================================================================

    /**
     * Obtiene la lista completa de usuarios para mostrarla en la tabla.
     */
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    /**
     * Guarda o registra un nuevo trabajador en el sistema.
     */
    public Usuario guardarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    /**
     * Cambia la jerarquía/rol de un usuario. Bloquea cambios si es MartinCM.
     */
    public void cambiarRol(Long id, String accion) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + id));

        // CANDADO DE SEGURIDAD: Evita que modifiquen al jefe por peticiones externas
        if ("MartinCM".equals(usuario.getUsername())) {
            throw new IllegalArgumentException("¡Acción denegada! El rol del jefe principal no puede ser modificado.");
        }

        // Lógica para alternar roles (ajústala según tus puestos: GERENTE, CONTADOR, etc.)
        if ("subir".equals(accion)) {
            usuario.setRol("GERENTE");
        } else if ("bajar".equals(accion)) {
            usuario.setRol("CONTADOR"); // O el rol inferior que manejes
        }

        usuarioRepository.save(usuario);
    }

    /**
     * Elimina/Despide a un trabajador del sistema. Bloquea la acción si es MartinCM.
     */
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + id));

        // CANDADO DE SEGURIDAD: Evita que eliminen al jefe
        if ("MartinCM".equals(usuario.getUsername())) {
            throw new IllegalArgumentException("¡Acción denegada! No se puede eliminar al jefe principal del sistema.");
        }

        usuarioRepository.delete(usuario);
    }
}