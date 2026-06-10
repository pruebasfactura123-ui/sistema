package com.example.sistema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Obliga al sistema a leer contraseñas directas en texto plano (sin BCrypt por ahora)
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Deshabilitado para pruebas de desarrollo
            .authorizeHttpRequests(auth -> auth
                // 1. Accesos públicos sin restricciones (Rutas del login y recursos estáticos)
                .requestMatchers("/login", "/registrar-empresa", "/css/**", "/js/**", "/images/**").permitAll()
                
                // 2. Permisos de Administración Avanzada (Solo el puesto JEFE)
                .requestMatchers("/usuarios/crear", "/borrar-todo", "/usuarios/cambiar-rol/**").hasAnyAuthority("JEFE", "ROLE_JEFE")
                
                // 3. Permisos de Gestión de Personal y Módulo de Nóminas (JEFE y GERENTE únicamente)
                .requestMatchers(
                    "/usuarios/eliminar/**", 
                    "/usuarios",
                    "/operaciones/nominas",
                    "/operaciones/nominas/**"
                ).hasAnyAuthority("JEFE", "ROLE_JEFE", "GERENTE", "ROLE_GERENTE")
                
                // 4. Dashboard principal y operaciones comunes (Cualquier rol que se haya autenticado)
                .requestMatchers(
                    "/", 
                    "/facturas/nueva", 
                    "/facturas/historial", 
                    "/subir", 
                    "/descargar/**", 
                    "/borrar/**", 
                    "/perfil", 
                    "/perfil/actualizar-clave", 
                    "/perfil/actualizar-foto"
                ).hasAnyAuthority("JEFE", "ROLE_JEFE", "GERENTE", "ROLE_GERENTE", "TRABAJADOR", "ROLE_TRABAJADOR")
                
                // Candado de cierre para cualquier otra pantalla del sistema
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            );
        
        return http.build();
    }
}