package com.example.sistema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 1. Accesos públicos sin restricciones (Rutas y Recursos Estáticos)
                .requestMatchers("/login", "/registrar-empresa", "/css/**", "/js/**", "/images/**").permitAll()
                
                // 2. Permisos de Administración (Solo JEFE)
                .requestMatchers("/usuarios/crear", "/borrar-todo", "/usuarios/cambiar-rol/**").hasAnyAuthority("JEFE", "ROLE_JEFE")
                
                // 3. Permisos de Gestión de Personal y Módulo de Nóminas (JEFE y GERENTE unicamente)
                // Se añadieron las rutas de nóminas para bloquear el acceso al TRABAJADOR
                .requestMatchers(
                    "/usuarios/eliminar/**", 
                    "/usuarios",
                    "/operaciones/nominas",
                    "/operaciones/nominas/**"
                ).hasAnyAuthority("JEFE", "ROLE_JEFE", "GERENTE", "ROLE_GERENTE")
                
                // 4. Dashboard principal y operaciones comunes (Cualquier rol autenticado)
                // ¡AQUÍ SE AGREGÓ /facturas/historial PARA DARLE PERMISOS DE ACCESO AL HISTORIAL!
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
                
                // Cierre de seguridad para cualquier otra ruta interna
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